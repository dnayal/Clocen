package models;

import javax.persistence.Column;
import javax.persistence.Embeddable;

	@Embeddable
	public class UserServiceNode {
		@Column(length=100)
		String userId;

		@Column(length=20)
		String nodeId;

		public UserServiceNode(String userId, String nodeId) {
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
			if (obj instanceof UserServiceNode) {
				UserServiceNode node = ((UserServiceNode)obj);
				return node.getNodeId().equalsIgnoreCase(getNodeId()) && node.getUserId().equalsIgnoreCase(getUserId());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return userId.hashCode();
		}
	}
