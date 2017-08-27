package com.coinotifier.data.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.coinotifier.data.model.ProfileModel;

@Service
public class ProfileDao extends JPADao<ProfileModel>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4449948398327588584L;

	public List<ProfileModel> getProfilesByUserID(long userId)
	{
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("userId", userId);
		
		return this.findByNamedQuery("Profile.findByUserID", params);
	}
	
}
