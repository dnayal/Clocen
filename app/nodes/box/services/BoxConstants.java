package nodes.box.services;

public interface BoxConstants {

	static final String NODE_ID = "box";

	static final String APP_NAME = "Box";
	
	static final String APP_DESCRIPTION = "Simple file sharing in the cloud";

	// api-partner@clocen.com
	static final String CLIENT_ID = "e5ndffixdomcndl6s9o5uiz5eo137gvf"; // API Key
	static final String CLIENT_SECRET = "wB4t6mCLchPz9AqhaHFlgsyvfaLzijzz";
	
	// Test API configuration - dnayal@gmail.com
	static final String TEST_CLIENT_ID = "af8on2xlppewm0m3i0ceng2yxg7rrgms";
	static final String TEST_CLIENT_SECRET = "I9AtREMVttIl7exrfJ5FIEoE43U5uB6j";

	static final String OAUTH_AUTHORIZE_URL = "https://www.box.com/api/oauth2/authorize";
	static final String OAUTH_TOKEN_URL = "https://www.box.com/api/oauth2/token";
	static final String APP_URL = "https://www.box.com";
	static final String API_BASE_URL = "https://api.box.com/2.0";

	static final String TRIGGER_UPLOADED = "uploaded";
	static final String TRIGGER_CREATED = "created";
	static final String TRIGGER_DELETED = "deleted";

	static final String ITEM_TYPE_FILE = "file";
	static final String ITEM_TYPE_FOLDER = "folder";
	
	static final String SERVICE_INFO_GET_FOLDERS = "getfolders";
	static final String SERVICE_TRIGGER_FILE_UPLOADED = "fileuploaded";
	static final String SERVICE_TRIGGER_NEW_FOLDER_CREATED = "newfoldercreated";
	static final String SERVICE_ACTION_CREATE_FILE = "createfile";
	static final String SERVICE_ACTION_CREATE_FOLDER = "createfolder";
	static final String SERVICE_INTERNAL_GET_USER_ID = "getuserid";
	static final String SERVICE_INTERNAL_RENAME_FILE = "renamefile";
	
	static final String INPUT_ID_PARENT_FOLDER = "parentfolder";
	
	static final String PARAM_USER_ID = "user_id";

}
