package controllers;

import java.util.Map;

import helpers.SecurityHelper;
import helpers.UtilityHelper;
import models.BetaUser;
import models.User;
import nodes.box.Box;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.*;

public class Application extends Controller {

	private static final String COMPONENT_NAME = "Application Controller";

	public static Result index() {
        Form<User> userForm = Form.form(User.class);
		if(User.isLoggedIn())
        	return redirect(routes.UserController.home());
        
		return ok(index.render(userForm));
    }
    

    public static Result getCreateProcessPage() {
        if(!User.isLoggedIn())
        	return redirect(routes.Application.index());

        return ok(create_process.render());
    }
    
    
    public static Result getRegistrationForm() {
    	String encryptedEmail = request().getQueryString("key");
    	String email = SecurityHelper.decrypt(encryptedEmail);
    	
    	if(email==null)
            return internalServerError(error_page.render());

    	BetaUser betaUser = BetaUser.getBetaUser(email);
    	if(betaUser==null) {
            return internalServerError(error_page.render());
    	} else {
    		User user = new User(null, null, betaUser.getEmail(), null, null, null, null);
    		Form<User> form = Form.form(User.class).fill(user);
    		return ok(register.render(form));
    	}
    }
  
    
	public static Result getUserProfilePage() {
		User user = User.getCurrentUser();
		
		if(user==null)
			return redirect(routes.Application.index());

		Form<User> userForm = Form.form(User.class).fill(user);
		return ok(user_profile.render(user, userForm));
	}
	
	
	public static Result getForgotAndResetPasswordPage(String requestType) {
		if(requestType.equalsIgnoreCase("reset")) {
			Form<User> form = Form.form(User.class);

			Map<String, String[]> map = request().queryString();
			String userIds[] = map.get("key1");
			String timeStamps[] = map.get("key2");
			
			if(userIds==null || timeStamps==null) {
				form.reject("reset_error", "Invalid password reset request.");
				return badRequest(forgot_password.render(form, User.PASSWORD_RESET_VIEW));
			}

			String userId = SecurityHelper.decrypt(userIds[0]);
			User user = User.getUser(userId);
			long createTimestamp = Long.parseLong(SecurityHelper.decrypt(timeStamps[0]));
			long timeDifference = (System.currentTimeMillis() - createTimestamp)/(1000*60*60);

			if (timeDifference>=24 || user == null) {
				form.reject("reset_error", "Invalid password reset request.");
				return badRequest(forgot_password.render(form, User.PASSWORD_RESET_VIEW));
			}
			
			User.setCurrentUser(user);
			
			return ok(forgot_password.render(Form.form(User.class).fill(user), User.PASSWORD_RESET_VIEW));
			
		} else if (requestType.equalsIgnoreCase("forgot")) {
			return ok(forgot_password.render(Form.form(User.class), User.PASSWORD_FORGOT_VIEW));
		} else {
			return ok(forgot_password.render(Form.form(User.class), User.PASSWORD_FORGOT_VIEW));
		}
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
