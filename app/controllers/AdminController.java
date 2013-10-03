package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;

import java.util.List;

import models.BetaUser;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.admin.*;
import views.html.email.*;


public class AdminController extends Controller {

	//private static final String COMPONENT_NAME = "Admin Controller";	
	

	public static Result showBetaUsers() {
		if(!User.isCurrentUserAdmin()) {
			return redirect(routes.Application.index());
		} else {
			List<BetaUser> list = BetaUser.getAllBetaUsers();
			return ok(show_betausers.render(list));
		}
	}
	
	
	public static Result inviteBetaUser(String email) {
		BetaUser user = BetaUser.getBetaUser(email);
		String passwordResetURL = routes.Application.getRegistrationForm().absoluteURL(
				request()).concat("?key="+SecurityHelper.encrypt(email));
		UtilityHelper.sendMail(email, "Welcome to Clocen!", invite_betauser.render(passwordResetURL).toString());
		user.setInviteEmailSent(true);
		user.save();
		return redirect(routes.AdminController.showBetaUsers());
	}

}
