package nodes;

import java.util.Map;

import models.ServiceAccessToken;
import models.User;

import org.codehaus.jackson.JsonNode;

public interface Node {
	
	public static enum AccessType {
		OAUTH_AUTHORIZE, 
		OAUTH_TOKEN,
		OAUTH_RENEW
	};
	
	public static final String TRIGGER_TYPE_POLL = "poll";
	public static final String TRIGGER_TYPE_HOOK = "hook";
	
	// constants for service attributes
	// MUST be same as the ones given in json config file
	public static final String ATTR_TYPE_SERVICE = "service";
	public static final String ATTR_TYPE_STRING = "string";
	public static final String ATTR_TYPE_LONGSTRING = "longstring";
	public static final String ATTR_TYPE_FILE = "file";
	
	public String authorize(String userId, AccessType accessType, String data);
	
	public String getNodeId();
	
	public String getName();
	
	public String getTriggerType();
	
	public String getLogo();
	
	public String getAppURL();
	
	public String getDescription();

	
	/**
	 * Checks whether the service was executed successfully. 
	 * If there are any errors, the node processes those accordingly
	 */
	public Boolean serviceResponseHasError(String serviceName, Integer statusCode, JsonNode responseJSON, ServiceAccessToken serviceToken);
	
	
	/**
	 * Calls the info service that executes the required node service.
	 * It is used to power the drop down input lists for the given Node
	 */
	public JsonNode callInfoService(User user, String service);
	

	/**
	 * Executes the required service for the node
	 */
	public Map<String, Object> executeService(String serviceName, ServiceAccessToken sat, Map<String, Object> nodeData);
	
	
	/**
	 * Used for node that support webhooks 
	 * to process event notifications
	 */
	public void executeTrigger(Object object);

}
