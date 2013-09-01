package helpers;

import models.User;
import play.mvc.Controller;

public class UserHelper {
	
	
	public static User getCurrentUser() {
		String userId = Controller.ctx().session().get("user_id");
		if(UtilityHelper.isEmptyString(userId))
			return null;
		else 
			return User.find.byId(userId);
	}
	
	
	public static void setCurrentUser(String userId) {
		Controller.ctx().session().put("user_id", userId);
	}

}
