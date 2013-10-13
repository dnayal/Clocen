package nodes;

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
	
	public String authorize(String userId, AccessType accessType, String data);
	public String getNodeId();
	public String getName();
	public String getTriggerType();
	public String getLogo();
	public String getDescription();
	public JsonNode callInfoService(User user, String service);

}
