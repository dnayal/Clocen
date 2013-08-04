package nodes;

public interface Node {
	
	public void authenticate(String code);
	public String getNodeId();
	public String getOauthAuthorizationURL();

}
