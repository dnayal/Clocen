package nodes.box;

import helpers.Helper;
import helpers.OAuthHelper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import play.Logger;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import nodes.Node;
import nodes.asana.Asana;

/**
 * Box access token is valid for 1 hour. In order to get a new valid token, 
 * use the refresh_token, which is valid for 14 days.
 * 
 * BOX API DOC - http://developers.box.com/docs/
 *
 */
public class Box implements Node {

	private static final String API_KEY = "af8on2xlppewm0m3i0ceng2yxg7rrgms"; // client id
	private static final String CLIENT_SECRET = "I9AtREMVttIl7exrfJ5FIEoE43U5uB6j";
	private static final String OAUTH_AUTHORIZE_URL = "https://www.box.com/api/oauth2/authorize";
	private static final String OAUTH_TOKEN_URL = "https://www.box.com/api/oauth2/token";
	private static final String NODE_ID = "box";
	private String accessToken = null;
	

	@Override
	public String getAccess(AccessType accessType, String data) {
		switch(accessType) {
			case OAUTH_AUTHORIZE:
				return "https://www.box.com/api/oauth2/authorize?response_type=code&client_id="+
					API_KEY+"&state=authenticated&redirect_uri=" + OAuthHelper.getOAuthRedirectURI(NODE_ID);
				
			case OAUTH_TOKEN:
				if(Helper.isEmptyString(data))
					throw new RuntimeException("Oauth code empty for Box token call");

				accessToken = WS.url("https://www.box.com/api/oauth2/token")
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.post("grant_type=authorization_code&code="+data+"&client_id="+API_KEY+
						"&client_secret="+CLIENT_SECRET+"&redirect_uri=" + OAuthHelper.getOAuthRedirectURI(NODE_ID))
				.get()
				.asJson()
				.get("access_token").asText();
				
				Logger.info("Box Access Token : " + accessToken);
				return accessToken;
				
			case OAUTH_RENEW:
				return null;
			
			default:
				return null;
		}
	}
	
	
	/* 
	 * TODO 
	 * This method needs to be updated to check 
	 * whether we have the access to the given node
	 */
	@Override
	public Boolean hasAccess() {
		return !Helper.isEmptyString(accessToken);
	}


	@Override
	public String getNodeId() {
		return NODE_ID;
	}


	public void getFile(String fileId) {
		String endPoint = "https://api.box.com/2.0/files/" + fileId + "/content";
		Promise<Response> response = WS.url(endPoint).setHeader("Authorization", "Bearer ZCHHnwVAa3apewaDxFhtGb6CrpQUNglX").get();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		Integer status = response.get().getStatus();
		String statusText = response.get().getStatusText();
		Logger.info("<===STATUS===>");
		Logger.info(status+"");
		Logger.info("<===STATUS TEXT===>");
		Logger.info(statusText);
		Asana asana = new Asana();
		asana.getWorkspaces();
		asana.createTask();
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

}
