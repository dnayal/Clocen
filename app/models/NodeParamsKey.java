package models;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Composite primary key for Node Params
 */
@Embeddable
public class NodeParamsKey {

	@Column(length=100)
	private String userId;
	
	@Column(length=20)
	private String nodeId;

	@Column(length=100)
	private String parameter;

	
	public NodeParamsKey(String userId, String nodeId, String parameter) {
		this.userId = userId;
		this.nodeId = nodeId;
		this.parameter = parameter;
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


	public String getParameter() {
		return parameter;
	}
	
	
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeParamsKey) {
			NodeParamsKey node = ((NodeParamsKey)obj);
			return node.getNodeId().equalsIgnoreCase(getNodeId()) 
					&& node.getUserId().equalsIgnoreCase(getUserId())
					&& node.getParameter().equalsIgnoreCase(getParameter());
		}
		return false;
	}
	

	@Override
	public int hashCode() {
		return userId.hashCode();
	}

	
	@Override
	public String toString() {
		return "NodeParamsKey [userId=" + userId + ", nodeId=" + nodeId + ", parameter=" + parameter + "]";
	}
}
