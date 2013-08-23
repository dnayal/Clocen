package handlers;

import models.User;

public class UserHandler {

	public static User getUserByEmail(String email) {
		return User.find.where().eq("email", email).findUnique();
	}
}
