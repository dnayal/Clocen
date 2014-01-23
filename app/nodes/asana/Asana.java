package nodes.asana;

import helpers.OAuth2Helper;
import helpers.UtilityHelper;

import java.util.Map;

import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;

import play.mvc.Controller;

/**
 * Node interface for Asana
 */
public class Asana implements Node, AsanaConstants {

	private static final String COMPONENT_NAME = "Asana Node";

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
		return controllers.routes.Assets.at("images/nodes/asana.png").absoluteURL(Controller.request());
	}


	@Override
	public String getTriggerType() {
		return Node.TRIGGER_TYPE_POLL;
	}


	@Override
	public String getAppURL() {
		return APP_URL;
	}


	/**
	 * API error handler for Asana
	 */
	@Override
	public Boolean serviceResponseHasError(String serviceName, Integer statusCode, 
			JsonNode responseJSON, ServiceAccessToken serviceToken) {
		
		Boolean serviceHasErrors = true; 
		
		switch(statusCode) {
			case 200: case 201:
				serviceHasErrors = false;
				break;
			case 400: // Invalid request
				serviceHasErrors = true;
				break;
			case 401: // No authorization
				serviceHasErrors = true;
				break;
			case 403: // Forbidden
				serviceHasErrors = true;
				break;
			case 404: // Not Found
				serviceHasErrors = true;
				break;
			case 429: // Rate Limit Enforced
				serviceHasErrors = true;
				UtilityHelper.logError(COMPONENT_NAME, "serviceResponseHasError()", responseJSON.path("errors").get("message").asText(), new RuntimeException("Rate Limit Enforced"));
				break;
			case 500: // Server error
				serviceHasErrors = true;
				break;
			default: // Unknown status/error
				serviceHasErrors = true;
				break;
		}
	
		if(serviceHasErrors) {
			JsonNode errors = responseJSON.path("errors");
			
			for(JsonNode error : errors) {
				UtilityHelper.logMessage(COMPONENT_NAME, "serviceResponseHasError()", "Service error for user [" + 
						serviceToken.getKey().getUserId() + "] & node [" + serviceToken.getKey().getNodeId() + 
						"] - (" + serviceName + ")" + error.get("message").asText() + " [" + statusCode + "]");
			}
		}
	
		return serviceHasErrors;
	}


	/**
	 * Asana services to get information for powering process editor
	 */
	@Override
	public JsonNode callInfoService(User user, String service) {
		ServiceAccessTokenKey key = new ServiceAccessTokenKey(user.getUserId(), getNodeId());
		ServiceAccessToken token = ServiceAccessToken.getServiceAccessToken(key);
		AsanaServices services = new AsanaServices(token);
		
		// service to get the list of all workspaces
		if (service.equalsIgnoreCase(SERVICE_INFO_GET_WORKSPACES)) {
			return services.getWorkspaces();
		} else {
			return null;
		}
	}


	/**
	 * Asana services exposed to the user
	 */
	@Override
	public Map<String, Object> executeService(String serviceName, ServiceAccessToken sat, Map<String, Object> nodeData) {
		AsanaServices services = new AsanaServices(sat);
		
		// service to know whether new task was created
		if (serviceName.equalsIgnoreCase(SERVICE_TRIGGER_NEW_TASK_CREATED)) {
			return services.getNewTaskCreated(nodeData);
		// service to create a new task
		} else if (serviceName.equalsIgnoreCase(SERVICE_ACTION_CREATE_TASK)) {
			return services.createTask(nodeData);
		// service to create a new project
		} else if (serviceName.equalsIgnoreCase(SERVICE_ACTION_CREATE_PROJECT)) {
			return services.createProject(nodeData);
		// service to know whether new project was created
		} else if (serviceName.equalsIgnoreCase(SERVICE_TRIGGER_NEW_PROJECT_CREATED)) {
			return services.getNewProjectCreated(nodeData);
		} else
			return null;
	}


	/**
	 * Asana does not support webhooks for now
	 */
	@Override
	public void executeTrigger(Object object) {
		return;
	}

}
