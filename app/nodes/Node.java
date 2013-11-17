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
	public JsonNode callInfoService(User user, String service);
	public Map<String, Object> executeService(String serviceName, ServiceAccessToken sat, Map<String, Object> nodeData);

}
