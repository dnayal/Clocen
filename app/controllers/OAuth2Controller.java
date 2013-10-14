package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;
import models.ServiceAccessToken;
import models.ServiceAccessTokenKey;
import nodes.Node;
import nodes.Node.AccessType;
import play.data.DynamicForm;
import play.mvc.*;

public class OAuth2Controller extends Controller {
	
	private static final String COMPONENT_NAME = "OAuth2 Controller";

	public static Result authorizeCall(String userId, String nodeId) {
		Node node = ServiceNodeHelper.getNode(nodeId);
    	return ok(node.authorize(userId, AccessType.OAUTH_AUTHORIZE, null));
    }
    
    
    public static Result tokenCallback(String userId, String nodeId) {
    	DynamicForm form = DynamicForm.form().bindFromRequest();
    	String code = form.get("code");

    	if (UtilityHelper.isEmptyString(code)) {
        	String error = request().getQueryString("error");
        	String errorDescription = request().getQueryString("error_description");
    		UtilityHelper.logError(COMPONENT_NAME, "tokenCallback", "Code not received for Node Id - " + nodeId + " Error - " + error, new RuntimeException(errorDescription));
    		return badRequest();
    	} else {
    		UtilityHelper.logMessage(COMPONENT_NAME, "tokenCallback", "Code Received - " + code);
    		Node node = ServiceNodeHelper.getNode(nodeId);
    		node.authorize(userId, AccessType.OAUTH_TOKEN, code);
			return ok();
    	}
    }
    
    
    public static Result refreshToken(String userId, String nodeId) {
    	ServiceAccessTokenKey key = new ServiceAccessTokenKey(userId, nodeId);
    	ServiceAccessToken token = ServiceAccessToken.getServiceAccessToken(key);
    	token.refreshToken();
    	UtilityHelper.logMessage(COMPONENT_NAME, "refreshToken", "Refresh token - " + token);
		return redirect(routes.Application.index());
    }

}
