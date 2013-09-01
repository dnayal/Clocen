package models;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PersistenceException;

import nodes.Node;
import nodes.Node.AccessType;

import play.db.ebean.Model;

@SuppressWarnings("serial")
@Entity
public class ServiceAccessToken extends Model {

	private static final String COMPONENT_NAME = "ServiceAccessToken Model";
	
	@EmbeddedId
	UserServiceNode userNode;
	
	@Column(length=100)
	String accessToken;
	
	@Column(length=100)
	String refreshToken;

	Date expirationTime;

	Date createTimestamp;
	
	public static Finder<UserServiceNode, ServiceAccessToken> find = new Finder<UserServiceNode, ServiceAccessToken>(UserServiceNode.class, ServiceAccessToken.class);

	public ServiceAccessToken() {}
	
	public ServiceAccessToken(UserServiceNode userNode, String accessToken, String refreshToken, 
			Date expirationTime, Date createTimestamp) {
		this.userNode = userNode;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime;
		this.createTimestamp = createTimestamp;
	}

	public ServiceAccessToken(String userId, String nodeId, String accessToken, String refreshToken, 
			Date expirationTime, Date createTimestamp) {
		this.userNode = new UserServiceNode(userId, nodeId);
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime;
		this.createTimestamp = createTimestamp;
	}

	public UserServiceNode getUserNode() {
		return userNode;
	}

	public void setUserNode(UserServiceNode userNode) {
		this.userNode = userNode;
	}

	public String getAccessToken() {
		return accessToken;
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
	

	public static ServiceAccessToken getServiceAccessToken(UserServiceNode key) {
		return find.byId(key);
	}
	
	
	public Boolean refreshToken() {
		Boolean success = false;
		
		if(!UtilityHelper.isEmptyString(refreshToken)) {
			Node node = ServiceNodeHelper.getNode(userNode.getNodeId());
			try {
				if(node.authorize(AccessType.OAUTH_RENEW, refreshToken)!=null)
					success = true;
				else {
					throw new RuntimeException();
				}
			} catch (Exception exception) {
				UtilityHelper.logError(COMPONENT_NAME, "refreshToken", "Unable to refresh token for userId:" + userNode.getUserId() + " nodeId:"+ userNode.getNodeId(), new RuntimeException());
				delete();
				success = false;
			}
		}
		
		return success;
	}
	
	
	@Override
	public void delete() {
		UtilityHelper.logMessage(COMPONENT_NAME, "delete", "Deleting ServiceAccessToken...");
		super.delete();
	}
	
	
	@Override
	public void save() {
		try {
			super.save();
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save", exception.getMessage(), exception);
			update();
		}
	}

	
	@Override
	public void update() {
		super.update();
	}

	
	@Override
	public String toString() {
		return "ServiceAccessToken [userNode=" + userNode + ", accessToken="
				+ accessToken + ", refreshToken=" + refreshToken
				+ ", expirationTime=" + expirationTime + ", createTimestamp="
				+ createTimestamp + "]";
	}

}
