package models;

import helpers.UtilityHelper;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import play.data.validation.Constraints.Email;
import play.db.ebean.Model;

@SuppressWarnings("serial")
@Entity
public class BetaUser extends Model {

	private static final String COMPONENT_NAME = "BetaUser Model";

	@Id
	@Email
	@Column(length=100, unique=true)
	private String email;
	private Boolean inviteEmailSent;
	private Boolean registered;
	private Date createTimestamp;
	private static Finder<String, BetaUser> find = new Finder<String, BetaUser>(String.class, BetaUser.class);
	
	
	public BetaUser(String email, Date createTimestamp, Boolean inviteEmailSent, Boolean registered) {
		this.email = email;
		this.createTimestamp = createTimestamp;
		this.inviteEmailSent = inviteEmailSent;
		this.registered = registered;
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
	
	
	public Boolean isInviteEmailSent() {
		return inviteEmailSent;
	}
	
	
	public void setInviteEmailSent(Boolean inviteEmailSent) {
		this.inviteEmailSent = inviteEmailSent;
	}
	
	
	public Boolean isRegistered() {
		return registered;
	}
	
	
	public void setRegistered(Boolean registered) {
		this.registered = registered;
	}
	
	
	@Override
	public void save() {
		BetaUser bu = find.byId(email);
		try{
			if(bu == null)
				super.save();
			else
				super.update();
				
		} catch (PersistenceException exception) {
			UtilityHelper.logError(COMPONENT_NAME, "save()", exception.getMessage(), exception);
		}
	}
	
	
	public static BetaUser getBetaUser(String email) {
		return find.byId(email);
	}
	
	
	public static List<BetaUser> getAllBetaUsers() {
		return find.orderBy("createTimestamp desc").findList();
	}
	

	@Override
	public String toString() {
		return "BetaUser [email=" + email + ", inviteEmailSent="
				+ inviteEmailSent + ", registered=" + registered
				+ ", createTimestamp=" + createTimestamp + "]";
	}
	
}
