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
	
	private static final String COMPONENT_NAME = "Process Controller";

	/**
	 * Private method to return Process details as a json.
	 * Converting a process directly to a JSON, would lead 
	 * to getting the Process Data treated as a string
	 */
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

	
	/**
	 * Returns a list of all processes created by the given user
	 */
	public static Result getAllProcessesForUser(String userId) {
		List<Process> list = Process.getProcessesForUser(userId);
		List<JsonNode> jsonList = new ArrayList<JsonNode>();
		for(Process process : list) {
			jsonList.add(getProcessJson(process));
		}

		UtilityHelper.logMessage(COMPONENT_NAME, "getAllProcessesForUser()", jsonList.size() + " processes returned for User [" + userId + "]");
		return ok(Json.toJson(jsonList));
	}
	
	
	/**
	 * Save the process for the given user. Expects a form as a POST
	 */
	public static Result saveProcess(String userId) {
				
		Form<Process> form = Form.form(Process.class).bindFromRequest();
		Process process = form.get();

		if(userId==null || UtilityHelper.isEmptyString(process.getProcessData())) {
			UtilityHelper.logMessage(COMPONENT_NAME, "saveProcess()", "Invalid user or process data received");
			return badRequest();
		}

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
			
			UtilityHelper.logMessage(COMPONENT_NAME, "saveProcess()", "Process [" + process.getProcessId() + "] saved for User [" + userId + "]");
			
			return ok(getProcessJson(process));
		} else {
			UtilityHelper.logMessage(COMPONENT_NAME, "saveProcess()", "Invalid process data for User [" + userId + "]");

			return badRequest();
		}

	}
	
	
	public static Result getProcess(String processId) {
		Process process = Process.getProcess(processId);
		return ok(getProcessJson(process));
	}
	
	
	/**
	 * Pause for the process for the given user. Only the owner will be able to pause the process
	 */
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
	
	
	/**
	 * Delete the process. Only the owner will be able to delete the process
	 */
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
