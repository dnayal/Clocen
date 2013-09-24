package models;

import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import nodes.Node;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import play.mvc.Controller;

@SuppressWarnings("serial")
@Entity
public class User extends Model {
	
	private static final String COMPONENT_NAME = "User Model";

	@Id
	@Column(length=100)
	String userId;
	
	@Required
	@Column(length=100)
	String name;
	
	@Required
	@Email
	@Column(length=100, unique=true)
	String email;
	
	
	Date createTimestamp;
	
	
	public static Finder<String, User> find = new Finder<String, User>(String.class, User.class);
	
	
	public User() {}
	
	
	public User(String userId, String name, String email, Date createTimestamp) {
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.createTimestamp = createTimestamp;
	}


	public String getUserId() {
		return userId;
	}

	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	public String getName() {
		return name;
	}

	
	public void setName(String name) {
		this.name = name;
	}

	
	public String getEmail() {
		return email;
	}

	
	public void setEmail(String email) {
		this.email = email;
	}

	
	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	
	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	

	public static User getUser(String userId) {
		return find.byId(userId);	
	}
	

	public static User getUserByEmail(String email) {
		return find.where().eq("email", email).findUnique();
	}


	@Override
	public String toString() {
		return "User [userId=" + userId + ", name=" + name + ", email=" + email
				+ ", createTimestamp=" + createTimestamp + "]";
	}
	
	
	/**
	 * Logout the current user
	 */
	public static void logout() {
		Controller.ctx().session().clear();
	}
	
	
	/**
	 * Returns whether a user has logged in
	 */
	public static Boolean isLoggedIn() {
		String userId = Controller.ctx().session().get("user_id");
		if(UtilityHelper.isEmptyString(userId)) {
			return false;
		} else { 
			return true;
		}
	}
	
	
	/**
	 * Returns currently logged in user
	 */
	public static User getCurrentUser() {
		String userId = Controller.ctx().session().get("user_id");
		if(UtilityHelper.isEmptyString(userId))
			return null;
		else 
			return User.find.byId(userId);
	}
	
	
	/**
	 * Sets the id of currently logged in user in session
	 */
	public static void setCurrentUser(User user) {
		Controller.ctx().session().put("user_id", user.getUserId());
	}


	/**
	 * Sets the user as currently logged in
	 */
	public void setAsCurrentUser() {
		Controller.ctx().session().put("user_id", userId);
	}


	public List<ServiceAccessToken> getAllServiceTokens() {
		List<ServiceAccessToken> tokenList = new ArrayList<ServiceAccessToken>();
		tokenList = ServiceAccessToken.find.where()
				.eq("user_id", userId)
//				.ge("expiration_time", Calendar.getInstance().getTime())
				.findList();
		for(ServiceAccessToken token: tokenList) {
			if(token.getExpirationTime().before(Calendar.getInstance().getTime())) {
				UtilityHelper.logMessage(COMPONENT_NAME, "getAllServiceTokens()", "Token Expired - user:" + token.getKey().getUserId() + " node:" + token.getKey().getNodeId());
				if(!token.refreshToken()) {
					UtilityHelper.logMessage(COMPONENT_NAME, "getAllServiceTokens()", "Removing Token - user:" + token.getKey().getUserId() + " node:" + token.getKey().getNodeId());
					tokenList.remove(token);
				}
			}
		}
		return tokenList;
	}
	
	
	/**
	 * Returns the list of all nodes, along with the flag mentioning 
	 * whether the user has authorized use of that service.
	 * 
	 * This method first goes through the list of all nodes, then for 
	 * each node finds out for which ones has the user provided access, 
	 * sets the flag accordingly - and returns the list of all the nodes
	 */
	public List<ServiceNodeInfo> getAllNodes() {
		ArrayList<ServiceNodeInfo> nodeList = new ArrayList<ServiceNodeInfo>();
		boolean isAuthorized;

		List<ServiceAccessToken> serviceTokens = getAllServiceTokens();
		HashSet<String> allNodeIds = ServiceNodeHelper.getAllNodeIds();
		
		Iterator<String> allNodeIterator = allNodeIds.iterator();
		
		while(allNodeIterator.hasNext()) {
			String nodeId = allNodeIterator.next();
			isAuthorized = false;
			
			for (ServiceAccessToken serviceToken : serviceTokens) {
				if(serviceToken.getKey().getNodeId().equalsIgnoreCase(nodeId)) {
					isAuthorized = true;
					break;
				}
			}
			
			Node node = ServiceNodeHelper.getNode(nodeId);
			ServiceNodeInfo serviceNode = new ServiceNodeInfo(nodeId, node.getName(), node.getLogo(), node.getDescription(), 
					isAuthorized
						?controllers.routes.OAuthController.refreshToken(node.getNodeId()).toString()	
						:controllers.routes.OAuthController.authorizeCall(node.getNodeId()).toString(), 
					isAuthorized);
			nodeList.add(serviceNode);
		}

		return nodeList;
	}


	public ServiceAccessToken getServiceAccessToken(String nodeId) {
		ServiceAccessToken token = ServiceAccessToken.find.where().eq("user_id", userId).eq("node_id", nodeId).findUnique();
		
		if (token.getExpirationTime().before(Calendar.getInstance().getTime())) {
			UtilityHelper.logError(COMPONENT_NAME, "getServiceAccessToken()", "Token expired", new RuntimeException("Token expired for Node:" +nodeId + " User:"+userId));
			if(!token.refreshToken()) {
				UtilityHelper.logError(COMPONENT_NAME, "getServiceAccessToken()", "Token refresh failed", new RuntimeException("Unable to refresh token for Node:" +nodeId + " User:"+userId));
				return null;
			} else {
				// retrieve token again, if refresh is successful
				token = ServiceAccessToken.find.where().eq("user_id", userId).eq("node_id", nodeId).findUnique();
				UtilityHelper.logMessage(COMPONENT_NAME, "getServiceAccessToken()", "Token refreshed");
			}
		}

		return token;
	}

}
