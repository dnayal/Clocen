package controllers;

import nodes.box.Box;
import play.Logger;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
  
    
    public static Result callTriggerListener(String nodeId) {
        return ok(index.render("Call triggered by - " + nodeId));
    }
    

    public static Result redirectAuthenticationCall(String nodeId) {
        Box box = new Box();
    	return redirect(box.getOauthAuthorizationURL());
    }
    
    public static Result receiveOauthCallback(String nodeId) {
    	String code = request().getQueryString("code");
    	if (code == null || code.equalsIgnoreCase("")) {
        	String error = request().getQueryString("error");
        	String errorDescription = request().getQueryString("error_description");
    		Logger.error("Code not received for Node Id - " + nodeId);
    		Logger.error("Error - " + error + " :: Error Description - " + errorDescription);
    		return TODO;
    	} else {
    		Logger.info("Code - " + code);
    		Box box = new Box();
    		box.authenticate(code);
			return TODO;
    	}
    }

}
