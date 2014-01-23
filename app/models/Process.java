package models;

import helpers.UtilityHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import nodes.Node;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.Play;
import play.db.ebean.Model;
import play.libs.Json;

import com.avaje.ebean.Expr;

/**
 * The process model contains all process related information and operations
 */
@Entity
@SuppressWarnings({"serial", "unchecked"})
public class Process extends Model {

	private static final String COMPONENT_NAME = "Process Model";
	
	@Id
	@Column(length=100)
	private String processId;
	@Column(length=100)
	private String userId;
	@Column(length=10)
	private String version;
	@Column(length=25)
	private String triggerNode;
	@Column(length=10)
	private String triggerType;
	@Column(columnDefinition="text")
	private String processData;
	private Boolean paused = false;
	private Date createTimestamp;
	private static Finder<String, Process> find = new Finder<String, Process>(String.class, Process.class);

	
	public Process(String processId, String userId, String triggerNode,
			String triggerType, String processData, Boolean paused, Date createTimestamp) {
		this.processId = processId;
		this.userId = userId;
		this.version = Play.application().configuration().getString("process.version");
		this.triggerNode = triggerNode;
		this.triggerType = triggerType;
		this.processData = processData;
		this.paused = paused;
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

	
	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
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
	
	
	/**
	 * Maps the process data to object so that it can be easily manipulated
	 */
	public ArrayList<Map<String, Object>> getProcessDataAsObject() {
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<Map<String, Object>> array = null;
		
		try {
			array = mapper.readValue(Json.parse(getProcessData()), ArrayList.class);
		} catch (Exception exception) {
			UtilityHelper.logError(COMPONENT_NAME, "getProcessDataAsObject()", exception.getMessage(), exception);
		}
		
		return array;
	}

	
	public void setProcessData(String processData) {
		this.processData = processData;
	}

	
	public Boolean isPaused() {
		if(paused==null)
			return false;
		
		return paused;
	}


	public void setPaused(Boolean paused) {
		this.paused = paused;
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
	
	
	/**
	 * All active processes, including ones with trigger type as poll or hook
	 */
	public static List<Process> getAllActiveProcesses() {
		return find.where().or(Expr.isNull("paused"), Expr.ne("paused", true)).findList();
	}
	
	
	/**
	 * All active processes, but only with trigger type as poll
	 */
	public static List<Process> getAllActivePollableProcesses() {
		return find.where().eq("trigger_type",Node.TRIGGER_TYPE_POLL)
				.or(Expr.isNull("paused"), Expr.ne("paused", true)).findList();
	}
	

	public static List<Process> getProcessesForUser(String userId) {
		List<Process> list = new ArrayList<Process>();
		list = Process.find.where().eq("user_id", userId).orderBy("createTimestamp desc").findList();
		return list;
	}
	
	
	/**
	 * Returns all processes stored by the given user, the node 
	 * and the trigger. This is used to get the appropriate HOOK 
	 * process, when its event is generated by the node application 
	 * (e.g. New File Created trigger for Box application)
	 */
	public static List<Process> getProcessesForTrigger(ServiceAccessTokenKey key, String operationId) {
		List<Process> processList = find.where()
				.eq("user_id", key.getUserId())
				.eq("trigger_node", key.getNodeId())
				.or(Expr.isNull("paused"), Expr.ne("paused", true)).findList();
		
		// list carrying all the process that do match the criteria
		List<Process> removalList = new ArrayList<Process>();
		
		for (Process process : processList) {
			JsonNode processData = Json.parse(process.getProcessData());
			if(processData.isArray()) {
				JsonNode json = processData.get(0);
				String id = json.path("data").get("id").getTextValue();
				if(!id.equalsIgnoreCase(operationId)) 
					removalList.add(process);
			}
		}

		// remove all the process that did not match the criteria
		processList.removeAll(removalList);
		return processList;
	}
	
	
	public Boolean isUserOwner(String userId) {
		if(userId.equalsIgnoreCase(this.userId))
			return true;
		else
			return false;
	}
	

	@Override
	public void save() {
		try {
			Process token = getProcess(processId);
			setVersion(Play.application().configuration().getString("process.version"));
			if(token==null)
				super.save();
			else
				super.update();
			
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}
	
	
}
