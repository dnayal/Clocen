package nodes.asana.services;

import helpers.UtilityHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.Response;
import models.ServiceAccessToken;

public class AsanaServices {
	
	private static final String COMPONENT_NAME = "AsanaServices";
	private static final String BASE_URL = "https://app.asana.com/api/1.0";

	private ServiceAccessToken sat;
	
	public AsanaServices(ServiceAccessToken sat) {
		this.sat = sat;
	}
	
	// TODO Need to add attachments as one of the return parameters
	// TODO Need to capture cases when more than one tasks are newly created
	/**
	 * Method to execute for Asana's "New Task Created" event. It first checks 
	 * the input (workspaces id) provided by the user, then uses that value to make 
	 * the call to Asana. Once it gets the result, output values are stored in 
	 * the map object 
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getNewTaskCreated(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>)data.get("input");
		String workspaceId = null;
		
		// get the workspace id from the input values
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			if(type.equalsIgnoreCase("service") && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			}
		}
		
		if(workspaceId==null)
			return null;
		
		// make the call to web service to get all the tasks 
		Promise<Response> response = WS.url(BASE_URL+"/workspaces/"+workspaceId+"/tasks")
				.setQueryParameter("opt_fields", "created_at,name,notes")
				.setQueryParameter("assignee", "me")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();

		// TODO - check for errors
		JsonNode json = response.get().asJson().path("data");
		
		// parse the webservice response, and go through each task
		for(JsonNode taskJson : json) {
			// get the create date of the task from webservice response
			String createDate = taskJson.get("created_at").asText();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date = null;
			try {
				date = dateFormat.parse(createDate);
			} catch (ParseException exception) {
				UtilityHelper.logError(COMPONENT_NAME, "getNewTaskCreated()", exception.getMessage(), exception);
				return null;
			}

			// get the current time minus the polling interval - to get the last time we polled (approximately)
			Calendar calendar = Calendar.getInstance();
			Integer pollerInterval = Play.application().configuration().getInt("process.poller.interval");
			calendar.add(Calendar.MINUTE, -pollerInterval);
			Date intervalDate = calendar.getTime();
			
			// check whether a task was created since the last time we polled
			// if a new task was created, populate the output variables with 
			// the task details
			if(date.after(intervalDate)) {
				String taskId = taskJson.get("id").asText();
				String taskName = taskJson.get("name").asText();
				String taskDescription = taskJson.get("notes").asText();
				
				ArrayList<Map<String, String>> outputs = (ArrayList<Map<String, String>>)data.get("output");
				for(Map<String, String> output : outputs) {
					if(output.get("id").equalsIgnoreCase("taskid")) {
						output.put("value", taskId);
					} else if(output.get("id").equalsIgnoreCase("taskname")) {
						output.put("value", taskName);
					} else if(output.get("id").equalsIgnoreCase("taskdescription")) {
						output.put("value", taskDescription);
					} // TODO - need to add output for attachments as well
				}
				
				// if you did get a newly created task, and have got its details, 
				// return the data for the first matching task you get
				return data;
			}
		}

		// if the method reaches here, it did not find any new tasks created
		return null;
	}
	

	/**
	 * Create new task with the assignee as the user who created the process
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> createTask(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>)data.get("input");
		String workspaceId = null, taskName = null, taskDescription = null;

		// get the workspace id from the input values
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			if(type.equalsIgnoreCase("service") && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			} else if(id.equalsIgnoreCase("taskname")) {
				taskName = (String) input.get("value"); 
			} else if(id.equalsIgnoreCase("taskdescription")) {
				taskDescription = (String) input.get("value"); 
			}
		}
		
		// we need to have atleast the workspace id to be 
		// able to create a new task
		if(workspaceId==null)
			return null;
		
		// make the call to web service to create new task
		// with the current user as the assignee
		Promise<Response> response = WS.url(BASE_URL+"/workspaces/"+workspaceId+"/tasks")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken())
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.post("name="+UtilityHelper.getString(taskName)+"&notes="+UtilityHelper.getString(taskDescription)+"&assignee=me");
		JsonNode json = response.get().asJson().path("data");

		String taskId = json.get("id").asText();
		
		// add the output values back to the map 
		ArrayList<Map<String, String>> outputs = (ArrayList<Map<String, String>>)data.get("output");
		for(Map<String, String> output : outputs) {
			if(output.get("id").equalsIgnoreCase("taskid")) {
				output.put("value", taskId);
			} else if(output.get("id").equalsIgnoreCase("taskname")) {
				output.put("value", taskName);
			} else if(output.get("id").equalsIgnoreCase("taskdescription")) {
				output.put("value", taskDescription);
			} // TODO - need to add output for attachments as well
		}
		
		return data;
	}
}
