package auth;

import java.util.Date;
import java.util.List;

import models.ServiceAuthToken;
import nodes.Node;

public interface OAuth2AuthNode extends Node {

	public static enum OAuth2AccessType {
		OAUTH2_AUTHORIZE, 
		OAUTH2_TOKEN,
		OAUTH2_RENEW
	};
	
	public String authorize(String userId, OAuth2AccessType accessType, String data);
	
	public String getAccessToken(List<ServiceAuthToken> serviceTokens);
	
	public void saveAccessToken(String userId, String nodeId, String accessToken);
	
	public String getRefreshToken(List<ServiceAuthToken> serviceTokens);
	
	public void saveRefreshToken(String userId, String nodeId, String refreshToken);
	
	public Date getAccessTokenExpirationTime(List<ServiceAuthToken> serviceTokens);
	
	public void saveAccessTokenExpirationTime(String userId, String nodeId, Date accessTokenExpirationTime);
	
	public Boolean refreshAccessToken(String userId, String nodeId);
	
	public String getUserId(List<ServiceAuthToken> serviceTokens);
	
}
