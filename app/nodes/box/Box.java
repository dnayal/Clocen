package nodes.box;

import helpers.OAuth2Helper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import models.NodeParams;
import models.NodeParamsKey;
import models.Process;
import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;

import play.data.DynamicForm;
import process.ProcessExecutor;

/**
 * Box access token is valid for 1 hour. In order to get a new valid token, 
 * use the refresh_token, which is valid for 14 days.
 * 
 * BOX API DOC - http://developers.box.com/docs/
 *
 */
public class Box implements Node, BoxConstants {

	private static final String COMPONENT_NAME = "Box Node";	

	@Override
	public String authorize(String userId, AccessType accessType, String data) {
		String response = OAuth2Helper.getAccess(userId, NODE_ID, CLIENT_ID, CLIENT_SECRET, 
				accessType, data, OAUTH_AUTHORIZE_URL, OAUTH_TOKEN_URL);

		// replacing this if loop with switch case will give error
		// because the compiler then generates an internal class
		// which fails in ServiceNodeHelper.getNodeClass as runtime 
		// engine is unable to instantiate it
		if(accessType==AccessType.OAUTH_RENEW || accessType==AccessType.OAUTH_TOKEN) {
			ServiceAccessTokenKey key = new ServiceAccessTokenKey(userId, NODE_ID);
			BoxServices boxServices = new BoxServices(ServiceAccessToken.getServiceAccessToken(key));

			// get the user id and store it
			String id = boxServices.getUserId();
			if(!UtilityHelper.isEmptyString(id)) {
				NodeParams params = new NodeParams(new NodeParamsKey(userId, NODE_ID, PARAM_USER_ID), 
											id, Calendar.getInstance().getTime());
				params.store();
			}
		}

		return response;
	}
	
	
	@Override
	public String getNodeId() {
		return NODE_ID;
	}


	@Override
	public String getName() {
		return APP_NAME;
	}
	

	@Override
	public String getDescription() {
		return APP_DESCRIPTION;
	}
	

	@Override
	public String getLogo() {
		return UtilityHelper.getAsset("/images/nodes/box.png");
	}


	@Override
	public String getTriggerType() {
		return Node.TRIGGER_TYPE_HOOK;
	}


	@Override
	public String getAppURL() {
		return APP_URL;
	}


	/**
	 * API error handler for Box
	 */
	@Override
	public Boolean serviceResponseHasError(String serviceName, Integer statusCode, 
			JsonNode responseJSON, ServiceAccessToken serviceToken) {
	
		Boolean serviceHasErrors = true; 
		
		switch(statusCode) {
			case 200: case 201: case 202: case 204:
				serviceHasErrors = false;
				break;
			case 302: // Redirect
				serviceHasErrors = true;
				break;
			case 304: // Not modified
				serviceHasErrors = true;
				break;
			case 400: // Bad request
				serviceHasErrors = true;
				break;
			case 401: // Unauthorized
				serviceHasErrors = true;
				break;
			case 403: // Forbidden
				serviceHasErrors = true;
				break;
			case 404: // Not Found
				serviceHasErrors = true;
				break;
			case 405: // Method not allowed
				serviceHasErrors = true;
				break;
			case 409: // Conflict
				serviceHasErrors = true;
				break;
			case 412: // Precondition failed
				serviceHasErrors = true;
				break;
			case 429: // Too many requests
				serviceHasErrors = true;
				UtilityHelper.logError(COMPONENT_NAME, "serviceResponseHasError()", responseJSON.get("message").asText(), new RuntimeException("Too many requests"));
				break;
			case 500: // Internal server error
				serviceHasErrors = true;
				break;
			case 507: // Insufficient storage
				serviceHasErrors = true;
				break;
			default: // Unknown status/error
				serviceHasErrors = true;
				break;
		}
	
		if(serviceHasErrors) {
			JsonNode errors = responseJSON;
	
			for(JsonNode error : errors) {
				UtilityHelper.logMessage(COMPONENT_NAME, "serviceResponseHasError()", "Service error for user [" + 
						serviceToken.getKey().getUserId() + "] & node [" + serviceToken.getKey().getNodeId() + 
						"] - (" + serviceName + ")" + error.get("message").asText() + " [" + statusCode + "]");
			}
		}
	
		return serviceHasErrors;
	}


	/**
	 * Box services to get information for powering process editor
	 */
	@Override
	public JsonNode callInfoService(User user, String service) {
		ServiceAccessTokenKey key = new ServiceAccessTokenKey(user.getUserId(), getNodeId());
		ServiceAccessToken token = ServiceAccessToken.getServiceAccessToken(key);
		BoxServices services = new BoxServices(token);
		
		// service to get list of all folders to monitor
		if (service.equalsIgnoreCase(SERVICE_INFO_GET_FOLDERS)) {
			return services.getFolders();
		} else {
			return null;
		}
	}


	/**
	 * Box services exposed to the user
	 */
	@Override
	public Map<String, Object> executeService(String processId, Integer nodeIndex, String serviceName, ServiceAccessToken sat, Map<String, Object> nodeData) {
		BoxServices services = new BoxServices(sat);
		
		// service to create a new file
		if(serviceName.equalsIgnoreCase(SERVICE_ACTION_CREATE_FILE)) {
			return services.createFile(processId, nodeIndex, nodeData);
		// service to create a new folder	
		} if(serviceName.equalsIgnoreCase(SERVICE_ACTION_CREATE_FOLDER)) {
			return services.createFolder(processId, nodeIndex, nodeData);
		} else { 
			return null;
		}
	}


	/**
	 * Box Webhooks event notification handler
	 */
	@Override
	public void executeTrigger(Object object) {
		DynamicForm form = (DynamicForm) object;
		String eventType = form.get("event_type"); // uploaded, created or deleted
		String itemType = form.get("item_type"); // file or folder
		String boxUserId = form.get("from_user_id");
		
		// For the given box id, get the NodeParams based on the incoming userId
		// This will provide us with the internal user id mapped in Clocen
		NodeParams params = NodeParams.retrieveByValue(NODE_ID, PARAM_USER_ID, boxUserId);
		// Once you have the Clocen user id, you can create a 
		// servicetoken to get the processes created by the user
		ServiceAccessTokenKey key = new ServiceAccessTokenKey(params.getKey().getUserId(), NODE_ID);
		ServiceAccessToken accessToken = ServiceAccessToken.getServiceAccessToken(key);
		BoxServices boxServices = new BoxServices(accessToken);
		
		List<Process> processes = null;

		// Check for file created or updated event
		// Box webhooks throws an "uploaded" event, instead of a "created" event
		// when a new file is created
		if((eventType.equalsIgnoreCase(TRIGGER_UPLOADED) || eventType.equalsIgnoreCase(TRIGGER_CREATED)) 
															&& itemType.equalsIgnoreCase(ITEM_TYPE_FILE)) {
			UtilityHelper.logMessage(COMPONENT_NAME, "executeTrigger()", "File Created or Updated for user id : " + params.getKey().getUserId());
			// Get all the processes with the given trigger
			processes = Process.getProcessesForTrigger(key, SERVICE_TRIGGER_FILE_UPLOADED);
			
			// Loop through each process and check whether the trigger 
			// has been fired for it. If so, map the output values
			for(Process process: processes) {
				ArrayList<Map<String, Object>> array = process.getProcessDataAsObject();
				Map<String, Object> output = boxServices.getNewFileUploaded(array.get(0), 
						form.get("item_parent_folder_id"), form.get("item_id"), form.get("item_name"));

				// if there was a valid output for the trigger node
				// update the process array with it
				if(output != null) {
					array.set(0, output);
					ProcessExecutor.executeHookProcess(process.getProcessId(), array, params.getKey().getUserId());
				}
			}
			
		} 

		// Check for new folder created event
		else if(eventType.equalsIgnoreCase(TRIGGER_CREATED) && itemType.equalsIgnoreCase(ITEM_TYPE_FOLDER)) {
			
			UtilityHelper.logMessage(COMPONENT_NAME, "executeTrigger()", "New Folder Created for user id : " + params.getKey().getUserId());
			// Get all processes with the given trigger
			processes = Process.getProcessesForTrigger(key, SERVICE_TRIGGER_NEW_FOLDER_CREATED);

			// Loop through each process and check whether the trigger 
			// has been fired for it. If so, map the output values
			for(Process process: processes) {
				ArrayList<Map<String, Object>> array = process.getProcessDataAsObject();
				Map<String, Object> output = boxServices.getNewFolderCreated(array.get(0), 
						form.get("item_parent_folder_id"), form.get("item_id"), form.get("item_name"));

				// if there was a valid output for the trigger node
				// update the process array with it
				if(output != null) {
					array.set(0, output);
					ProcessExecutor.executeHookProcess(process.getProcessId(), array, params.getKey().getUserId());
				}
			}
		}
	}
}
