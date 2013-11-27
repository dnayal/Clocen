package helpers;


import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import org.codehaus.jackson.JsonNode;

import models.ServiceAccessToken;
import nodes.Node;
import nodes.Node.AccessType;
import play.Play;
import play.libs.WS;
import play.mvc.Controller;

public class ServiceNodeHelper {
	
	private static final String COMPONENT_NAME = "ServiceNode Helper";
	
	
	/**
	 * Private method for OAuth redirect URI using 
	 * the node id, used for OAuth callback
	 */
	private static String getRedirectURI(String nodeId) {
		return controllers.routes.Application.oauth2TokenCallback(nodeId).absoluteURL(Controller.request());
	}
	
	
	/**
	 * OAuth 2 specific method to get the access. Makes OAuth auth calls 
	 * to authorize, get token and refresh token
	 */
	public static String getAccess(String userId, String nodeId, String clientId, String clientSecret, 
			AccessType accessType, String data, String authorizeURL, String tokenURL) {
		
		String accessToken = null;
		String refreshToken = null;
		Integer expiresIn = null;
		
		switch(accessType) {
			case OAUTH_AUTHORIZE:
				return authorizeURL + "?response_type=code&client_id="+
					clientId +"&state=authenticated&redirect_uri=" + getRedirectURI(nodeId);
				
			case OAUTH_TOKEN:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth code empty for Node Id: " + nodeId + " token call");

				JsonNode json = WS.url(tokenURL)
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("grant_type=authorization_code&code="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				accessToken = json.get("access_token").asText();
				refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				ServiceAccessToken sat = new ServiceAccessToken(userId, nodeId, accessToken, 
						refreshToken, calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token saved for user [" + userId + "] node [" + nodeId + "]");
				
				return accessToken;
				
			case OAUTH_RENEW:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth refresh token empty for Node Id: " + nodeId + " refresh call");

				json = WS.url(tokenURL)
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("grant_type=refresh_token&refresh_token="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				if (json.get("error")!=null) {
					UtilityHelper.logError(COMPONENT_NAME, "getAccess()", json.get("error_description").asText(), new RuntimeException(json.get("error_description").asText()));
					return null;
				}
				
				accessToken = json.get("access_token").asText();
				if (json.get("refresh_token")!=null)
					refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				sat = new ServiceAccessToken(userId, nodeId, accessToken, refreshToken, 
						calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getAccess()", "Service token renewed for user [" + userId + "] node [" + nodeId + "]");

				return accessToken;
			
			default:
				return null;
		}
	}

	
	/**
	 * Private method to read the [node] directory, and then 
	 * returning the Node interface, by looping through all 
	 * classes in that folder and looking for the one that 
	 * implements Node interface  
	 */
	@SuppressWarnings("rawtypes")
	private static Node getNodeClass(File nodeDirectory) {
		Node node = null;
		// Get all files in the directory
		File classes[] = nodeDirectory.listFiles();
		
		// Loop through all files 
		for(File nodeClass : classes) {
			String className = nodeClass.getName();
			// Get all classes in the node
			if(className.endsWith(".class")) {
				// all nodes must follow this convention nodes.<<node name>>.<<Node class>>
				String fullClassName = "nodes." + nodeDirectory.getName() + "."+ className.substring(0, className.length()-(".class".length()));
				Object object = null;
				try {
					Class cls = Class.forName(fullClassName);
					// do not instantiate an interface
					if (!cls.isInterface())
						object = cls.newInstance();
				} catch (Exception exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getNodeClass()", exception.getMessage(), exception);
				} 
				// If the class is of type Node, then cast it 
				// to the Node interface
				if (object != null && object instanceof Node) 
					node = ((Node)object);
			}
		}
		
		return node;
	}

	
	/**
	 * Returns the Node interface based on the input node id.
	 * It leverages the convention that all nodes will be under package
	 * nodes.<<node package>>
	 */
	public static Node getNode(String nodeId) {
		ClassLoader classLoader = Play.application().classloader();
		// prepare the path of the node package using convention
		String path = "nodes/" + nodeId;

		// Load the directory for that Node
		File nodeDirectory = new File(classLoader.getResource(path).getFile());

		// Get the Node interface
		Node node = getNodeClass(nodeDirectory);
		
		return node;
	}
	
	
	/**
	 * Get all Nodes
	 */
	public static HashMap<String, Node> getAllNodes() {
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes";
		// Get the parent directory
		File nodesDirectory = new File(classLoader.getResource(path).getFile());
		// Get all nodes within the parent Node directory
		File nodes[] = nodesDirectory.listFiles();

		for(File node: nodes) {
			// If the node is a directory
			if(node.isDirectory()) {
				// pass it to get the Node instance
				Node serviceNode = getNodeClass(node);
				// if the node instance is valid
				if (serviceNode != null) {
					// add it to hashmap
					nodeMap.put(node.getName(), serviceNode);
				}
			}
		}
		
		return nodeMap;
	}
	
	
	/**
	 * Get the ids of all nodes. This using the convention 
	 * that all nodes are under the nodes parent directory 
	 * and have the same package name as the node id
	 */
	public static HashSet<String> getAllNodeIds() {
		HashSet<String> set = new HashSet<String>();
		
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes";
		File nodesDirectory = new File(classLoader.getResource(path).getFile());
		File nodes[] = nodesDirectory.listFiles();

		for(File node: nodes) {
			// Get all node directories
			if(node.isDirectory())
				set.add(node.getName());
		}
		
		return set;
	}
	
}
