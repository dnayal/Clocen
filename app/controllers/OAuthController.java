package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;
import models.ServiceAccessToken;
import models.UserServiceNode;
import nodes.Node;
import nodes.Node.AccessType;
import play.mvc.*;

public class OAuthController extends Controller {
	
	private static final String COMPONENT_NAME = "OAuth Controller";

	public static Result authorizeCall(String nodeId) {
		Node node = ServiceNodeHelper.getNode(nodeId);
    	return redirect(node.authorize(AccessType.OAUTH_AUTHORIZE, null));
    }
    
    
    public static Result tokenCallback(String nodeId) {
    	String code = request().getQueryString("code");
    	if (UtilityHelper.isEmptyString(code)) {
        	String error = request().getQueryString("error");
        	String errorDescription = request().getQueryString("error_description");
    		UtilityHelper.logError(COMPONENT_NAME, "tokenCallback", "Code not received for Node Id - " + nodeId + " Error - " + error, new RuntimeException(errorDescription));
    		return TODO;
    	} else {
    		UtilityHelper.logMessage(COMPONENT_NAME, "tokenCallback", "Code Received - " + code);
    		Node node = ServiceNodeHelper.getNode(nodeId);
    		node.authorize(AccessType.OAUTH_TOKEN, code);
			return redirect(routes.Application.index());
    	}
    }
    
    
    public static Result refreshToken(String nodeId) {
    	UserServiceNode key = new UserServiceNode(session("user_id"), nodeId);
    	ServiceAccessToken token = ServiceAccessToken.getServiceAccessToken(key);
    	token.refreshToken();
    	UtilityHelper.logMessage(COMPONENT_NAME, "refreshToken", "Refresh token - " + token);
		return redirect(routes.Application.index());
    }

}
