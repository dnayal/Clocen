package models;

/**
 * Class (not storing data) for managing id-name pairs.
 * Used by callInfoService() methods of nodes, which 
 * map their data to this class and then convert it into
 * JSON before sending it to the calling client
 */
public class IdName {

	private String id;
	private String name;
	
	public IdName() {}

	public IdName(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
