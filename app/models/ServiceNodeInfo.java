package models;

public class ServiceNodeInfo {

	private String nodeId;
	private String nodeName;
	private String nodeLogo;
	private String nodeDescription;
	private String URL;
	private Boolean authorized=false;
	
	
	public ServiceNodeInfo(String nodeId, String nodeName, String nodeLogo, String nodeDescription, String URL, Boolean authorized) {
		this.nodeId = nodeId;
		this.nodeName = nodeName;
		this.nodeLogo = nodeLogo;
		this.nodeDescription = nodeDescription;
		this.URL = URL;
		this.authorized = authorized;
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
	
	
	public Boolean isAuthorized() {
		return authorized;
	}


	public void setAuthorized(Boolean authorized) {
		this.authorized = authorized;
	}
	

	public String getNodeLogo() {
		return nodeLogo;
	}


	public void setNodeLogo(String nodeLogo) {
		this.nodeLogo = nodeLogo;
	}


	@Override
	public String toString() {
		return "ServiceNodeInfo [nodeId=" + nodeId + ", nodeName=" + nodeName
				+ ", nodeDescription=" + nodeDescription + ", URL=" + URL
				+ ", authorized=" + authorized + "]";
	}

}
