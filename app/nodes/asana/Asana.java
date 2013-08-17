package nodes.asana;

import helpers.UtilityHelper;
import helpers.OAuthHelper;

import java.util.Iterator;

import nodes.Node;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

public class Asana implements Node {

	private static final String CLIENT_ID = "7169780706601"; // API Key
	private static final String CLIENT_SECRET = "3f230584e30bcffa7f9149a3c5332319";
	private static final String OAUTH_AUTHORIZE_URL = "https://app.asana.com/-/oauth_authorize";
	private static final String OAUTH_TOKEN_URL = "https://www.box.com/api/oauth2/token";
	private static final String NODE_ID = "asana";
	private String accessToken = null;

	@Override
	public String authorize(AccessType accessType, String data) {
		return OAuthHelper.getAccess(NODE_ID, CLIENT_ID, CLIENT_SECRET, 
				accessType, data, OAUTH_AUTHORIZE_URL, OAUTH_TOKEN_URL);
	}
	
	
	/* 
	 * TODO 
	 * This method needs to be updated to check 
	 * whether we have the access to the given node
	 */
	@Override
	public Boolean hasAccess() {
		return !UtilityHelper.isEmptyString(accessToken);
	}


	@Override
	public String getNodeId() {
		return NODE_ID;
	}
	

	public void getWorkspaces() {
		String endPoint = "https://app.asana.com/api/1.0/workspaces";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer " + accessToken).get();
		int status = response.get().getStatus();
		String statusText = response.get().getStatusText();
		Iterator<JsonNode> iterator = response.get().asJson().path("data").getElements();
		Logger.info("++++++++++++++++");
		Logger.info(response.get().getBody());
		Logger.info("++++++++++++++++");
		Logger.info("ASANA STATUS: [" + status + "] " + statusText);
		Logger.info("=====WORKSPACES=====");
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			Logger.info(node.get("id").asText() + " : " + node.get("name").asText()); 
		}
		Logger.info("+-+-+-+-+-+-+-+-+-+-+-+");
	}


	public void createTask() {
		String endPoint = "https://app.asana.com/api/1.0/workspaces/180666096176/tasks";
		JsonNode json=null;
			json = WS.url(endPoint).setHeader("Authorization", "Bearer " + accessToken)
					.setHeader("Content-Type", "application/x-www-form-urlencoded")
					.post("name=Successful+Task&notes=YYYYYIIIIPPPPEEEE&assignee=177113805935")
							.get().asJson();
		Logger.info("NEW TASK : " + json.toString());
	}


}
