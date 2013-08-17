package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import play.db.ebean.Model;

@Entity
public class ServiceAccessToken extends Model {

	@EmbeddedId
	UserServiceNode userNode;
	
	@Column(length=100)
	String accessToken;
	
	@Column(length=100)
	String refreshToken;

	Date expirationTime;

	public static Finder<UserServiceNode, ServiceAccessToken> find = new Finder<UserServiceNode, ServiceAccessToken>(UserServiceNode.class, ServiceAccessToken.class);

	public ServiceAccessToken() {}
	
	public ServiceAccessToken(UserServiceNode userNode, String accessToken, String refreshToken, Date expirationTime) {
		this.userNode = userNode;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime;
	}

	public UserServiceNode getUserNode() {
		return userNode;
	}

	public void setUserNode(UserServiceNode userNode) {
		this.userNode = userNode;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}

}
