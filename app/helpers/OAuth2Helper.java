package helpers;

import java.util.Calendar;

import models.ServiceAccessToken;
import nodes.Node.AccessType;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.libs.WS;
import play.mvc.Controller;

public class OAuth2Helper {

	private static final String COMPONENT_NAME = "OAuth 2 Helper";
	
	
	/**
	 * Private method for OAuth redirect URI using 
	 * the node id, used for OAuth callback
	 */
	private static String getRedirectURI(String nodeId) {
		String URI = null;
		try {
			URI = controllers.routes.Application.oauth2TokenCallback(nodeId).absoluteURL(Controller.request());
		}catch (Exception exception) {
			if(Play.isProd()) {
				URI = Play.application().configuration().getString("application.URL.PROD") + 
						"/app/oauth2/callback/" + nodeId;
			} else {
				URI = Play.application().configuration().getString("application.URL.DEV") + 
						"/app/oauth2/callback/" + nodeId;
			}
		}
		
		return URI;
	}
	
	
	/**
	 * OAuth 2 specific method to get the access. Makes OAuth auth calls 
	 * to authorize, get token and refresh token
	 */
	public static String getAccess(String userId, String nodeId, String clientId, String clientSecret, 
			AccessType accessType, String data, String authorizeURL, String tokenURL) {
		
		String accessToken = null;
		String refreshToken = null;
		Integer expiresIn = null;
		
		switch(accessType) {
			case OAUTH_AUTHORIZE:
				return authorizeURL + "?response_type=code&client_id="+
					clientId +"&state=authenticated&redirect_uri=" + getRedirectURI(nodeId);
				
			case OAUTH_TOKEN:
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
				
				ServiceAccessToken sat = new ServiceAccessToken(userId, nodeId, accessToken, 
						refreshToken, calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token saved for user [" + userId + "] node [" + nodeId + "]");
				
				return accessToken;
				
			case OAUTH_RENEW:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth 2 refresh token empty for Node Id: " + nodeId + " refresh call");

				json = WS.url(tokenURL)
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("grant_type=refresh_token&refresh_token="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				if (json.get("error")!=null) {
					UtilityHelper.logError(COMPONENT_NAME, "getAccess()", json.get("error_description").asText(), new RuntimeException(json.get("error_description").asText()));
					return null;
				}
				
				accessToken = json.get("access_token").asText();
				if (json.get("refresh_token")!=null)
					refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				sat = new ServiceAccessToken(userId, nodeId, accessToken, refreshToken, 
						calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token renewed for user [" + userId + "] node [" + nodeId + "]");

				return accessToken;
			
			default:
				return null;
		}
	}

}
