package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;
import models.BetaUser;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.betauser_thanks;
import views.html.error_page;
import views.html.forgot_password;
import views.html.login;
import views.html.register;
import views.html.email.signup_betauser;


public class UserController extends Controller {
	
	private static final String COMPONENT_NAME = "User Controller";
	
	
	/**
	 * Set the Remember Me cookie for user. Currently, set to expire after 6 months
	 */
	private static Boolean rememberUser() {
		DynamicForm dynaForm = DynamicForm.form().bindFromRequest();
		Boolean rememberMe = new Boolean(dynaForm.get(UtilityHelper.REMEMBER_ME));

		if(rememberMe) {
			response().setCookie(UtilityHelper.REMEMBER_ME, SecurityHelper.encrypt(User.getCurrentUser().getUserId()), 60*60*24*180);
			UtilityHelper.logMessage(COMPONENT_NAME, "rememberUser()", "Remember Me cookie set for user [" + User.getCurrentUser().getUserId() + "]");
		}
		return rememberMe;
	}
	
	
	private static void forgetUser() {
		response().discardCookie(UtilityHelper.REMEMBER_ME);
	}
	
	
	/**
	 * Allow the user to login to the application
	 */
	public static Result login() {
		Form<User> userForm = Form.form(User.class).bindFromRequest();
		if(userForm.hasErrors()) {
			userForm.reject("login_error", "Please enter valid email id and password");
			return badRequest(login.render(userForm));
		}

		User user = userForm.get();
		if(User.login(user.getEmail(), user.getPassword())) {
			rememberUser();
			return redirect(routes.Application.index());
		} else {
			userForm.reject("login_error", "Please enter valid email id and password");
			return badRequest(login.render(userForm));
		}

    }
	
	
	public static Result logout() {
		User.logout();
		forgetUser();
		return redirect(routes.Application.index());
	}
	
	
	/**
	 * Allow users to sign up to the application - for Beta stage only,
	 * as currently users cannot register to the application directly
	 */
	public static Result signup() {
		try {
			DynamicForm form = DynamicForm.form().bindFromRequest();
			String email = form.get("email");
			BetaUser user = new BetaUser(email, UtilityHelper.getCurrentTime(), false, false);
			user.save();
			UtilityHelper.logMessage(COMPONENT_NAME, "signup()", "Beta user [" + email + "] saved");
			UtilityHelper.sendMail(email, "Thanks for signing up with Clocen!", signup_betauser.render().toString());
			
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "signup()", "Error while registering user", exception);
		}
		return ok(betauser_thanks.render());
	}
	
	
	/**
	 * Register the user. Only Beta users can currently register, 
	 * once they have been sent the registration email
	 */
	public static Result register() {
		try {
			Form<User> userForm = Form.form(User.class).bindFromRequest();
			User user = userForm.get();
			String email = user.getEmail();
			String password = user.getPassword();
			
			BetaUser betaUser = BetaUser.getBetaUser(email);
			
			if(betaUser!=null && !UtilityHelper.isEmptyString(email) 
					&& !UtilityHelper.isEmptyString(password) ) {
				user.setUserId(UtilityHelper.getUniqueId());
				user.setPassword(SecurityHelper.generateHash(user.getUserId(), password));
				user.setCreateTimestamp(UtilityHelper.getCurrentTime());
				
				user.save();
				User.login(email, password);
				rememberUser();
				
				betaUser.setRegistered(true);
				betaUser.save();
				
				UtilityHelper.logMessage(COMPONENT_NAME, "register()", "Beta user [" + user.getUserId() + "] registered");
			} else {
				userForm.reject("register_error", "Please enter valid email and password");
				return badRequest(register.render(userForm));
			}
			
			return redirect(routes.Application.index());
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "register()", "Error while registering user", exception);
    		return internalServerError(error_page.render());
		}
	}
	

	public static Result updateProfile(String userId) {
		try {
			Form<User> userForm = Form.form(User.class).bindFromRequest();
			User user = userForm.get();
			User currentUser = User.getUser(userId);
			
			user.setUserId(currentUser.getUserId());
			user.setCreateTimestamp(currentUser.getCreateTimestamp());

			// Password is only updated if the user has enetered any value for it
			if(!UtilityHelper.isEmptyString(user.getPassword()))
				user.setPassword(SecurityHelper.generateHash(user.getUserId(), user.getPassword()));
			else
				user.setPassword(currentUser.getPassword());
			
			user.save();
			
			return redirect(routes.Application.getUserProfilePage());
		} catch (Exception exception) {
    		UtilityHelper.logError(COMPONENT_NAME, "updateProfile()", "Error while updating user profile", exception);
    		return internalServerError(error_page.render());
		}
	}
	
	
	/**
	 * Process user's request for forgot password, by sending 
	 * an email with a link to allow the user to reset the password 
	 * on its own
	 */
	public static Result forgotPassword() {
		Form<User> form = Form.form(User.class).bindFromRequest();
		User formUser = form.get();

		if(UtilityHelper.isEmptyString(formUser.getEmail())) {
			form.reject("reset_error", "Please enter a valid email id.");
			return badRequest(forgot_password.render(form, User.PASSWORD_FORGOT_VIEW));
		} else {
			User user = User.getUserByEmail(formUser.getEmail());
			if(user == null) {
				form.reject("reset_error", "This email id is not registered with us. Please enter a valid email id.");
				return badRequest(forgot_password.render(form, User.PASSWORD_FORGOT_VIEW));
			} else {
				// Prepare the URL for user to reset the password. Add the user id and 
				// current timestamp to the URL, in order to be able to identify it and 
				// identify whether the password reset request has expired
				StringBuffer url = new StringBuffer(controllers.routes.Application.getForgotAndResetPasswordPage("reset").absoluteURL(Controller.request()));
				url.append("?key1=" + SecurityHelper.encrypt(user.getUserId()));
				url.append("&key2=" + SecurityHelper.encrypt(String.valueOf(System.currentTimeMillis())));
				
				UtilityHelper.sendMail(user.getEmail(), "Clocen Password Reset Request", views.html.email.forgot_password.render(url.toString()).toString());
				
				return ok(forgot_password.render(form, User.PASSWORD_FORGOT_PROCESSED));
			}
		}
	}
	
	
	/**
	 * Allow the user to reset the password
	 */
	public static Result passwordReset() {
		Form<User> form = Form.form(User.class).bindFromRequest();
		User formUser = form.get();
		
		if(formUser==null || UtilityHelper.isEmptyString(formUser.getPassword())) {
			form.reject("reset_error", "Invalid entry. Please enter your new password.");
			return badRequest(forgot_password.render(form, User.PASSWORD_RESET_VIEW));
		}
		
		// the link to show the form already puts the user
		// in the session, so that we only have to change 
		// the password of the current user
		User user = User.getCurrentUser();
		user.setPassword(SecurityHelper.generateHash(user.getUserId(), formUser.getPassword()));
		user.save();
		
		return redirect(routes.Application.index());
	}
	
}
