package models;

import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import play.db.ebean.Model;

@Entity
@SuppressWarnings("serial")
public class Process extends Model {

	private static final String COMPONENT_NAME = "Process Model";
	public static final String TRIGGER_TYPE_POLL = "poll";
	public static final String TRIGGER_TYPE_HOOK = "hook";
	
	@Id
	@Column(length=100)
	private String processId;
	@Column(length=100)
	private String userId;
	@Column(length=25)
	private String triggerNode;
	@Column(length=10)
	private String triggerType;
	@Column(columnDefinition="text")
	private String processData;
	private Date createTimestamp;
	private static Finder<String, Process> find = new Finder<String, Process>(String.class, Process.class);

	
	public Process(String processId, String userId, String triggerNode,
			String triggerType, String processData, Date createTimestamp) {
		this.processId = processId;
		this.userId = userId;
		this.triggerNode = triggerNode;
		this.triggerType = triggerType;
		this.processData = processData;
		this.createTimestamp = createTimestamp;
	}

	
	public String getProcessId() {
		return processId;
	}

	
	public void setProcessId(String processId) {
		this.processId = processId;
	}

	
	public String getUserId() {
		return userId;
	}

	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	
	public String getTriggerNode() {
		return triggerNode;
	}

	
	public void setTriggerNode(String triggerNode) {
		this.triggerNode = triggerNode;
	}

	
	public String getTriggerType() {
		return triggerType;
	}

	
	public void setTriggerType(String triggerType) {
		this.triggerType = triggerType;
	}

	
	public String getProcessData() {
		return processData;
	}

	
	public void setProcessData(String processData) {
		this.processData = processData;
	}

	
	public Date getCreateTimestamp() {
		return createTimestamp;
	}

	
	public void setCreateTimestamp(Date createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	
	
	public static Process getProcess(String processId) {
		return find.byId(processId);
	}
	

	public static List<Process> getProcessesForUser(String userId) {
		List<Process> list = new ArrayList<Process>();
		list = Process.find.where().eq("user_id", userId).findList();
		return list;
	}
	

	@Override
	public void save() {
		try {
			Process token = getProcess(processId);
			
			if(token==null)
				super.save();
			else
				super.update();
			
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}
	
	
}
