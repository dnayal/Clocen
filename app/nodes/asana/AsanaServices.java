package nodes.asana;

import helpers.FileHelper;
import helpers.UtilityHelper;
import helpers.WSHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import models.IdName;
import models.ServiceAccessToken;
import nodes.Node;

import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;

import play.Play;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

/**
 * The main engine class for Asana, powering all its services
 */
@SuppressWarnings("unchecked")
public class AsanaServices implements AsanaConstants {
	
	private static final String COMPONENT_NAME = "Asana Services";

	private ServiceAccessToken sat;
	
	// if you delete this method, the Node initiation can fail
	// as the ServiceNodeHelper looks for all classes in the  
	// node.<<node id>> package and initializes using the  
	// default constructor
	public AsanaServices() {
		sat = null;
	}
	
	
	public AsanaServices(ServiceAccessToken sat) {
		this.sat = sat;
	}


	/**
	 * This method gets the attachments for a particular task.
	 * After getting the response from Asana API, it loops all 
	 * attachments and stores the FileHelper object, which 
	 * in turn stores the name and download_url 
	 */
	private ArrayList<Map<String, Object>> getAttachments(String taskId) {
		// make the call to web service to get all the attachments for the task 
		Promise<Response> response = WS.url(API_BASE_URL+"/tasks/"+taskId+"/attachments")
				.setQueryParameter("opt_fields", "name,download_url")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
	
		JsonNode json = null;
		Response result = response.get();
		
		// check for errors, and if found, process those
		Asana asana = new Asana();
		if(asana.serviceResponseHasError(SERVICE_INTERNAL_GET_ATTACHMENTS, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson().path("data");
		
		// initialize the attachments object
		ArrayList<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
		
		// parse the webservice response, and go through each task
		for(JsonNode taskJson : json) {
			String name = taskJson.get("name").asText();
			String downloadURL = taskJson.get("download_url").asText();
	
			// initialize the FileHelper object and store 
			// the file name and document URL in it
			FileHelper fileHelper = new FileHelper();
			fileHelper.setFileSource(name, downloadURL);
	
			// we are using a Map object here only to comply 
			// with the ObjectMapper created by parsing a json 
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(Node.ATTR_TYPE_FILE, fileHelper);
			
			// add the file to the attachment list
			attachments.add(map);
		}
		
		return attachments;
		
	}
	

	// TODO Need to capture cases when more than one tasks are newly created
	/**
	 * Method to execute for Asana's "New Task Created" event. It first checks 
	 * the input (workspaces id) provided by the user, then uses that value to make 
	 * the call to Asana. Once it gets the result, output values are stored in 
	 * the map object 
	 */
	public Map<String, Object> getNewTaskCreated(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>)data.get("input");
		String workspaceId = null;
		
		// get the workspace id from the input values
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			if(type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE) && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			}
		}
		
		if(workspaceId==null)
			return null;
		
		// make the call to web service to get all the tasks
		// Returns only the tasks assigned to the current user
		Promise<Response> response = WS.url(API_BASE_URL+"/workspaces/"+workspaceId+"/tasks")
				.setQueryParameter("opt_fields", "created_at,name,notes")
				.setQueryParameter("assignee", "me")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();

		JsonNode json = null;
		Response result = response.get();
		
		// check for errors, and if found, process those
		Asana asana = new Asana();
		if(asana.serviceResponseHasError(SERVICE_TRIGGER_NEW_TASK_CREATED, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson().path("data");
		
		// parse the webservice response, and go through each task
		for(JsonNode taskJson : json) {
			// get the create date of the task from webservice response
			DateTime taskCreateDate = UtilityHelper.convertToUTCTime(taskJson.get("created_at").asText(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			// get the current time minus the polling interval - to get the last time we polled (approximately)
			DateTime nowMinusPollerInterval = UtilityHelper.getCurrentTimeMinusPollerInterval();
						
			// check whether a task was created since the last time we polled
			// if a new task was created, populate the output variables with 
			// the task details
			if(taskCreateDate.isAfter(nowMinusPollerInterval)) {
				String taskId = taskJson.get("id").asText();
				String taskName = taskJson.get("name").asText();
				String taskDescription = taskJson.get("notes").asText();
				ArrayList<Map<String, Object>> attachments = getAttachments(taskId);
				
				ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>)data.get("output");
				for(Map<String, Object> output : outputs) {
					String id = (String) output.get("id");
					
					if(id.equalsIgnoreCase("taskid")) {
						output.put("value", taskId);
					} else if(id.equalsIgnoreCase("taskname")) {
						output.put("value", taskName);
					} else if(id.equalsIgnoreCase("taskdescription")) {
						output.put("value", taskDescription);
					} else if(id.equalsIgnoreCase("attachment")) {
						output.put("value", attachments);
					} 
				}
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getNewTaskCreated()", "New Task Event processed for Asana for user [" + sat.getKey().getUserId() + "]");
				
				// if you did get a newly created task, and have got its details, 
				// return the data for the first matching task you get
				return data;
			}
		}

		// if the method reaches here, it did not find any new tasks created
		return null;
	}
	
	
	/**
	 * Checks whether a new project is created. If so, it will 
	 * extract its properties to pass to the next activity in 
	 * the process  
	 */
	public Map<String, Object> getNewProjectCreated(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>) data.get("input");
		String workspaceId = null;
		
		// get the workspace id from the input values
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			if(type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE) && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			}
		}
		
		if(workspaceId==null)
			return null;
		
		// make the call to web service to get all the projects in workspace 
		Promise<Response> response = WS.url(API_BASE_URL+"/workspaces/"+workspaceId+"/projects")
				.setQueryParameter("opt_fields", "created_at,name,notes")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();

		JsonNode json = null;
		Response result = response.get();
		
		// check for errors, and if found, process those
		Asana asana = new Asana();
		if(asana.serviceResponseHasError(SERVICE_TRIGGER_NEW_PROJECT_CREATED, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson().path("data");
		
		// parse the webservice response, and go through each task
		for(JsonNode projectJSON : json) {
			// get the create date of the task from webservice response
			DateTime projectCreateDate = UtilityHelper.convertToUTCTime(projectJSON.get("created_at").asText(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			// get the current time minus the polling interval - to get the last time we polled (approximately)
			DateTime nowMinusPollerInterval = UtilityHelper.getCurrentTimeMinusPollerInterval();
			
			// check whether a project was created since the last time we polled
			// if a new project was created, populate the output variables with 
			// the project details
			if(projectCreateDate.isAfter(nowMinusPollerInterval)) {
				String projectId = projectJSON.get("id").asText();
				String projectName = projectJSON.get("name").asText();
				String projectDescription = projectJSON.get("notes").asText();
				
				
				ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>)data.get("output");
				for(Map<String, Object> output : outputs) {
					String id = (String) output.get("id");
					
					if(id.equalsIgnoreCase("projectid")) {
						output.put("value", projectId);
					} else if(id.equalsIgnoreCase("projectname")) {
						output.put("value", projectName);
					} else if(id.equalsIgnoreCase("projectdescription")) {
						output.put("value", projectDescription);
					} 
				}
				
				UtilityHelper.logMessage(COMPONENT_NAME, "getNewProjectCreated()", "New Project Event processed for Asana for user [" + sat.getKey().getUserId() + "]");
				
				// if you did get a newly created task, and have got its details, 
				// return the data for the first matching task you get
				return data;
			}
		}
		
		return null;
	}
	

	/**
	 * Create new task with the assignee as the user who created the process
	 */
	public Map<String, Object> createTask(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>)data.get("input");
		String workspaceId = null, taskName = null, taskDescription = null;

		// object to store attachments for a task
		ArrayList<Map<String, Object>> attachments = null;

		// loop through all the input variables for this service
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			
			// for a variable of type service, get the id paramter from the value
			if(type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE) && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			} else if(id.equalsIgnoreCase("taskname")) {
				taskName = (String) input.get("value"); 
			} else if(id.equalsIgnoreCase("taskdescription")) {
				taskDescription = (String) input.get("value"); 
			} else if(id.equalsIgnoreCase("attachment")) {
				// get the attachments
				attachments = (ArrayList<Map<String, Object>>) input.get("value"); 
			}
		}
		
		// we need to have atleast the workspace id to be 
		// able to create a new task
		if(workspaceId==null)
			return null;
		
		// make the call to web service to create new task
		// with the current user as the assignee
		Promise<Response> response = WS.url(API_BASE_URL+"/workspaces/"+workspaceId+"/tasks")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken())
				.setHeader("Content-Type", Play.application().configuration().getString("application.services.POST.contentType"))
				.post("name="+UtilityHelper.getString(taskName)+"&notes="+UtilityHelper.getString(taskDescription)+"&assignee=me");

		JsonNode json = null;
		Response result = response.get();
		
		// check for errors, and if found, process those
		Asana asana = new Asana();
		if(asana.serviceResponseHasError(SERVICE_ACTION_CREATE_TASK, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson().path("data");

		// if the task is successfully created, Asana API returns a task id
		String taskId = json.get("id").asText();
		
		// if there are any attachments, add those to the task
		if(attachments!=null && attachments.size()>0) {
			// loop through all the attachments
			for(Map<String, Object> attachment : attachments) {
				
				// Prepare the objects to be sent to the POST request
				// get the filehelper object and then get the file
				FileHelper fileHelper = (FileHelper) attachment.get(Node.ATTR_TYPE_FILE);
				File file = fileHelper.getFileFromSource();
				// body part to be sent to the REST POST service
				FileBody fileBody = new FileBody(file);
				// parameters expected by the REST POST service
				Map<String, AbstractContentBody> bodyPart = new HashMap<String, AbstractContentBody>();
				bodyPart.put("file", fileBody);
				
				try {
					
					// Call the REST POST request with the required parameters
					Map<String, Object> map = WSHelper.postRequestWithFileUpload(API_BASE_URL+"/tasks/"+taskId+"/attachments", sat, bodyPart);

					Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
					Long attachmentId = (Long) dataMap.get("id");
					if(attachmentId != null)
						fileHelper.deleteFile();

				} catch (Exception exception) {
					fileHelper.deleteFile();
					UtilityHelper.logError(COMPONENT_NAME, "createTask()", exception.getMessage(), exception);
				}

			}
		}
		
		// add the output values back to the map 
		ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>)data.get("output");
		for(Map<String, Object> output : outputs) {
			String id = (String) output.get("id");
			if(id.equalsIgnoreCase("taskid")) {
				output.put("value", taskId);
			} else if(id.equalsIgnoreCase("taskname")) {
				output.put("value", taskName);
			} else if(id.equalsIgnoreCase("taskdescription")) {
				output.put("value", taskDescription);
			} else if(id.equalsIgnoreCase("attachment")) {
				output.put("value", attachments);
			} 
		}
		
		UtilityHelper.logMessage(COMPONENT_NAME, "createTask()", "New Task Created in Asana for user [" + sat.getKey().getUserId() + "]");
		
		return data;
	}
	
	
	/**
	 * Returns all Asana workspaces for the given user
	 */
	public JsonNode getWorkspaces() {
		ArrayList<IdName> list = new ArrayList<IdName>();

		String endPoint = API_BASE_URL + "/workspaces";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();

		JsonNode json = null;
		Response result = response.get();
		
		// check for errors in response
		Asana asana = new Asana();
		if(asana.serviceResponseHasError(SERVICE_INFO_GET_WORKSPACES, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson().path("data");

		Iterator<JsonNode> iterator = json.getElements();
		// map all the workspaces to the ID-Name pair
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			list.add(new IdName(node.get("id").asText(), node.get("name").asText()));
		}
		
		// and then convert that id-name pair to JSON
		// so that it an be transferred to the client app
		json = null;
		ObjectMapper mapper = new ObjectMapper();
		json = mapper.valueToTree(list);

		return json;
	}


}
