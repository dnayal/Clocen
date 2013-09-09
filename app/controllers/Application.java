package controllers;

import helpers.UtilityHelper;
import nodes.box.Box;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.*;

public class Application extends Controller {

	private static final String COMPONENT_NAME = "Application Controller";

	public static Result index() {
        return ok(index.render());
    }
    

    public static Result getCreateProcessPage() {
    	return ok(create_process.render());
    }
  
    
    public static Result callTriggerListener(String nodeId) {
		DynamicForm form = DynamicForm.form().bindFromRequest();
    	UtilityHelper.logMessage(COMPONENT_NAME, "callTriggerListener", "Item Id : " + form.get("item_id"));
    	UtilityHelper.logMessage(COMPONENT_NAME, "callTriggerListener", "From User Id : " + form.get("from_user_id"));
    	UtilityHelper.logMessage(COMPONENT_NAME, "callTriggerListener", "Item Name : " + form.get("item_name"));
    	UtilityHelper.logMessage(COMPONENT_NAME, "callTriggerListener", "Event Type : " + form.get("event_type"));
    	UtilityHelper.logMessage(COMPONENT_NAME, "callTriggerListener", "Call triggered by - " + nodeId);
        Box box = new Box();
        box.getFile(form.get("item_id"));
        return ok();
    }
    

}
