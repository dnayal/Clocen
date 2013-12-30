package nodes.box;

import helpers.FileHelper;
import helpers.UtilityHelper;
import helpers.WSHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import models.IdName;
import models.ServiceAccessToken;
import nodes.Node;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

@SuppressWarnings("unchecked")
public class BoxServices implements BoxConstants {

	private static final String COMPONENT_NAME = "Box Services";

	private ServiceAccessToken sat;
	
	// if you delete this method, the Node initiation can fail
	// as the ServiceNodeHelper looks for all classes in the  
	// node.<<node id>> package and initializes using the  
	// default constructor
	public BoxServices() {
		sat = null;
	}
	
	
	public BoxServices(ServiceAccessToken sat) {
		this.sat = sat;
	}
	
	
	/**
	 * Returns Box folders (first level only) for the given user
	 * 
	 * TODO - Allow users to add triggers for multiple level folders
	 */
	public JsonNode getFolders() {
		String id = null;
		String name = null;

		// get the information on the root folder
		Promise<Response> response = WS.url(API_BASE_URL + "/folders/0")
				.setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
		
		JsonNode json = null; 
		Response result = response.get();
		
		// check for errors in response
		Box box = new Box();
		if(box.serviceResponseHasError(SERVICE_INFO_GET_FOLDERS, result.getStatus(), result.asJson(), sat))
			return null;
		else
			json = result.asJson();
		
		// map all the folders to the ID-Name pair
		ArrayList<IdName> list = new ArrayList<IdName>();
		// map the root folder
		list.add(new IdName(json.path("id").asText(), json.path("name").asText()));
		// get all folders in the root folder
		JsonNode itemEntries = json.path("item_collection").path("entries");
		Iterator<JsonNode> iterator = itemEntries.iterator();

		// iterate through all the folders and 
		// map the folders to the ID-Name pair
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			String type = node.path("type").asText();
			
			// only get folders; ignore the files
			if(!UtilityHelper.isEmptyString(type) && type.equalsIgnoreCase("folder")) {
				id = node.path("id").asText();
				name = node.path("name").asText();
				list.add(new IdName(id, name));
			}
			
		}
		
		json = null;
		ObjectMapper mapper = new ObjectMapper();
		json = mapper.valueToTree(list);
		
		return json;
	}
	
	
	/**
	 * Create new file
	 * 
	 * TODO - If there are multiple files to be attached, for now 
	 * this method only returns the information of the last file created
	 */
	public Map<String, Object> createFile(Map<String, Object> data) {
		// get the input variables
		ArrayList<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>) data.get("input");
		String parentFolderId = null, fileName = null;

		// object to store file
		ArrayList<Map<String, Object>> attachments = null;

		// loop through all the input variables for this service
		for(Map<String, Object> input : inputs) {
			String type = (String) input.get("type");
			String id = (String) input.get("id");
			
			// for a variable of type service, get the id parameter from the value
			if(type.equalsIgnoreCase(Node.ATTR_TYPE_SERVICE) && id.equalsIgnoreCase("parentfolder")) {
				Map<String, String> value = (Map<String, String>) input.get("value"); 
				parentFolderId = value.get("id");
			} else if(id.equalsIgnoreCase("filename")) {
				fileName = (String) input.get("value"); 
			} else if(id.equalsIgnoreCase("filedata")) {
				// get the attachments
				attachments = (ArrayList<Map<String, Object>>) input.get("value"); 
			}
		}
		
		// we need to have atleast the workspace id to be 
		// able to create a new task
		if (parentFolderId==null || fileName==null || attachments==null || attachments.size()<=0)
			return null;
		
		String attachmentId = null;
		
		// loop through all the attachments
		for(Map<String, Object> attachment : attachments) {
			
			// Prepare the objects to be sent to the POST request
			// get the filehelper object and then get the file
			FileHelper fileHelper = (FileHelper) attachment.get(Node.ATTR_TYPE_FILE);
			File file = fileHelper.getFileFromSource();
			// body parts to be sent to the REST POST service
			FileBody fileBody = new FileBody(file);
			StringBody stringBody = new StringBody(parentFolderId, ContentType.TEXT_PLAIN);
			// parameters expected by the REST POST service
			Map<String, AbstractContentBody> bodyPart = new HashMap<String, AbstractContentBody>();
			bodyPart.put("filename", fileBody);
			bodyPart.put("parent_id", stringBody);
			
			try {
				
				// Call the REST POST request with the required parameters
				Map<String, Object> map = WSHelper.postRequestWithFileUpload("https://upload.box.com/api/2.0/files/content", sat, bodyPart);
				
				ArrayList<Map<String, Object>> entries = (ArrayList<Map<String, Object>>) map.get("entries");
				attachmentId = (String) entries.get(0).get("id");
				if(attachmentId != null)
					fileHelper.deleteFile();
				
			} catch (Exception exception) {
				UtilityHelper.logError(COMPONENT_NAME, "createFile()", exception.getMessage(), exception);
				fileHelper.deleteFile();
			}

		} // attachments for-loop

		// add the output values back to the map 
		ArrayList<Map<String, Object>> outputs = (ArrayList<Map<String, Object>>) data.get("output");
		for(Map<String, Object> output : outputs) {
			String id = (String) output.get("id");
			
			if(id.equalsIgnoreCase("fileid")) {
				output.put("value", attachmentId);
			} else if(id.equalsIgnoreCase("filename")) {
				output.put("value", fileName);
			} else if(id.equalsIgnoreCase("filedata")) {
				output.put("value", attachments);
			} 
		}
		
		UtilityHelper.logMessage(COMPONENT_NAME, "createFile()", "New file created in Box for user [" + sat.getKey().getUserId() + "]");

		return data;
	}


	private void getFile(String fileId) {
		String endPoint = API_BASE_URL + "/files/" + fileId + "/content";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		FileOutputStream file;
		try {
			file = new FileOutputStream("BOX_API_OUTPUT_" + System.currentTimeMillis());
			InputStream input = response.get().getBodyAsStream();
			while((bytesRead = input.read(buffer))!=-1)
				file.write(buffer, 0, bytesRead);
			file.flush();file.close();
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
