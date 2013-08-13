package controllers;

import nodes.Helper;
import nodes.Node;
import nodes.Node.AccessType;
import nodes.asana.Asana;
import nodes.box.Box;
import play.Logger;
import play.data.DynamicForm;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
  
    
    public static Result callTriggerListener(String nodeId) {
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	Logger.info("Item Id : " + form.get("item_id"));
    	Logger.info("From User Id : " + form.get("from_user_id"));
        Logger.info("Item Name : " + form.get("item_name"));
        Logger.info("Event Type : " + form.get("event_type"));
        Logger.info("Call triggered by - " + nodeId);
        Box box = new Box();
        box.getFile(form.get("item_id"));
        return ok();
    }
    

    public static Result redirectAuthenticationCall(String nodeId) {
        Node node = null;
        if (nodeId.equalsIgnoreCase("box"))
        	node = new Box();
        else if (nodeId.equalsIgnoreCase("asana"))
        	node = new Asana();
    	return redirect(node.getAccess(AccessType.OAUTH_AUTHORIZE, null));
    }
    
    
    public static Result receiveOauthCallback(String nodeId) {
    	String code = request().getQueryString("code");
    	if (Helper.isEmptyString(code)) {
        	String error = request().getQueryString("error");
        	String errorDescription = request().getQueryString("error_description");
    		Logger.error("Code not received for Node Id - " + nodeId);
    		Logger.error("Error - " + error + " :: Error Description - " + errorDescription);
    		return TODO;
    	} else {
    		Logger.info("Code - " + code);
    		Node node = null;
    		if (nodeId.equalsIgnoreCase("box"))
    			node = new Box();
    		else if (nodeId.equalsIgnoreCase("asana"))
    			node = new Asana();
    		node.getAccess(AccessType.OAUTH_TOKEN, code);
			return TODO;
    	}
    }

}
