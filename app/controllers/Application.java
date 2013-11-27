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
import play.mvc.Http.Cookie;
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

	
	/**
	 * Method checks whether the current user is remembered or not 
	 */
	private static Boolean isUserRemembered() {
		Cookie cookie = request().cookie(UtilityHelper.REMEMBER_ME);
		if (cookie == null) {
			UtilityHelper.logMessage(COMPONENT_NAME, "isUserRemembered()", "Remember Me cookie for current user not available");
			return false;
		}
		String userId = SecurityHelper.decrypt(cookie.value());
		User user = User.getUser(userId);
		if(user == null) {
			UtilityHelper.logMessage(COMPONENT_NAME, "isUserRemembered()", "Invalid user remembered");
			return false;
		} else {
			User.setCurrentUser(user);
			UtilityHelper.logMessage(COMPONENT_NAME, "isUserRemembered()", user.getName() + " remembered");
			return true;
		}
	}

	
	public static Result index() {

		isUserRemembered();
		User user = User.getCurrentUser();
		Form<User> userForm = Form.form(User.class);

		if(user!=null) {
			Promise<Response> response = WS.url(routes.ProcessController.getAllProcessesForUser(user.getUserId()).absoluteURL(request())).get();
			JsonNode allProcesses = Json.parse(response.get().getBody());
			
			List<ServiceNodeInfo> list = user.getAllNodes();
			UtilityHelper.logMessage(COMPONENT_NAME, "index()", "Nodes and Processes retrieved for user : " + user.getName() + "[" + user.getUserId() + "]");
			
			return ok(user_home.render(Json.toJson(list), allProcesses));
		} else {
			return ok(index.render(userForm));
		}
        
    }
    	
	
	public static Result getLoginPage() {
		Form<User> form = Form.form(User.class);
		return ok(login.render(form));
	}
	
	
    /**
     * Displays the registration form for beta users. This method should 
     * be called by the URL that is sent to the beta users in the email.
     * The URL will have a parameter "key" which will have the encrypted 
     * email id, which itself is used for displaying the registration form
     */
	public static Result getRegistrationForm() {
    	String encryptedEmail = request().getQueryString("key");
    	String email = SecurityHelper.decrypt(encryptedEmail);
    	
    	if(email==null) {
    		UtilityHelper.logMessage(COMPONENT_NAME, "getRegistrationForm()", "Invalid email for Registration Form");
    		return internalServerError(error_page.render());
    	}

    	BetaUser betaUser = BetaUser.getBetaUser(email);
    	if(betaUser==null) {
    		UtilityHelper.logMessage(COMPONENT_NAME, "getRegistrationForm()", "Beta user not available for given email");
            return internalServerError(error_page.render());
    	} else {
    		User user = new User(null, null, betaUser.getEmail(), null, null, null, null);
    		Form<User> form = Form.form(User.class).fill(user);
    		UtilityHelper.logMessage(COMPONENT_NAME, "getRegistrationForm()", "Beta user retrieved for registration");
    		return ok(register.render(form));
    	}
    }
  
    
	/**
	 * Get the profile page of the current user
	 */
	public static Result getUserProfilePage() {
		User user = User.getCurrentUser();
		
		if(user==null)
			return redirect(routes.Application.index());

		Form<User> userForm = Form.form(User.class).fill(user);
		return ok(user_profile.render(user, userForm, UtilityHelper.getCountries()));
	}
	
	
	/**
	 * Show the Forgot/Reset password page. For Reset request the timestamp 
	 * and user id are passed as keys in the URL in email. If the reset 
	 * request is not executed within 24 hours of sending that email, 
	 * it will not be executed. For Forgot request, the Forgot web page is 
	 * displayed to the user directly   
	 */
	public static Result getForgotAndResetPasswordPage(String requestType) {
		if(requestType.equalsIgnoreCase("reset")) {
			Form<User> form = Form.form(User.class);

			// look for userid and timestamp in the URL parameter
			Map<String, String[]> map = request().queryString();
			String userIds[] = map.get("key1");
			String timeStamps[] = map.get("key2");
			
			// if the user id or timestamp is invalid, it was a badrequest
			if(userIds==null || timeStamps==null) {
				form.reject("reset_error", "Invalid password reset request.");
				UtilityHelper.logMessage(COMPONENT_NAME, "getForgotAndResetPasswordPage()", "Invalid password reset request. Either user id or timestamp is null");
				return badRequest(forgot_password.render(form, User.PASSWORD_RESET_VIEW));
			}

			String userId = SecurityHelper.decrypt(userIds[0]);
			User user = User.getUser(userId);
			long createTimestamp = Long.parseLong(SecurityHelper.decrypt(timeStamps[0]));
			long timeDifference = (System.currentTimeMillis() - createTimestamp)/(1000*60*60);

			// if the user is null or request has expired, it was a bad request
			if (timeDifference>=24 || user == null) {
				form.reject("reset_error", "Invalid password reset request.");
				UtilityHelper.logMessage(COMPONENT_NAME, "getForgotAndResetPasswordPage()", "Invalid password reset request. Either user id is null or the request has expired");
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
    
    
	/**
	 * View the given process
	 */
    public static Result viewProcess(String processId) {
		Promise<Response> response = WS.url(routes.ProcessController.getProcess(processId).absoluteURL(request())).get();
		JsonNode processJSON = response.get().asJson();
		UtilityHelper.logMessage(COMPONENT_NAME, "viewProcess()", processJSON.asText());
		return ok(view_process.render(processJSON));
	}
	
	
    /**
     * Save the process, based on the input provided by the user
     */
    public static Result saveProcess() {
		User user = User.getCurrentUser();
		
		// convert the incoming HTML form into a string so that 
		// it can be passed onto the required service 
		String requestBody = UtilityHelper.convertMapToRequestString(request().body().asFormUrlEncoded());
		
		if(user==null || UtilityHelper.isEmptyString(requestBody))
			return internalServerError(error_page.render());

		// save the service
		Promise<Response> response = WS.url(routes.ProcessController.saveProcess(user.getUserId()).absoluteURL(request()))
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post(requestBody);
		
		if (response.get().getStatus()==OK) {
			UtilityHelper.logMessage(COMPONENT_NAME, "saveProcess()", "Process successfully saved for User : [" + user.getName() + "] Id : [" + user.getUserId() + "]");
			return redirect(routes.Application.index());
		} else {
			UtilityHelper.logMessage(COMPONENT_NAME, "saveProcess()", "Problem while saving process");
			return internalServerError(error_page.render());
		}
    }
    
    
	
    // Delete the process
    public static Result deleteProcess(String processId) {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.ProcessController.deleteProcess(processId, user.getUserId()).absoluteURL(request())).post(processId);
		if (response.get().getStatus()==OK) {
			UtilityHelper.logMessage(COMPONENT_NAME, "deleteProcess()", "Process [" + processId + "] successfully deleted for User : [" + user.getName() + "] Id : [" + user.getUserId() + "]");
			return ok();
		} else {
			UtilityHelper.logMessage(COMPONENT_NAME, "deleteProcess()", "Problem while deleting process");
			return internalServerError(error_page.render());
		}
	}
	
	
	
    // pause the process
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
	
    // Get all nodes for the current user
    public static Result getAllNodes() {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.NodeController.getAllNodes(user.getUserId()).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
	// Get the configuration information of the current node
    public static Result getNode(String nodeId) {
		Promise<Response> response = WS.url(routes.NodeController.getNodeConfiguration(nodeId).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
	/**
	 * Call a particular helper service provided by a Node and get 
	 * its results. This method is used to power the Create Process 
	 * form to fill in the input controls of a given Node
	 */
    public static Result getNodeService(String nodeId, String service) {
		User user = User.getCurrentUser();
		
		Promise<Response> response = WS.url(routes.NodeController.callNodeInfoService(user.getUserId(), nodeId, service).absoluteURL(request())).get();
    	return ok(response.get().asJson());
    }

	
/***************
 * OAUTH 2 CALLS 	
 **/
	
    /**
     * Redirect the user to the OAuth 2 authorization page of a given Node,
     * so that the user can grant the access for this application
     */
    public static Result authorizeOauth2Call(String nodeId) {
		User user = User.getCurrentUser();
		Promise<Response> response = WS.url(routes.OAuth2Controller.authorizeCall(user.getUserId(), nodeId).absoluteURL(request())).get();
    	return redirect(response.get().getBody());
    }
    
    
    /**
     * Callback method called by the target Node application for OAuth 2 authorization
     */
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
		if (response.get().getStatus()==OK) {
			UtilityHelper.logMessage(COMPONENT_NAME, "oauth2TokenCallback()", "Node [" + nodeId + "] callback successful");
			return ok(oauth_callback.render());
		} else {
			UtilityHelper.logMessage(COMPONENT_NAME, "oauth2TokenCallback()", "Problem with callback for Node [" + nodeId + "]");
			return internalServerError(error_page.render());
		}
    }
    

    /**
     * Refresh the OAuth 2 token for the given node
     */
    public static Result refreshOauth2Token(String nodeId) {
		User user = User.getCurrentUser();
    	
		Promise<Response> response = WS.url(routes.OAuth2Controller.refreshToken(user.getUserId(), nodeId).absoluteURL(request()))
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post(nodeId);

		/**
		 * TODO Show appropriate page or response based 
		 * on approval or rejection of oauth request by user  
		 */
		if (response.get().getStatus()==OK) {
			UtilityHelper.logMessage(COMPONENT_NAME, "refreshOauth2Token()", "Node [" + nodeId + "] token refresh successful");
			return ok(oauth_callback.render());
		} else {
			UtilityHelper.logMessage(COMPONENT_NAME, "refreshOauth2Token()", "Problem with token refresh for Node [" + nodeId + "]");
			return internalServerError(error_page.render());
		}
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
