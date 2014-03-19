package helpers;

import java.util.Calendar;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.libs.WS;
import auth.OAuth2AuthNode;
import auth.OAuth2AuthNode.OAuth2AccessType;

public class OAuth2Helper {

	private static final String COMPONENT_NAME = "OAuth 2 Helper";
	
	
	/**
	 * Private method for OAuth redirect URI using 
	 * the node id, used for OAuth callback
	 */
	private static String getRedirectURI(String nodeId) {
		String URI = null;
		
		if(Play.isProd()) {
			URI = Play.application().configuration().getString("application.URL.PROD") + 
					"/app/oauth2/callback/" + nodeId;
		} else {
			URI = Play.application().configuration().getString("application.URL.DEV") + 
					"/app/oauth2/callback/" + nodeId;
		}

		return URI;
	}
	
	
	/**
	 * OAuth 2 specific method to get the access. Makes OAuth auth calls 
	 * to authorize, get token and refresh token
	 */
	public static String getAccess(String userId, String nodeId, String clientId, String clientSecret, 
			OAuth2AuthNode oauthNode, OAuth2AccessType accessType, String data, String authorizeURL, String tokenURL) {
		
		String accessToken = null;
		String refreshToken = null;
		Integer expiresIn = null;
		
		switch(accessType) {
			case OAUTH2_AUTHORIZE:
				return authorizeURL + "?response_type=code&client_id="+
					clientId +"&state=authenticated&redirect_uri=" + getRedirectURI(nodeId);
				
			case OAUTH2_TOKEN:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth 2 code empty for Node Id: " + nodeId + " token call");

				JsonNode json = WS.url(tokenURL)
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("grant_type=authorization_code&code="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				accessToken = json.get("access_token").asText();
				refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				// save the various OAuth2 tokens after successful token call
				oauthNode.saveAccessToken(userId, nodeId, accessToken);
				oauthNode.saveRefreshToken(userId, nodeId, refreshToken);
				oauthNode.saveAccessTokenExpirationTime(userId, nodeId, calendar.getTime());
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token saved for user [" + userId + "] node [" + nodeId + "]");
				
				return accessToken;
				
			case OAUTH2_RENEW:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth 2 refresh token empty for Node Id: " + nodeId + " refresh call");

				json = WS.url(tokenURL)
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("grant_type=refresh_token&refresh_token="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				if (json.get("error")!=null) {
					throw new RuntimeException(json.get("error_description").asText());
				}
				
				accessToken = json.get("access_token").asText();
				if (json.get("refresh_token")!=null)
					refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				// save the various OAuth2 tokens after successful refresh
				oauthNode.saveAccessToken(userId, nodeId, accessToken);
				oauthNode.saveRefreshToken(userId, nodeId, refreshToken);
				oauthNode.saveAccessTokenExpirationTime(userId, nodeId, calendar.getTime());
				
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token renewed for user [" + userId + "] node [" + nodeId + "]");

				return accessToken;
			
			default:
				return null;
		}
	}

}
