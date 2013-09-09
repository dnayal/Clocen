package nodes;

public interface Node {
	
	public static enum AccessType {
		OAUTH_AUTHORIZE, 
		OAUTH_TOKEN,
		OAUTH_RENEW
	};
	
	public String authorize(AccessType accessType, String data);
	public String getNodeId();
	public String getName();
	public String getDescription();

}
