package controllers;

import helpers.SecurityHelper;
import helpers.UtilityHelper;

import java.util.List;

import models.BetaUser;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import process.ProcessManager;
import views.html.admin.*;
import views.html.email.*;


public class AdminController extends Controller {

	private static final String COMPONENT_NAME = "Admin Controller";	
	

	/**
	 * Shows all beta users who have registered for the application
	 */
	public static Result showBetaUsers() {
		if(!User.isCurrentUserAdmin()) {
			return redirect(routes.Application.index());
		} else {
			List<BetaUser> list = BetaUser.getAllBetaUsers();
			return ok(show_betausers.render(list));
		}
	}
	
	

	/**
	 * Send the email invitation to the given beta user
	 */
	public static Result inviteBetaUser(String email) {
		BetaUser user = BetaUser.getBetaUser(email);
		String passwordResetURL = routes.Application.getRegistrationForm().absoluteURL(
				request()).concat("?key="+SecurityHelper.encrypt(email));
		
		UtilityHelper.sendMail(email, "Welcome to Clocen!", invite_betauser.render(passwordResetURL).toString());
		
		user.setInviteEmailSent(true);
		
		UtilityHelper.logMessage(COMPONENT_NAME, "inviteBetaUser()", "Registration invite sent to - " + email);
		
		user.save();
		return redirect(routes.AdminController.showBetaUsers());
	}
	
	
	/**
	 * Main admin landing page
	 */
	public static Result viewMainAdminPage() {
		if(!User.isCurrentUserAdmin()) {
			return redirect(routes.Application.index());
		} else {
			return ok(main.render());
		}
	}


	/**
	 * Dummy method to execute processes
	 */
	public static Result executeProcess() {
		ProcessManager manager = new ProcessManager();
		manager.runProcesses();
		return redirect(routes.AdminController.viewMainAdminPage());
	}
	
	
}
