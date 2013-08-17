package helpers;

import nodes.Node.AccessType;
import play.libs.WS;

public class OAuthHelper {

	public static String getOAuthRedirectURI(String nodeId) {
		return "https://localhost:9443/nodes/" + nodeId + "/callback/oauth";
	}
	
	public static String getAccess(String nodeId, String clientId, String clientSecret, 
			AccessType accessType, String data, String redirectURI, String authorizeURL, String tokenURL) {
		switch(accessType) {
			case OAUTH_AUTHORIZE:
				return authorizeURL + "?response_type=code&client_id="+
					clientId +"&state=authenticated&redirect_uri=" + getOAuthRedirectURI(nodeId);
				
			case OAUTH_TOKEN:
				if(Helper.isEmptyString(data))
					throw new RuntimeException("OAuth code empty for Node Id: " + nodeId + " token call");

				return WS.url(tokenURL)
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.post("grant_type=authorization_code&code="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getOAuthRedirectURI(nodeId))
				.get()
				.asJson()
				.get("access_token").asText();
				
			case OAUTH_RENEW:
				return null;
			
			default:
				return null;
		}
	}

}
