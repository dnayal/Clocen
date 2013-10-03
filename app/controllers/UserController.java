package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;

import java.util.Calendar;
import java.util.List;

import models.BetaUser;
import models.ServiceNodeInfo;
import models.User;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.*;

public class UserController extends Controller {
	
	
	private static final String COMPONENT_NAME = "User Controller";	

	
	public static Result login() {
		Form<User> userForm = Form.form(User.class).bindFromRequest();
		User user = userForm.get();
		
		if(User.login(user.getEmail(), user.getPassword())) {
			return redirect(routes.UserController.home());
		} else {
			userForm.reject("login_error", "Incorrect email id or password. Please enter correct information.");
			return badRequest(index.render(userForm));
		}

    }
	
	
	public static Result logout() {
		User.logout();
		return redirect(routes.Application.index());
	}
	
	
	public static Result signup() {
		try {
			DynamicForm form = DynamicForm.form().bindFromRequest();
			String email = form.get("email");
			BetaUser user = new BetaUser(email, Calendar.getInstance().getTime(), false, false);
			user.save();
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
				
				betaUser.setRegistered(true);
				betaUser.save();
			} else {
				userForm.reject("register_error", "Please enter valid email and password");
				return badRequest(register.render(userForm));
			}
			
			return redirect(routes.UserController.home());
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "register()", "Error while registering user", exception);
    		return internalServerError(error_page.render());
		}
	}
	

	public static Result home() {
		User user = User.getCurrentUser();
		
		if(user==null)
			return redirect(routes.Application.index());
		
		List<ServiceNodeInfo> list = user.getAllNodes();
		return ok(user_home.render(list));
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
		
		return redirect(routes.UserController.home());
	}
	
}
