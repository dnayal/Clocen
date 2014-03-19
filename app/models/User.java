package models;

import helpers.SecurityHelper;
import helpers.ServiceNodeHelper;
import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import nodes.Node;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import play.mvc.Controller;

/**
 * Model with attributes and operations for User object
 */
@SuppressWarnings("serial")
@Entity
public class User extends Model {
	
	private static final String COMPONENT_NAME = "User Model";
	private static final String ADMIN_ROLE = "admin";

	public static final Integer PASSWORD_FORGOT_VIEW = 1;
	public static final Integer PASSWORD_RESET_VIEW = 2;
	public static final Integer PASSWORD_FORGOT_PROCESSED = 3;

	@Id
	@Column(length=100)
	private String userId;
	@Column(length=100)
	private String name;
	@Required
	@Email
	@Column(length=100, unique=true)
	private String email;
	@Column(length=100)
	private String password;
	@Column(length=100)
	private String company;
	@Column(length=200)
	private String website;
	@Column(length=100)
	private String country;
	@Column(length=20)
	private String role;
	private Date createTimestamp;
	private static Finder<String, User> find = new Finder<String, User>(String.class, User.class);
	
	
	public User(String userId, String name, String email, String password, String company, String website, 
			String country, String role, Date createTimestamp) {
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.password = password;
		this.company = company;
		this.website = website;
		this.country = country;
		this.role = role;
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

	
	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getCompany() {
		return company;
	}


	public void setCompany(String company) {
		this.company = company;
	}


	public String getWebsite() {
		return website;
	}


	public void setWebsite(String website) {
		this.website = website;
	}


	public String getCountry() {
		return country;
	}


	public void setCountry(String country) {
		this.country = country;
	}


	public String getRole() {
		return role;
	}


	public void setRole(String role) {
		this.role = role;
	}


	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	
	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	

	@Override
	public String toString() {
		return "User [userId=" + userId + ", name=" + name + ", email=" + email
				+ ", password=" + password + ", company=" + company
				+ ", website=" + website + ", country=" + country + ", role="
				+ role + ", createTimestamp=" + createTimestamp + "]";
	}


	public static User getUser(String userId) {
		if(userId==null)
			return null;
		
		return find.byId(userId);	
	}
	

	public static User getUserByEmail(String email) {
		return find.where().eq("email", email).findUnique();
	}


	@Override
	public void save() {
		User user = find.byId(userId);
		try{
			if(user == null)
				super.save();
			else
				super.update();
				
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}

	
	/**
	 * Returns true if the user with given credentials exists
	 */
	public static Boolean login(String email, String password) {
		User checkUser = getUserByEmail(email);
		if(checkUser == null)
			return false;
		
		User user = find.where().eq("email", email)
				.eq("password", SecurityHelper.generateHash(checkUser.getUserId(), password)).findUnique();
		
		if(user==null) {
			return false;
		} else {
			setCurrentUser(user);
			return true;
		}
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
		
	
	/**
	 * Returns whether the user an admin
	 */
	public Boolean isAdmin() {
		if(!UtilityHelper.isEmptyString(role) && role.equalsIgnoreCase(ADMIN_ROLE))
			return true;
		else
			return false;
	}
	
	
	/**
	 * Returns whether the currently logged in user is admin
	 */
	public static Boolean isCurrentUserAdmin() {
		User user = getCurrentUser();
		if (user!=null && user.isAdmin())
			return true;
		else
			return false;
	}


	/**
	 * Returns the list of all nodes, along with the flag mentioning 
	 * whether the user has authorized use of that service.
	 */
	public List<ServiceNodeInfo> getAllNodes() {
		// initialize the ArrayList 
		ArrayList<ServiceNodeInfo> nodeList = new ArrayList<ServiceNodeInfo>();

		// Get all node ids
		HashSet<String> allNodeIds = ServiceNodeHelper.getAllNodeIds();
		
		// go through each node
		for(String nodeId : allNodeIds) {
			// Instantiate the node
			Node node = ServiceNodeHelper.getNode(nodeId);
			
			// Check whether the user has authorized the node
			Boolean isAuthorized = node.isAuthorized(userId);
			
			// create the ServiceNodeInfo object
			ServiceNodeInfo serviceNode = new ServiceNodeInfo(nodeId, node.getName(), node.getLogo(), node.getDescription(), 
					isAuthorized
					?controllers.routes.Application.refreshOauth2Token(node.getNodeId()).toString()	
					:controllers.routes.Application.authorizeOauth2Call(node.getNodeId()).toString(),
					node.getAppURL(),
					isAuthorized);

			// add it to the list
			nodeList.add(serviceNode);
		}
		
		// return the list back
		return nodeList;
	}

}
