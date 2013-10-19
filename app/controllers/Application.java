package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;

import java.util.List;
import java.util.Map;

import models.BetaUser;
import models.ServiceNodeInfo;
import models.User;
import nodes.box.Box;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.create_process;
import views.html.error_page;
import views.html.forgot_password;
import views.html.index;
import views.html.login;
import views.html.oauth_callback;
import views.html.register;
import views.html.user_home;
import views.html.user_profile;
import views.html.view_process;

public class Application extends Controller {

	private static final String COMPONENT_NAME = "Application Controller";

	public static Result index() {
		User user = User.getCurrentUser();
		Form<User> userForm = Form.form(User.class);

		if(user!=null) {
			Promise<Response> response = WS.url(routes.ProcessController.getAllProcessesForUser(user.getUserId()).absoluteURL(request())).get();
			JsonNode allProcesses = Json.parse(response.get().getBody());
			
			List<ServiceNodeInfo> list = user.getAllNodes();
			return ok(user_home.render(Json.toJson(list), allProcesses));
		} else {
			return ok(index.render(userForm));
		}
        
    }
    	
	public static Result getLoginPage() {
		Form<User> form = Form.form(User.class);
		return ok(login.render(form));
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
		return ok(user_profile.render(user, userForm, UtilityHelper.getCountries()));
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
	
/***************
 * PROCESS CALLS 	
 **/
    public static Result viewCreateProcess() {
        if(!User.isLoggedIn())
        	return redirect(routes.Application.index());

        return ok(create_process.render());
    }
    
    
	public static Result viewProcess(String processId) {
		Promise<Response> response = WS.url(routes.ProcessController.getProcess(processId).absoluteURL(request())).get();
		JsonNode processJSON = response.get().asJson();
		return ok(view_process.render(processJSON));
	}
	
    public static Result saveProcess() {
		User user = User.getCurrentUser();
		
		String requestBody = UtilityHelper.convertMapToRequestString(request().body().asFormUrlEncoded());
		
		if(user==null || UtilityHelper.isEmptyString(requestBody))
			return internalServerError(error_page.render());

		Promise<Response> response = WS.url(routes.ProcessController.saveProcess(user.getUserId()).absoluteURL(request()))
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post(requestBody);
		if (response.get().getStatus()==OK)
			return redirect(routes.Application.index());
		else
			return internalServerError(error_page.render());
    }
    
    
	public static Result deleteProcess(String processId) {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.ProcessController.deleteProcess(processId, user.getUserId()).absoluteURL(request())).post(processId);
		if (response.get().getStatus()==OK)
			return ok();
		else
			return internalServerError(error_page.render());
	}
	
	
	public static Result pauseProcess(String processId) {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.ProcessController.pauseProcess(processId, user.getUserId()).absoluteURL(request())).post(processId);
		if (response.get().getStatus()==OK)
			return ok();
		else
			return internalServerError(error_page.render());
	}
	

/***************
 * NODE CALLS 	
 **/
	public static Result getAllNodes() {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.NodeController.getAllNodes(user.getUserId()).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
	public static Result getNode(String nodeId) {
		Promise<Response> response = WS.url(routes.NodeController.getNodeConfiguration(nodeId).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
	public static Result getNodeService(String nodeId, String service) {
		User user = User.getCurrentUser();
		
		Promise<Response> response = WS.url(routes.NodeController.callNodeInfoService(user.getUserId(), nodeId, service).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
/***************
 * OAUTH 2 CALLS 	
 **/
	public static Result authorizeOauth2Call(String nodeId) {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.OAuth2Controller.authorizeCall(user.getUserId(), nodeId).absoluteURL(request())).get();
    	return redirect(response.get().getBody());
    }
    
    
    public static Result oauth2TokenCallback(String nodeId) {
		User user = User.getCurrentUser();
		String requestString = UtilityHelper.convertMapToRequestString(request().queryString());

		Promise<Response> response = WS.url(routes.OAuth2Controller.tokenCallback(user.getUserId(), nodeId).absoluteURL(request()))
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post(requestString);
		/**
		 * TODO Show appropriate page or response based 
		 * on approval or rejection of oauth request by user  
		 */
		if (response.get().getStatus()==OK)
			return ok(oauth_callback.render());
		else
			return internalServerError(error_page.render());
    }
    

    public static Result refreshOauth2Token(String nodeId) {
		User user = User.getCurrentUser();
    	
		Promise<Response> response = WS.url(routes.OAuth2Controller.refreshToken(user.getUserId(), nodeId).absoluteURL(request()))
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post(nodeId);

		/**
		 * TODO Show appropriate page or response based 
		 * on approval or rejection of oauth request by user  
		 */
		if (response.get().getStatus()==OK)
			return ok(oauth_callback.render());
		else
			return internalServerError(error_page.render());
    }
    
/***************
 * TEMP STUFF 	
 **/
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
