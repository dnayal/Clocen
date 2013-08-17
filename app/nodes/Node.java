package nodes;

public interface Node {
	
	public static enum AccessType {
		OAUTH_AUTHORIZE, 
		OAUTH_TOKEN,
		OAUTH_RENEW
	};
	
	public String authorize(AccessType accessType, String data);
	public Boolean hasAccess();
	public String getNodeId();

}
