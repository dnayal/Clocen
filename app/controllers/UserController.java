package controllers;

import helpers.UtilityHelper;

import java.util.List;

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
		DynamicForm form = DynamicForm.form().bindFromRequest();
		String email = form.get("email");

		if(!UtilityHelper.isEmptyString(email)) {
			User user = User.getUserByEmail(email);
			user.setAsCurrentUser();
		}

		return redirect(routes.UserController.home());
    }
	
	
	public static Result logout() {
		User.logout();
		return redirect(routes.Application.index());
	}
	
	
	public static Result register() {
		return TODO;
	}
	

	public static Result home() {
		User user = User.getCurrentUser();
		
		if(user==null)
			return redirect(routes.Application.index());
		
		List<ServiceNodeInfo> list = user.getAllNodes();
		return ok(user_home.render(list));
	}
	
	
	public static Result getProfile() {
		User user = User.getCurrentUser();
		
		if(user==null)
			return redirect(routes.Application.index());

		Form<User> userForm = Form.form(User.class).fill(user);
		return ok(user_profile.render(user, userForm));
	}
	
	
	public static Result updateProfile(String userId) {
		Form<User> userForm = Form.form(User.class).bindFromRequest();
		User newUser = userForm.get();
		User oldUser = User.getUser(userId);
		newUser.setUserId(oldUser.getUserId());
		newUser.setCreateTimestamp(oldUser.getCreateTimestamp());
		newUser.update();
		return redirect(routes.UserController.getProfile());
	}
	

}
