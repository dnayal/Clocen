package models;

import java.util.Date;

import helpers.UtilityHelper;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.PersistenceException;

import play.db.ebean.Model;

@SuppressWarnings("serial")
@Entity
public class NodeParams extends Model {
	
	private static final String COMPONENT_NAME = "NodeParams Model";
	
	@EmbeddedId
	private NodeParamsKey key;
	
	@Column(length=100)
	private String value;

	private Date createTimestamp;
	
	private static Finder<NodeParamsKey, NodeParams> find = new Finder<NodeParamsKey, NodeParams>(NodeParamsKey.class, NodeParams.class);

	
	public NodeParams(NodeParamsKey key, String value, Date createTimestamp) {
		this.key = key;
		this.value = value;
		this.createTimestamp = createTimestamp;
	}
	

	public NodeParams(String userId, String nodeId, String parameter, String value, Date createTimestamp) {
		this.key = new NodeParamsKey(userId, nodeId, parameter);
		this.value = value;
		this.createTimestamp = createTimestamp;
	}
	
	
	public NodeParamsKey getKey() {
		return key;
	}


	public void setKey(NodeParamsKey key) {
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

	
	/**
	 * Stores the value for the given node parameter
	 */
	public void store() {
		NodeParams nodeParams = retrieveByKey(key);
		
		try{
			if(nodeParams == null)
				super.save();
			else
				super.update();
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "store()", exception.getMessage(), exception);
		}
	}
	
	
	/**
	 * Retrieves the NodeParams by parameter
	 */
	public static NodeParams retrieveByKey(NodeParamsKey key) {
		return find.byId(key);
	}


	/**
	 * Retrieves the NodeParams by parameter and value
	 */
	public static NodeParams retrieveByValue(String nodeId, String parameter, String value) {
		NodeParams nodeParams = find.where().eq("node_id", nodeId).eq("parameter", parameter).eq("value", value).findUnique();
		
		return nodeParams;
	}

}
