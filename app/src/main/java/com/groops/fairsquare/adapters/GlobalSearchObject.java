/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.adapters;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.groops.fairsquare.R;
import com.groops.fairsquare.models.InterestCategory;
import com.groops.fairsquare.models.PostList;
import com.groops.fairsquare.models.UsersBundle;
import com.groops.fairsquare.models.enums.GlobalSearchTypeEnum;
import com.groops.fairsquare.models.enums.GlobalSearchUITypeEnum;
import com.groops.fairsquare.utility.helpers.JsonHelper;

import org.json.JSONObject;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public class GlobalSearchObject implements Comparable<GlobalSearchObject>, JsonHelper.JsonDeSerializer {

	private Object mainObject;
	private int sortOrder;
	private String returnType;
	private GlobalSearchTypeEnum objectType;
	private GlobalSearchUITypeEnum uiType;

	/**
	 * @since 4/16/2019: needed to tell the {@link GlobalSearchListsAdapter} to show a {@link android.widget.Button}.
	 */
	private boolean isButton = false;


	public GlobalSearchObject() {}

	public GlobalSearchObject(String returnType, String type, String uiType) {
		this.returnType = returnType;
		this.objectType = GlobalSearchTypeEnum.toEnum(type);
		this.uiType = GlobalSearchUITypeEnum.toEnum(uiType);
	}

	public GlobalSearchObject(Object mainObject, String returnType, String uiType) {
		if (mainObject instanceof PostList) {
			this.mainObject = mainObject;
			this.sortOrder = ((PostList) mainObject).getSortOrder();
		}
		else if (mainObject instanceof InterestCategory) {
			this.mainObject = mainObject;
			this.sortOrder = ((InterestCategory) mainObject).getSortOrder();
		}
		else if (mainObject instanceof UsersBundle) {
			this.mainObject = mainObject;
			this.sortOrder = ((UsersBundle) mainObject).getSortOrder();
		}

		this.objectType = GlobalSearchTypeEnum.toEnum(returnType);
		this.uiType = GlobalSearchUITypeEnum.toEnum(uiType);
	}

	@Override
	public int compareTo(@NonNull GlobalSearchObject o) {
		return Integer.compare(sortOrder, o.sortOrder);
	}

	@Override
	public int hashCode() {
		return String.valueOf(sortOrder).hashCode();
	}

	public GlobalSearchObject deserializeToClass(JSONObject json) {
		return (GlobalSearchObject) deserialize(json, GlobalSearchObject.class);
	}

	@LayoutRes
	public int getSingleItemLayoutRes() {
		if (mainObject instanceof PostList)
			return R.layout.item_diary_text;
		if (mainObject instanceof InterestCategory)
			return R.layout.item_interest_global_search;
		if (mainObject instanceof UsersBundle)
			return R.layout.item_interest_global_search_user;

		throw new IllegalStateException("Invalid mainObject");
	}

	public boolean isUsers() {
		return objectType == GlobalSearchTypeEnum.USERS;
	}

	public boolean isInterests() {
		return objectType == GlobalSearchTypeEnum.INTERESTS ||
				objectType == GlobalSearchTypeEnum.COMPANIES ||
				objectType == GlobalSearchTypeEnum.BRANDS ||
				objectType == GlobalSearchTypeEnum.ORGANIZATIONS ||
				objectType == GlobalSearchTypeEnum.CHANNELS;
	}

	public boolean isPosts() {
		return objectType == GlobalSearchTypeEnum.STORIES;
//				returnType == GlobalSearchTypeEnum.P_MY ||
//				returnType == GlobalSearchTypeEnum.P_MY_FEED ||
//				returnType == GlobalSearchTypeEnum.P_PUBLIC;
	}


	//region == Serialization and Deserialization interface ==

	@Override
	public JsonElement serializeWithExpose() {
		return null;
	}

	@Override
	public String serializeToStringWithExpose() {
		return null;
	}

	@Override
	public JsonElement serialize() {
		return null;
	}

	@Override
	public String serializeToString() {
		return null;
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JSONObject json, Class myClass) {
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
		return JsonHelper.deserialize(jsonString, myClass);
	}

	@Override
	public Object getSelfObject() {
		return this;
	}

	//endregion


	//region == Getters and setters ==

	public Object getMainObject() {
		return mainObject;
	}
	public void setMainObject(Object mainObject) {

		if (mainObject instanceof PostList) {
			this.mainObject = mainObject;
			this.sortOrder = ((PostList) mainObject).getSortOrder();
		}
		else if (mainObject instanceof InterestCategory) {
			this.mainObject = mainObject;
			this.sortOrder = ((InterestCategory) mainObject).getSortOrder();
		}
		else if (mainObject instanceof UsersBundle) {
			this.mainObject = mainObject;
			this.sortOrder = ((UsersBundle) mainObject).getSortOrder();
		}
	}

	public int getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getReturnType() {
		return returnType;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public GlobalSearchTypeEnum getObjectType() {
		return objectType;
	}
	public void setObjectType(GlobalSearchTypeEnum objectType) {
		this.objectType = objectType;
	}

	public GlobalSearchUITypeEnum getUIType() {
		return uiType;
	}
	public void setUIType(GlobalSearchUITypeEnum UIType) {
		this.uiType = UIType;
	}

	public boolean isButton() {
		return isButton;
	}
	public void setButton(boolean button) {
		isButton = button;
	}

	//endregion

}
