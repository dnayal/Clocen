package nodes;

public interface Node {
	
	public static enum AccessType {
		OAUTH_AUTHORIZE, 
		OAUTH_TOKEN,
		OAUTH_RENEW
	};
	
	public String getAccess(AccessType accessType, String data);
	public Boolean hasAccess();
	public String getNodeId();

}
