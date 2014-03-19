package auth;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import models.ServiceAuthToken;
import models.ServiceAuthTokenKey;
import nodes.Node;

/**
 * This is the default implementation of OAuth 2. 
 * Nodes can leverage this implementation or if required can override it
 *
 */
public abstract class OAuth2NodeDefaultImpl implements OAuth2AuthNode {

	private static final String COMPONENT_NAME = "OAuth2NodeDefaultImpl";
	private static final String EXPIRATION_DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";

	/** default OAuth 2 tokens **/
	protected final String TOKEN_ACCESS = "access_token";
	protected final String TOKEN_REFRESH = "refresh_token";
	protected final String TOKEN_EXPIRES_IN = "expires_in";
	
	
	/**
	 * Default implementation to get OAuth 2 access token
	 */
	@Override
	public String getAccessToken(List<ServiceAuthToken> serviceTokens) {
		// if the access token has expired
		if(getAccessTokenExpirationTime(serviceTokens).before(UtilityHelper.getCurrentTime())) {
			// get its refresh_token
			String refreshToken = getRefreshToken(serviceTokens);
			// get the first service access token, so that you can use that for getting user id and node id
			ServiceAuthTokenKey firstKey = serviceTokens.get(0).getKey();
			// now try to refresh the access_token
			if(refreshAccessToken(firstKey.getUserId(), firstKey.getNodeId(), refreshToken)) {
				// if it is successful
				// update the service access tokens with the new ones
				serviceTokens = 
						ServiceAuthToken.getServiceAuthTokens(firstKey.getUserId(), firstKey.getNodeId());
			} else {
				// if the refresh token was not successful
				return null;
			}
		}
		
		// loop through all the service access tokens provided
		for(ServiceAuthToken accessToken: serviceTokens) {
			ServiceAuthTokenKey key = accessToken.getKey();
			// if you find the one matching access_token, return its value
			if(key.getToken().equalsIgnoreCase(TOKEN_ACCESS))
				return accessToken.getValue();
		}
		// if the call reaches here, no access_token was found
		return null;
	}


	/**
	 * Default implementation to set OAuth 2 access token
	 */
	@Override
	public void saveAccessToken(String userId, String nodeId, String accessToken) {
		ServiceAuthToken serviceToken = new ServiceAuthToken(
				userId, nodeId, TOKEN_ACCESS, accessToken, UtilityHelper.getCurrentTime());
		
		serviceToken.save();
		
	}


	/**
	 * Default implementation to get Oauth 2 refresh token
	 */
	@Override
	public String getRefreshToken(List<ServiceAuthToken> serviceTokens) {
		// loop through all the service access tokens provided
		for(ServiceAuthToken accessToken: serviceTokens) {
			ServiceAuthTokenKey key = accessToken.getKey();
			// if you find the one matching refresh_token, return its value
			if(key.getToken().equalsIgnoreCase(TOKEN_REFRESH))
				return accessToken.getValue();
		}
		// if the call reaches here, no refresh_token was found
		return null;
	}


	/**
	 * Default implementation to save the refresh token
	 */
	@Override
	public void saveRefreshToken(String userId, String nodeId, String refreshToken) {
		ServiceAuthToken accessToken = new ServiceAuthToken(
				userId, nodeId, TOKEN_REFRESH, refreshToken, UtilityHelper.getCurrentTime());
		accessToken.save();
	}


	/**
	 * Default implementation to get OAuth 2 expiration time
	 */
	@Override
	public Date getAccessTokenExpirationTime(List<ServiceAuthToken> serviceTokens) {
		// loop through all the service access tokens provided
		for(ServiceAuthToken accessToken: serviceTokens) {
			ServiceAuthTokenKey key = accessToken.getKey();
			
			// if you find the one matching refresh_token, return its value
			if(key.getToken().equalsIgnoreCase(TOKEN_EXPIRES_IN)) {
				try {
					return UtilityHelper.convertStringToDate(EXPIRATION_DATE_FORMAT, accessToken.getValue());
				} catch (ParseException exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getAccessTokenExpirationTime()", exception.getMessage(), exception);
				}
			}
		}
		// if the call reaches here, no expires_in was found
		return null;
	}


	/**
	 * Default implementation to set OAuth 2 expiration time
	 */
	@Override
	public void saveAccessTokenExpirationTime(String userId, String nodeId, Date accessTokenExpirationTime) {
		ServiceAuthToken accessToken = new ServiceAuthToken(
				userId, nodeId, TOKEN_EXPIRES_IN, accessTokenExpirationTime.toString(), UtilityHelper.getCurrentTime());
		accessToken.save();
	}


	/**
	 * Refreshes token for the given node. Returns true of the refresh is 
	 * successful else returns false
	 */
	@Override
	public Boolean refreshAccessToken(String userId, String nodeId) {
		ServiceAuthTokenKey key = new ServiceAuthTokenKey(userId, nodeId, TOKEN_REFRESH);
		ServiceAuthToken token = ServiceAuthToken.getServiceAuthToken(key);
		return refreshAccessToken(userId, nodeId, token.getValue());
	}
	
	
	/**
	 * Private method used for refreshing the access_token
	 */
	private Boolean refreshAccessToken(String userId, String nodeId, String refreshToken) {
		Boolean success = false;
		
		// if the refresh token is not an empty string
		if(!UtilityHelper.isEmptyString(refreshToken)) {
			// get the node
			Node node = ServiceNodeHelper.getNode(nodeId);
			try {
				// convert the node to OAuth2 Node, because the authorize 
				// method is in OAuth2AuthNode interface 
				OAuth2AuthNode oauthNode = (OAuth2AuthNode) node;
				// if the authorize call of the given node was successful
				// return true else throw RuntimeException
				if(oauthNode.authorize(userId, OAuth2AccessType.OAUTH2_RENEW, refreshToken)!=null)
					success = true;
				else
					throw new RuntimeException();

			} catch (Exception exception) {
				UtilityHelper.logError(COMPONENT_NAME, "refreshToken()", "Unable to refresh token for userId:" + userId + " nodeId:"+ nodeId, exception);
				success = false;
			}
		}
		
		return success;
	}


	/**
	 * Default OAuth 2 implementation of isAuthorized method
	 */
	@Override
	public Boolean isAuthorized(String userId) {
		List<ServiceAuthToken> serviceTokens = ServiceAuthToken.getServiceAuthTokens(userId, getNodeId());
		
		if(serviceTokens==null || serviceTokens.isEmpty()) {
			return false;
		}
			
		if(getAccessToken(serviceTokens)==null)
			return false;
		else 
			return true;
	}


	/**
	 * Default implementation to return user id from the service access tokens
	 */
	@Override
	public String getUserId(List<ServiceAuthToken> serviceTokens) {
		if (serviceTokens!=null && !serviceTokens.isEmpty())
			return serviceTokens.get(0).getKey().getUserId();
		else
			return null;
	}

}
