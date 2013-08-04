package nodes.box;

import play.Logger;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.WS.Response;
import nodes.Node;

/**
 * Box access token is valid for 1 hour. In order to get a new valid token, 
 * use the refresh_token, which is valid for 14 days.
 *
 */
public class Box implements Node {

	private static String API_KEY = "af8on2xlppewm0m3i0ceng2yxg7rrgms"; // client id
	private static String CLIENT_SECRET = "I9AtREMVttIl7exrfJ5FIEoE43U5uB6j";
	private static String NODE_ID = "box";
	
	@Override
	public void authenticate(String code) {
		WS.url("https://www.box.com/api/oauth2/token")
		.setHeader("Content-Type", "application/x-www-form-urlencoded")
		.post("grant_type=authorization_code&code="+code+"&client_id="+API_KEY+"&client_secret="+CLIENT_SECRET)
		.map(new Function<WS.Response, String>() {
			@Override
			public String apply(Response response) throws Throwable {
				String accessToken = response.asJson().get("access_token").asText();
				Logger.info("Access Token : " + accessToken);
				return accessToken;
			}
		});
	}

	@Override
	public String getNodeId() {
		return NODE_ID;
	}

	@Override
	public String getOauthAuthorizationURL() {
		return "https://www.box.com/api/oauth2/authorize?response_type=code&client_id="+API_KEY+"&state=authenticated";
	}

}
