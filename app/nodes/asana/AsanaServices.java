package nodes.asana;

import helpers.FileHelper;
import helpers.UtilityHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import models.IdName;
import models.ServiceAccessToken;
import nodes.Node;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.Play;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

/**
 * The main engine class for Asana, powering all its services
 */
public class AsanaServices implements AsanaConstants {
	
	private static final String COMPONENT_NAME = "AsanaServices";

	private ServiceAccessToken sat;
	
	// if you delete this method, the Node initiation can fail
	// as the ServiceNodeHelper looks for all classes in the  
	// node.<<node id>> package
	public AsanaServices() {
		sat = null;
	}
	
	public AsanaServices(ServiceAccessToken sat) {
		this.sat = sat;
	}
	
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
			if(type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE) && id.equalsIgnoreCase("workspace")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				workspaceId = value.get("id");
			}
		}
		
		if(workspaceId==null)
			return null;
		
		// make the call to web service to get all the tasks 
		Promise<Response> response = WS.url(API_BASE_URL+"/workspaces/"+workspaceId+"/tasks")
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
	 * Create new task with the assignee as the user who created the process
	 */
	@SuppressWarnings("unchecked")
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

		// TODO check for error responses
		JsonNode json = response.get().asJson().path("data");

		// if the task is successfully created, Asana API returns a task id
		String taskId = json.get("id").asText();
		
		// if there are any attachments, add those to the task
		if(attachments!=null && attachments.size()>0) {
			// loop through all the attachments
			for(Map<String, Object> attachment : attachments) {
				
				// get the filehelper object
				FileHelper fileHelper = (FileHelper) attachment.get(Node.ATTR_TYPE_FILE);
				// get the file
				File file = fileHelper.getFileFromSource();
				
				///////////////////////////////////
				// TODO - this needs to be replaced with WS.post when Play 
				// starts supporting multipart file upload for WS api
				HttpPost postRequest = new HttpPost(API_BASE_URL+"/tasks/"+taskId+"/attachments");
				postRequest.setHeader("Authorization", "Bearer " + sat.getAccessToken());
				MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
				FileBody fileBody = new FileBody(file);
				multipartEntity.addPart(Node.ATTR_TYPE_FILE, fileBody);
				postRequest.setEntity(multipartEntity.build());
				
				HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
				CloseableHttpClient httpClient = httpClientBuilder.build();
				try {
					HttpResponse httpResponse = httpClient.execute(postRequest);
					HttpEntity httpEntity = httpResponse.getEntity();
					ObjectMapper responseMapper = new ObjectMapper();
					Map<String, Object> map = responseMapper.readValue(httpEntity.getContent(), Map.class);
					Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
					Long attachmentId = (Long) dataMap.get("id");

					if(attachmentId != null)
						fileHelper.deleteFile();

					httpClient.close();
				} catch (Exception exception) {
					UtilityHelper.logError(COMPONENT_NAME, "createTask()", exception.getMessage(), exception);
				}
				///////////////////////////////////

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

		// TODO - check for errors
		JsonNode json = response.get().asJson().path("data");
		
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
	

	/**
	 * Returns all Asana workspaces for the given user
	 */
	public JsonNode getWorkspaces() {
		ArrayList<IdName> list = new ArrayList<IdName>();

		String endPoint = API_BASE_URL + "/workspaces";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
		JsonNode json = response.get().asJson().path("data");

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
