package nodes;

public class Helper {
	
	public static void getAllNodes() {
		
	}
	
	public static String getOauthRedirectURI(String nodeId) {
		return "https://shadence.com:9443/nodes/" + nodeId + "/callback/oauth";
	}
	
	
	public static Boolean isEmptyString(String string) {
		if (string==null || string.trim().equalsIgnoreCase(""))
			return true;
		else
			return false;
	}

}
