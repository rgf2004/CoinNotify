package com.coinotifier.engine.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.coinotifier.data.model.UserModel;
import com.coinotifier.engine.profile.Profile;
import com.coinotifier.telegram.handler.ProfileType;
import com.coinotifier.telegram.handler.UserStatus;

public class User implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8844608986694444627L;

	private long userChatID;
	private String apiKey ;
	private String secret;
	private UserStatus status;
	private UserModel userModel;

	private ProfileType cachedProfileType;
	private String cachedApiKey;
	private String cachedSecret;
	private UserStatus cachedStatus;
	
	private List<Profile> profiles = new ArrayList<>();

	
	public User()
	{
		
	}
	
	public User(UserModel userModel)
	{
		userChatID = userModel.getChatID();
		this.userModel = userModel;
	}
	
	public void backupUserStatus()
	{
		this.cachedStatus = this.status;
	}
	
	public void revertUserStatus()
	{
		this.status = this.cachedStatus;
	}
	
	public long getUserChatID() {
		return userChatID;
	}
	public void setUserChatID(long channelID) {
		this.userChatID = channelID;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public ProfileType getCachedProfileType() {
		return cachedProfileType;
	}

	public void setCachedProfileType(ProfileType cachedProfileType) {
		this.cachedProfileType = cachedProfileType;
	}

	public String getCachedApiKey() {
		return cachedApiKey;
	}

	public void setCachedApiKey(String cachedApiKey) {
		this.cachedApiKey = cachedApiKey;
	}

	public String getCachedSecret() {
		return cachedSecret;
	}

	public void setCachedSecret(String cachedSecret) {
		this.cachedSecret = cachedSecret;
	}
	
	public UserModel getUserModel() {
		return userModel;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<Profile> profiles) {
		this.profiles = profiles;
	}
	
}
