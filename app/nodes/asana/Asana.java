package nodes.asana;

import helpers.ServiceNodeHelper;

import java.util.ArrayList;
import java.util.Iterator;

import models.IdName;
import models.ServiceAccessToken;
import models.User;
import nodes.Node;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;

/*
 * TODO Update Asana application and callback URL
 */
public class Asana implements Node {

	private static final String CLIENT_ID = "7169780706601"; // API Key
	private static final String CLIENT_SECRET = "3f230584e30bcffa7f9149a3c5332319";
	private static final String OAUTH_AUTHORIZE_URL = "https://app.asana.com/-/oauth_authorize";
	private static final String OAUTH_TOKEN_URL = "https://app.asana.com/-/oauth_token";
	private static final String APP_URL = "https://app.asana.com";
	private static final String NODE_ID = "asana";

	@Override
	public String authorize(String userId, AccessType accessType, String data) {
		return ServiceNodeHelper.getAccess(userId, NODE_ID, CLIENT_ID, CLIENT_SECRET, 
				accessType, data, OAUTH_AUTHORIZE_URL, OAUTH_TOKEN_URL);
	}
	
	
	@Override
	public String getNodeId() {
		return NODE_ID;
	}
	

	@Override
	public String getName() {
		return "Asana";
	}
	

	@Override
	public String getDescription() {
		return "Task management for teams";
	}
	
	
	
	public JsonNode getWorkspaces(User user) {
		ServiceAccessToken sat = user.getServiceAccessToken(NODE_ID);
		ArrayList<IdName> list = new ArrayList<IdName>();

		String endPoint = "https://app.asana.com/api/1.0/workspaces";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken()).get();
		JsonNode json = response.get().asJson().path("data");

		Iterator<JsonNode> iterator = json.getElements();
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			list.add(new IdName(node.get("id").asText(), node.get("name").asText()));
		}
		
		json = null;
		ObjectMapper mapper = new ObjectMapper();
		json = mapper.valueToTree(list);

		return json;
	}


	public void createTask(User user) {
		ServiceAccessToken sat = user.getServiceAccessToken(NODE_ID);
		
		String endPoint = "https://app.asana.com/api/1.0/workspaces/180666096176/tasks";
		
		WS.url(endPoint).setHeader("Authorization", "Bearer " + sat.getAccessToken())
			.setHeader("Content-Type", "application/x-www-form-urlencoded")
			.post("name=Successful+Task&notes=YYYYYIIIIPPPPEEEE&assignee=177113805935")
			.get().asJson();
	}


	@Override
	public JsonNode callInfoService(User user, String service) {
		if (service.equalsIgnoreCase("getworkspaces")) {
			return getWorkspaces(user);
		} else {
			return null;
		}
	}


	@Override
	public String getLogo() {
		return controllers.routes.Assets.at("images/nodes/asana.png").absoluteURL(Controller.request());
	}


	@Override
	public String getTriggerType() {
		return Node.TRIGGER_TYPE_POLL;
	}


	@Override
	public String getAppURL() {
		return APP_URL;
	}

}
