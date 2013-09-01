package models;

public class ServiceNodeInfo {

	String nodeId;
	String nodeName;
	String nodeDescription;
	String URL;
	Boolean isAuthorized=false;
	
	
	public ServiceNodeInfo() {}
	
	
	public ServiceNodeInfo(String nodeId, String nodeName, String nodeDescription, String URL, Boolean isAuthorized) {
		this.nodeId = nodeId;
		this.nodeName = nodeName;
		this.nodeDescription = nodeDescription;
		this.URL = URL;
		this.isAuthorized = isAuthorized;
	}
	
	
	public String getNodeId() {
		return nodeId;
	}
	
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	
	public String getNodeName() {
		return nodeName;
	}
	
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	
	public String getNodeDescription() {
		return nodeDescription;
	}
	
	
	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}
	
	
	public String getURL() {
		return URL;
	}
	
	
	public void setURL(String URL) {
		this.URL = URL;
	}
	
	
	public Boolean getIsAuthorized() {
		return isAuthorized;
	}
	
	
	public void setIsAuthorized(Boolean isAuthorized) {
		this.isAuthorized = isAuthorized;
	}


	@Override
	public String toString() {
		return "ServiceNodeInfo [nodeId=" + nodeId + ", nodeName=" + nodeName
				+ ", nodeDescription=" + nodeDescription + ", URL=" + URL
				+ ", isAuthorized=" + isAuthorized + "]";
	}
	
}
