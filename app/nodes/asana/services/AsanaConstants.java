package nodes.asana.services;

/**
 * All important constants used by Asana
 */
public interface AsanaConstants {

	static final String NODE_ID = "asana";
	static final String APP_NAME = "Asana";
	static final String APP_DESCRIPTION = "Task management for teams";

	// api-partner@clocen.com
	static final String CLIENT_ID = "10083524829090"; // API Key
	static final String CLIENT_SECRET = "03036fa4f9c94d66b97ff87e16e9a4d5";
	
	// Test API configuration - dnayal@gmail.com
	static final String TEST_CLIENT_ID = "7169780706601";
	static final String TEST_CLIENT_SECRET = "3f230584e30bcffa7f9149a3c5332319";
	
	static final String OAUTH_AUTHORIZE_URL = "https://app.asana.com/-/oauth_authorize";
	static final String OAUTH_TOKEN_URL = "https://app.asana.com/-/oauth_token";
	static final String APP_URL = "https://app.asana.com";
	static final String API_BASE_URL = "https://app.asana.com/api/1.0";
	
	static final String SERVICE_TRIGGER_NEW_TASK_CREATED = "newtaskcreated";
	static final String SERVICE_TRIGGER_NEW_PROJECT_CREATED = "newprojectcreated";
	static final String SERVICE_ACTION_CREATE_TASK = "createtask";
	static final String SERVICE_ACTION_CREATE_PROJECT = "createproject";
	static final String SERVICE_INFO_GET_WORKSPACES = "getworkspaces";
	static final String SERVICE_INTERNAL_GET_ATTACHMENTS = "getattachments";
	
}
