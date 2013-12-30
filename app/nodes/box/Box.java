package nodes.box;

import helpers.OAuth2Helper;
import helpers.UtilityHelper;

import java.util.Map;

import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;

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
		return OAuth2Helper.getAccess(userId, NODE_ID, CLIENT_ID, CLIENT_SECRET, 
				accessType, data, OAUTH_AUTHORIZE_URL, OAUTH_TOKEN_URL);
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
		return controllers.routes.Assets.at("images/nodes/box.png").absoluteURL(Controller.request());
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
	public Map<String, Object> executeService(String serviceName, ServiceAccessToken sat, Map<String, Object> nodeData) {
		BoxServices services = new BoxServices(sat);
		
		// service to create a new file
		if(serviceName.equalsIgnoreCase(SERVICE_ACTION_CREATE_FILE)) {
			return services.createFile(nodeData);
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
		Logger.info("********************");
		Logger.info("item_id : " + form.get("item_id"));
		Logger.info("item_name : " + form.get("item_name"));
		Logger.info("item_type : " + form.get("item_type"));
		Logger.info("event_type : " + form.get("event_type"));
		Logger.info("item_parent_folder_id : " + form.get("item_parent_folder_id"));
		Logger.info("item_description : " + form.get("item_description"));
		Logger.info("new_item_id : " + form.get("new_item_id"));
		Logger.info("new_item_parent_folder_id : " + form.get("new_item_parent_folder_id"));
		Logger.info("from_user_id : " + form.get("from_user_id"));
		Logger.info("********************");
	}
}
