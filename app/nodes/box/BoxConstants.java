package nodes.box;

interface BoxConstants {

	static final String NODE_ID = "box";

	static final String APP_NAME = "Box";
	
	static final String APP_DESCRIPTION = "Simple file sharing in the cloud";

	static final String CLIENT_ID = "af8on2xlppewm0m3i0ceng2yxg7rrgms"; // API Key
	
	static final String CLIENT_SECRET = "I9AtREMVttIl7exrfJ5FIEoE43U5uB6j";
	
	static final String OAUTH_AUTHORIZE_URL = "https://www.box.com/api/oauth2/authorize";
	
	static final String OAUTH_TOKEN_URL = "https://www.box.com/api/oauth2/token";
	
	static final String APP_URL = "https://www.box.com";

	static final String API_BASE_URL = "https://api.box.com/2.0";

	static final String TRIGGER_UPLOADED = "uploaded";
	static final String TRIGGER_DELETED = "deleted";
	
	static final String SERVICE_INFO_GET_FOLDERS = "getfolders";
	static final String SERVICE_ACTION_CREATE_FILE = "createfile";

}
