package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class User extends Model {

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
	
}
