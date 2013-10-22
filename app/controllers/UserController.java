package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;

import java.util.Calendar;

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
	
	
	private static Boolean rememberUser() {
		DynamicForm dynaForm = DynamicForm.form().bindFromRequest();
		Boolean rememberMe = new Boolean(dynaForm.get(UtilityHelper.REMEMBER_ME));
		if(rememberMe)
			response().setCookie(UtilityHelper.REMEMBER_ME, SecurityHelper.encrypt(User.getCurrentUser().getUserId()), 60*60*24*180);
		return rememberMe;
	}
	
	
	private static void forgetUser() {
		response().discardCookie(UtilityHelper.REMEMBER_ME);
	}
	
	
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
	
	
	public static Result signup() {
		try {
			DynamicForm form = DynamicForm.form().bindFromRequest();
			String email = form.get("email");
			BetaUser user = new BetaUser(email, Calendar.getInstance().getTime(), false, false);
			user.save();
			UtilityHelper.sendMail(email, "Thanks for signing up with Clocen!", signup_betauser.render().toString());
			
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "signup()", "Error while registering user", exception);
		}
		return ok(betauser_thanks.render());
	}
	
	
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
				user.setCreateTimestamp(Calendar.getInstance().getTime());
				
				user.save();
				User.login(email, password);
				rememberUser();
				
				betaUser.setRegistered(true);
				betaUser.save();
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
				StringBuffer url = new StringBuffer(controllers.routes.Application.getForgotAndResetPasswordPage("reset").absoluteURL(Controller.request()));
				url.append("?key1=" + SecurityHelper.encrypt(user.getUserId()));
				url.append("&key2=" + SecurityHelper.encrypt(String.valueOf(System.currentTimeMillis())));
				
				UtilityHelper.sendMail(user.getEmail(), "Clocen Password Reset Request", views.html.email.forgot_password.render(url.toString()).toString());
				
				return ok(forgot_password.render(form, User.PASSWORD_FORGOT_PROCESSED));
			}
		}
	}
	
	
	public static Result passwordReset() {
		Form<User> form = Form.form(User.class).bindFromRequest();
		User formUser = form.get();
		
		if(formUser==null || UtilityHelper.isEmptyString(formUser.getPassword())) {
			form.reject("reset_error", "Invalid entry. Please enter your new password.");
			return badRequest(forgot_password.render(form, User.PASSWORD_RESET_VIEW));
		}
		
		User user = User.getCurrentUser();
		user.setPassword(SecurityHelper.generateHash(user.getUserId(), formUser.getPassword()));
		user.save();
		
		return redirect(routes.Application.index());
	}
	
}
