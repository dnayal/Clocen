package nodes.box;

import helpers.ServiceNodeHelper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import models.IdName;
import models.ServiceAccessToken;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;

/**
 * Box access token is valid for 1 hour. In order to get a new valid token, 
 * use the refresh_token, which is valid for 14 days.
 * 
 * BOX API DOC - http://developers.box.com/docs/
 *
 */
public class Box implements Node {

	private static final String CLIENT_ID = "af8on2xlppewm0m3i0ceng2yxg7rrgms"; // API Key
	private static final String CLIENT_SECRET = "I9AtREMVttIl7exrfJ5FIEoE43U5uB6j";
	private static final String OAUTH_AUTHORIZE_URL = "https://www.box.com/api/oauth2/authorize";
	private static final String OAUTH_TOKEN_URL = "https://www.box.com/api/oauth2/token";
	private static final String NODE_ID = "box";
	

	@Override
	public String authorize(AccessType accessType, String data) {
		return ServiceNodeHelper.getAccess(NODE_ID, CLIENT_ID, CLIENT_SECRET, 
				accessType, data, OAUTH_AUTHORIZE_URL, OAUTH_TOKEN_URL);
	}
	
	
	@Override
	public String getNodeId() {
		return NODE_ID;
	}


	@Override
	public String getName() {
		return "Box";
	}
	

	@Override
	public String getDescription() {
		return "Simple file sharing in the cloud";
	}
	

	public JsonNode getFolders(User user) {
		ServiceAccessToken sat = user.getServiceAccessToken(NODE_ID);
		String id = null;
		String name = null;

		String endPoint = "https://api.box.com/2.0/folders/0";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
		JsonNode json = response.get().asJson();
		ArrayList<IdName> list = new ArrayList<IdName>();
		list.add(new IdName(json.path("id").asText(), json.path("name").asText()));
		JsonNode itemEntries = json.path("item_collection").path("entries");
		Iterator<JsonNode> iterator = itemEntries.iterator();

		Logger.info("=====Folders=====");
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			id = node.path("id").asText();
			name = node.path("name").asText();
			Logger.info( id + " : " + name); 
			list.add(new IdName(id, name));
		}
		Logger.info("=====END=====");
		
		json = null;
		ObjectMapper mapper = new ObjectMapper();
		json = mapper.valueToTree(list);
		
		return json;
	}


	public void getFile(String fileId) {
		String endPoint = "https://api.box.com/2.0/files/" + fileId + "/content";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer ZCHHnwVAa3apewaDxFhtGb6CrpQUNglX").get();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		Logger.info("<===BODY===>");
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
		Logger.info("File created");
	}


	@Override
	public JsonNode callInfoService(User user, String service) {
		if (service.equalsIgnoreCase("getfolders")) {
			return getFolders(user);
		} else {
			return null;
		}
	}


	@Override
	public String getLogo() {
		return controllers.routes.Assets.at("images/nodes/box.png").absoluteURL(Controller.request());
	}

}
