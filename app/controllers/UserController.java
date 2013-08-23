package controllers;

import org.codehaus.jackson.JsonNode;

import handlers.UserHandler;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class UserController extends Controller {

	public static Result getUserByEmail() {
		JsonNode json = request().body().asJson();
		Logger.info("*******");
		Logger.info(json.toString());
		Logger.info("*******");
		String email = json.get("email").getTextValue();
		Logger.info("UserController->getUserByEmail");
		Logger.info("Email : " + email);
		return ok(Json.toJson(UserHandler.getUserByEmail(email)));
	}
}
