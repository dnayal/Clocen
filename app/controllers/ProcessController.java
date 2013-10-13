package controllers;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import nodes.Node;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import models.Process;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class ProcessController extends Controller {
	
	private static ObjectNode getProcessJson(Process process) {
		ObjectNode json = Json.newObject();
		if (process!=null) {
			json.put("processId", process.getProcessId());
			json.put("createTimestamp", process.getCreateTimestamp().toString());
			json.put("paused", process.isPaused());
			json.put("data", Json.parse(process.getProcessData()));
		}
		return json;
	}

	
	public static Result getAllProcessesForUser(String userId) {
		List<Process> list = Process.getProcessesForUser(userId);
		List<JsonNode> jsonList = new ArrayList<JsonNode>();
		for(Process process : list)
			jsonList.add(getProcessJson(process));

		return ok(Json.toJson(jsonList));
	}
	
	
	public static Result saveProcess(String userId) {
				
		Form<Process> form = Form.form(Process.class).bindFromRequest();
		Process process = form.get();

		if(userId==null || UtilityHelper.isEmptyString(process.getProcessData()))
    		return badRequest();

		JsonNode json = Json.parse(process.getProcessData());
		Iterator<JsonNode> iterator = json.getElements();
		if(iterator.hasNext()) {
			String triggerNode = iterator.next().get("node").asText();
			Node node = ServiceNodeHelper.getNode(triggerNode);
			
			process.setTriggerType(node.getTriggerType());
			
			process.setProcessId(UtilityHelper.getUniqueId());
			process.setUserId(userId);
			process.setCreateTimestamp(Calendar.getInstance().getTime());
			process.save();
			return ok(getProcessJson(process));
		} else {
    		return badRequest();
		}

	}
	
	
	public static Result getProcess(String processId) {
		Process process = Process.getProcess(processId);
		return ok(getProcessJson(process));
	}
	
	
	public static Result pauseProcess(String processId, String callerUserId) {
		Process process = Process.getProcess(processId);
		if(process.isUserOwner(callerUserId)) {
			process.setPaused(!process.isPaused());
			process.save();
			return ok(getProcessJson(process));
		} else {
			return badRequest();
		}
	}
	
	
	public static Result deleteProcess(String processId, String callerUserId) {
		Process process = Process.getProcess(processId);
		if(process.isUserOwner(callerUserId)) {
			process.delete();
			return ok();
		} else {
			return badRequest();
		}
	}
}
