package controllers;

import helpers.UserHelper;
import helpers.UtilityHelper;

import java.util.List;

import models.ServiceNodeInfo;
import models.User;

import org.codehaus.jackson.JsonNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class UserController extends Controller {
	
	private static final String COMPONENT_NAME = "User Controller";	

	public static Result login() {
		JsonNode json = request().body().asJson();
		String email = json.get("email").getTextValue();
		List<ServiceNodeInfo> nodeList = null;
		
		if(!UtilityHelper.isEmptyString(email)) {
			User user = User.getUserByEmail(email);
			UserHelper.setCurrentUser(user.getUserId());

			nodeList = user.getAllNodes();
		}

		return ok(Json.toJson(nodeList));
    }
  
    
}
