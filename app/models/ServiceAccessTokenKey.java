package models;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Composite primary key for Service Access Tokens
 */
@Embeddable
public class ServiceAccessTokenKey {

	@Column(length=100)
	private String userId;
	@Column(length=20)
	private String nodeId;

	public ServiceAccessTokenKey(String userId, String nodeId) {
		this.userId = userId;
		this.nodeId = nodeId;
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceAccessTokenKey) {
			ServiceAccessTokenKey node = ((ServiceAccessTokenKey)obj);
			return node.getNodeId().equalsIgnoreCase(getNodeId()) && node.getUserId().equalsIgnoreCase(getUserId());
		}
		return false;
	}
	

	@Override
	public int hashCode() {
		return userId.hashCode();
	}

	
	@Override
	public String toString() {
		return "ServiceAccessTokenKey [userId=" + userId + ", nodeId=" + nodeId
				+ "]";
	}
}
