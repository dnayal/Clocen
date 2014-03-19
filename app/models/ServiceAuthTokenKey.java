package models;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Composite primary key for Service Access Tokens
 */
@Embeddable
public class ServiceAuthTokenKey {

	@Column(length=100)
	private String userId;
	@Column(length=100)
	private String nodeId;
	@Column(length=100)
	private String token;

	
	public ServiceAuthTokenKey(String userId, String nodeId, String token) {
		this.userId = userId;
		this.nodeId = nodeId;
		this.token = token;
	}


	public String getUserId() {
		return userId;
	}

	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	public String getNodeId() {
		return nodeId;
	}

	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	
	public String getToken() {
		return token;
	}

	
	public void setToken(String token) {
		this.token = token;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceAuthTokenKey) {
			ServiceAuthTokenKey key = ((ServiceAuthTokenKey) obj);
			return key.getNodeId().equalsIgnoreCase(getNodeId()) 
					&& key.getUserId().equalsIgnoreCase(getUserId())
					&& key.getToken().equalsIgnoreCase(getToken());
		}
		
		return false;
	}
	

	@Override
	public int hashCode() {
		return userId.hashCode();
	}


	@Override
	public String toString() {
		return "ServiceAuthTokenKey [userId=" + userId + ", nodeId=" + nodeId + ", token=" + token + "]";
	}

}
