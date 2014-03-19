package models;

import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PersistenceException;

import play.db.ebean.Model;

/**
 * Model to save save access tokens for users
 */
@SuppressWarnings("serial")
@Entity
public class ServiceAuthToken extends Model {

	private static final String COMPONENT_NAME = "ServiceAuthToken Model";
	
	@EmbeddedId
	private ServiceAuthTokenKey key;
	@Column(length=500)
	private String value;
	private Date createTimestamp;
	private static Finder<ServiceAuthTokenKey, ServiceAuthToken> find = new Finder<ServiceAuthTokenKey, ServiceAuthToken>(ServiceAuthTokenKey.class, ServiceAuthToken.class);

	
	public ServiceAuthToken(ServiceAuthTokenKey key, String value, Date createTimestamp) {
		this.key = key;
		this.value = value;
		this.createTimestamp = createTimestamp;
	}

	
	public ServiceAuthToken(String userId, String nodeId, String token, String value, Date createTimestamp) {
		this.key = new ServiceAuthTokenKey(userId, nodeId, token);
		this.value = value;
		this.createTimestamp = createTimestamp;
	}

	
	public ServiceAuthTokenKey getKey() {
		return key;
	}

	
	public void setKey(ServiceAuthTokenKey key) {
		this.key = key;
	}


	public String getValue() {
		return value;
	}

	
	public void setValue(String value) {
		this.value = value;
	}
	
	
	public Date getCreateTimestamp() {
		return createTimestamp;
	}
	
	
	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	

	public static ServiceAuthToken getServiceAuthToken(ServiceAuthTokenKey key) {
		return find.byId(key);
	}
	
	
	public static List<ServiceAuthToken> getServiceAuthTokens(String userId, String nodeId) {
		List<ServiceAuthToken> tokenList = new ArrayList<ServiceAuthToken>();

		tokenList = ServiceAuthToken.find.where()
				.eq("user_id", userId)
				.eq("node_id", nodeId)
				.findList();
		
		return tokenList;
	}
	
	
	public static List<ServiceAuthToken> getAllServiceAuthTokensForUser(String userId) {
		List<ServiceAuthToken> tokenList = new ArrayList<ServiceAuthToken>();

		tokenList = ServiceAuthToken.find.where()
				.eq("user_id", userId)
				.findList();
		
		return tokenList;
	}
	
	
	@Override
	public void delete() {
		super.delete();
	}
	
	
	@Override
	public void save() {
		try {
			ServiceAuthToken token = getServiceAuthToken(key);
			
			if(token==null)
				super.save();
			else
				super.update();
			
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}


	@Override
	public String toString() {
		return "ServiceAuthToken [key=" + key + ", value=" + value + ", createTimestamp=" + createTimestamp + "]";
	}
	
}
