package models;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PersistenceException;

import nodes.Node;
import nodes.Node.AccessType;
import play.db.ebean.Model;

/**
 * Model to save save access tokens for users
 */
@SuppressWarnings("serial")
@Entity
public class ServiceAccessToken extends Model {

	private static final String COMPONENT_NAME = "ServiceAccessToken Model";
	
	@EmbeddedId
	private ServiceAccessTokenKey key;
	@Column(length=100)
	private String accessToken;
	@Column(length=100)
	private String refreshToken;
	private Date expirationTime;
	private Date createTimestamp;
	private static Finder<ServiceAccessTokenKey, ServiceAccessToken> find = new Finder<ServiceAccessTokenKey, ServiceAccessToken>(ServiceAccessTokenKey.class, ServiceAccessToken.class);

	
	public ServiceAccessToken(ServiceAccessTokenKey key, String accessToken, String refreshToken, 
			Date expirationTime, Date createTimestamp) {
		this.key = key;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime;
		this.createTimestamp = createTimestamp;
	}

	
	public ServiceAccessToken(String userId, String nodeId, String accessToken, String refreshToken, 
			Date expirationTime, Date createTimestamp) {
		this.key = new ServiceAccessTokenKey(userId, nodeId);
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime;
		this.createTimestamp = createTimestamp;
	}

	
	public ServiceAccessTokenKey getKey() {
		return key;
	}

	
	public void setKey(ServiceAccessTokenKey key) {
		this.key = key;
	}


	public String getAccessToken() {
		String result = null;
		// if the token has expired
		if(getExpirationTime().before(Calendar.getInstance().getTime())) {
			UtilityHelper.logMessage(COMPONENT_NAME, "getAccessToken()", "Token Expired - user:" + getKey().getUserId() + " node:" + getKey().getNodeId());
			// try refreshing it
			if(refreshToken()) {
				// if refresh is successful, get the updated access token
				// the accesstoken of this object will remain the 
				// same, even if the data in database is updated
				ServiceAccessTokenKey updatedKey = new ServiceAccessTokenKey(getKey().getUserId(), getKey().getNodeId());
				ServiceAccessToken updatedToken = ServiceAccessToken.getServiceAccessToken(updatedKey);
				// update the access token with updated token values
				setAccessToken(updatedToken.getAccessToken());
				setExpirationTime(updatedToken.getExpirationTime());
				setRefreshToken(updatedToken.getRefreshToken());
				
				result = accessToken;
			} else {
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccessToken()", "Removing Token - user:" + getKey().getUserId() + " node:" + getKey().getNodeId());
			}
		} else {
			// if it has not expired return the access token
			result = accessToken;
		}

		return result;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}
	
	public Date getCreateTimestamp() {
		return createTimestamp;
	}
	
	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	

	public static ServiceAccessToken getServiceAccessToken(ServiceAccessTokenKey key) {
		return find.byId(key);
	}
	
	public static List<ServiceAccessToken> getAllServiceAccessTokensForUser(String userId) {
		List<ServiceAccessToken> tokenList = new ArrayList<ServiceAccessToken>();

		tokenList = ServiceAccessToken.find.where()
				.eq("user_id", userId)
				.findList();
		
		return tokenList;
	}
	
	public Boolean refreshToken() {
		Boolean success = false;
		
		if(!UtilityHelper.isEmptyString(refreshToken)) {
			Node node = ServiceNodeHelper.getNode(key.getNodeId());
			try {
				if(node.authorize(key.getUserId(), AccessType.OAUTH_RENEW, refreshToken)!=null)
					success = true;
				else {
					throw new RuntimeException();
				}
			} catch (Exception exception) {
				UtilityHelper.logError(COMPONENT_NAME, "refreshToken()", "Unable to refresh token for userId:" + key.getUserId() + " nodeId:"+ key.getNodeId(), exception);
				delete();
				success = false;
			}
		}
		
		return success;
	}
	
	
	@Override
	public void delete() {
		super.delete();
	}
	
	
	@Override
	public void save() {
		try {
			ServiceAccessToken token = getServiceAccessToken(key);
			
			if(token==null)
				super.save();
			else
				super.update();
			
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}
	
	
	@Override
	public String toString() {
		return "ServiceAccessToken [key=" + key + ", accessToken="
				+ accessToken + ", refreshToken=" + refreshToken
				+ ", expirationTime=" + expirationTime + ", createTimestamp="
				+ createTimestamp + "]";
	}

}
