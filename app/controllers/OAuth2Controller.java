package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import auth.OAuth2AuthNode;
import auth.OAuth2AuthNode.OAuth2AccessType;

public class OAuth2Controller extends Controller {
	
	private static final String COMPONENT_NAME = "OAuth2 Controller";

	
	public static Result authorizeCall(String userId, String nodeId) {
		OAuth2AuthNode node = (OAuth2AuthNode) ServiceNodeHelper.getNode(nodeId);
		String result = node.authorize(userId, OAuth2AccessType.OAUTH2_AUTHORIZE, null);
		
		UtilityHelper.logMessage(COMPONENT_NAME, "authorizeCall()", result);
    	
		return ok(result);
    }
    
    
    public static Result tokenCallback(String userId, String nodeId) {
    	DynamicForm form = DynamicForm.form().bindFromRequest();
    	String code = form.get("code");

    	if (UtilityHelper.isEmptyString(code)) {
        	String error = request().getQueryString("error");
        	String errorDescription = request().getQueryString("error_description");
    		
        	UtilityHelper.logError(COMPONENT_NAME, "tokenCallback()", "Code not received for Node Id - " + nodeId + " Error - " + error, new RuntimeException(errorDescription));
    		
        	return badRequest();
    	} else {
    		UtilityHelper.logMessage(COMPONENT_NAME, "tokenCallback()", "Code Received for User [" + userId + "] Node [" + nodeId + "]");
    		
    		OAuth2AuthNode node = (OAuth2AuthNode) ServiceNodeHelper.getNode(nodeId);
    		node.authorize(userId, OAuth2AccessType.OAUTH2_TOKEN, code);
			return ok();
    	}
    }
    
    
    public static Result refreshToken(String userId, String nodeId) {
    	OAuth2AuthNode node = (OAuth2AuthNode) ServiceNodeHelper.getNode(nodeId);
    	node.refreshAccessToken(userId, nodeId);
    	
		UtilityHelper.logMessage(COMPONENT_NAME, "refreshToken()", "Refresh token for User [" + userId + "] Node [" + nodeId + "]");
		
		return redirect(routes.Application.index());
    }

}
