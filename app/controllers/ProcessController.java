package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import nodes.Node;

import org.codehaus.jackson.JsonNode;

import models.Process;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.error_page;

public class ProcessController extends Controller {

	public static Result getAllProcessesForUser() {
		User user = User.getCurrentUser();
		List<Process> list = Process.getProcessesForUser(user.getUserId());
		List<JsonNode> json = new ArrayList<JsonNode>();
		for(Process process : list) 
			json.add(Json.parse(process.getProcessData()));

		return ok(Json.toJson(json));
	}
	
	
	public static Result saveProcess() {
		User user = User.getCurrentUser();		
		Form<Process> form = Form.form(Process.class).bindFromRequest();
		Process process = form.get();

		if(user==null || UtilityHelper.isEmptyString(process.getProcessData())) {
    		return internalServerError(error_page.render());
		}

		JsonNode json = Json.parse(process.getProcessData());
		Iterator<JsonNode> iterator = json.getElements();
		if(iterator.hasNext()) {
			String triggerNode = iterator.next().get("node").asText();
			Node node = ServiceNodeHelper.getNode(triggerNode);
			
			process.setTriggerType(node.getTriggerType());
			
			process.setProcessId(UtilityHelper.getUniqueId());
			process.setUserId(user.getUserId());
			process.setCreateTimestamp(Calendar.getInstance().getTime());
			process.save();
			return redirect(routes.UserController.home());
		} else {
    		return internalServerError(error_page.render());
		}
		
	}
	
	
	public static Result getProcess(String processId) {
		Process process = Process.getProcess(processId);
		return ok(Json.toJson(process));
	}
}
