package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import play.api.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class NodeController extends Controller {

	private static final String COMPONENT_NAME = "Node Controller";

	
    /**
     * Get all the nodes for the user, including information 
     * identifying which nodes have been authorized by the user
     */
	public static Result getAllNodes(String userId) {
    	User user = User.getUser(userId);
    	JsonNode json = Json.toJson(user.getAllNodes());
    	return ok(json);
    }
    
	
	/**
	 * Return the json configuration of the givn node
	 */
	public static Result getNodeConfiguration(String nodeId) {
		JsonNode result = null;
		try {
			JsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(Play.current().classloader().getResourceAsStream("nodes/" + nodeId + ".json"));
			result = parser.readValueAsTree();
			
			UtilityHelper.logMessage(COMPONENT_NAME, "getNodeConfiguration()", result.asText());
			
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getNodeConfiguration()", exception.getMessage(), exception);
		}
    	return ok(result);
    }
    
	
	/**
	 * Call the info service for the given node. 
	 * Used to power values for the input drop downs 
	 */
	public static Result callNodeInfoService(String userId, String nodeId, String service) {
		User user = User.getUser(userId);
		Node node = ServiceNodeHelper.getNode(nodeId);
		return ok(node.callInfoService(user, service));
	}

}
