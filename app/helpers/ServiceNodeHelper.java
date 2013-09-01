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
	
	private static final String COMPONENT_NAME = "Node Helper";
	
	
	public static String getRedirectURI(String nodeId) {
		return controllers.routes.OAuthController.tokenCallback(nodeId).absoluteURL(Controller.request());
	}
	
	
	public static String getAccess(String nodeId, String clientId, String clientSecret, 
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
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.post("grant_type=authorization_code&code="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				accessToken = json.get("access_token").asText();
				refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				ServiceAccessToken sat = new ServiceAccessToken(UserHelper.getCurrentUser().getUserId(), 
						nodeId, accessToken, refreshToken, calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				return accessToken;
				
			case OAUTH_RENEW:
				if(UtilityHelper.isEmptyString(data))
					throw new RuntimeException("OAuth refresh token empty for Node Id: " + nodeId + " refresh call");

				json = WS.url(tokenURL)
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.post("grant_type=refresh_token&refresh_token="+data+"&client_id=" + clientId +
						"&client_secret=" + clientSecret + "&redirect_uri=" + getRedirectURI(nodeId))
				.get().asJson();
				
				if (json.get("error")!=null) {
					UtilityHelper.logError(COMPONENT_NAME, "getAccess", json.get("error_description").asText(), new RuntimeException(json.get("error_description").asText()));
					return null;
				}
				
				accessToken = json.get("access_token").asText();
				if (json.get("refresh_token")!=null)
					refreshToken = json.get("refresh_token").asText();
				expiresIn = json.get("expires_in").asInt();
				calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, expiresIn);
				
				sat = new ServiceAccessToken(UserHelper.getCurrentUser().getUserId(), 
						nodeId, accessToken, refreshToken, calendar.getTime(), Calendar.getInstance().getTime());
				sat.save();
				
				return accessToken;
			
			default:
				return null;
		}
	}

	
	@SuppressWarnings("rawtypes")
	private static Node getNodeClass(File nodeDirectory) {
		Node node = null;
		File classes[] = nodeDirectory.listFiles();
		for(File nodeClass : classes) {
			String className = nodeClass.getName();
			// Get all classes in the node
			if(className.endsWith(".class")) {
				String fullClassName = "nodes." + nodeDirectory.getName() + "."+ className.substring(0, className.length()-(".class".length()));
				Object object = null;
				try {
					Class cls = Class.forName(fullClassName);
					object = cls.newInstance();
				} catch (ClassNotFoundException exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getNodeClass", exception.getMessage(), exception);
				} catch (InstantiationException exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getNodeClass", exception.getMessage(), exception);
				} catch (IllegalAccessException exception) {
					UtilityHelper.logError(COMPONENT_NAME, "getNodeClass", exception.getMessage(), exception);
				} 
				// Get node instance and add to HashMap
				if (object instanceof Node) 
					node = ((Node)object);
			}
		}
		
		return node;
	}

	
	public static Node getNode(String nodeId) {
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes/" + nodeId;
		File nodeDirectory = new File(classLoader.getResource(path).getFile());
		Node node = getNodeClass(nodeDirectory);
		return node;
	}
	
	
	public static HashMap<String, Node> getAllNodes() {
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		ClassLoader classLoader = Play.application().classloader();
		String path = "nodes";
		File nodesDirectory = new File(classLoader.getResource(path).getFile());
		File nodes[] = nodesDirectory.listFiles();

		for(File node: nodes) {
			// Get all node directories
			if(node.isDirectory()) {
				Node serviceNode = getNodeClass(node);
				nodeMap.put(node.getName(), serviceNode);
			}
		}
		
		return nodeMap;
	}
	
	
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
