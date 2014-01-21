package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;

import play.cache.Cache;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class NodeController extends Controller {
	
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
	 * Return the json configuration of the given node.
	 * This is a generic info, so access it from cache whenever possible
	 */
	public static Result getNodeConfiguration(String nodeId) {
		String node = "node." + nodeId + ".config";
		JsonNode result = null;
		
		Object object = Cache.get(node);
		
		if (object != null) {
			result = (JsonNode) object;
		} else {
			result = ServiceNodeHelper.getNodeConfiguration(nodeId);
			Cache.set(node, result);
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


	/***************
	 * Execute trigger call service 	
	 **/
	public static Result callNodeTrigger(String nodeId) {
		
		if(UtilityHelper.isEmptyString(nodeId))
			return badRequest();
		
		Node node = ServiceNodeHelper.getNode(nodeId);
		DynamicForm form = DynamicForm.form().bindFromRequest();
		
		if(form.hasErrors())
			return badRequest();
		
		node.executeTrigger(form);
        return ok();
    }

}
