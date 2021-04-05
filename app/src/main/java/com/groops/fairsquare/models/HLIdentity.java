/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.groops.fairsquare.R;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.services.GetTimelineService;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.utility.helpers.JsonHelper;
import com.groops.fairsquare.utility.realm.RealmUtils;
import com.groops.fairsquare.websocket_connection.HLSocketConnection;
import com.groops.fairsquare.websocket_connection.HLWebSocketAdapter;

import org.json.JSONObject;

import java.io.Serializable;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * @author mbaldrighi on 1/26/2018.
 */
@RealmClass
public class HLIdentity implements RealmModel, RealmModelListener, Serializable,
		JsonHelper.JsonDeSerializer, Comparable<HLIdentity> {

	@SerializedName(value = "userID")
	@PrimaryKey private String id;
	private String name;
	private String firstName;
	private String lastName;
	private String avatarURL;
	@SerializedName("wallImageLink")
	private String wallPictureURL;

	private boolean isInterest;

	@SerializedName(value = "idInterest")
	private String idDBObject;

	private boolean isNonProfit;

	private boolean hasActiveGiveSupportInitiative;

	private int totHeartsAvailable;


	public HLIdentity() {}


	@Override
	public int hashCode() {
		if (Utils.isStringValid(getId()))
			return getId().hashCode();
		else
			return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof HLIdentity && Utils.areStringsValid(getId(), ((HLIdentity) obj).getId()) &&
				getId().equals(((HLIdentity) obj).getId());
	}

	@Override
	public int compareTo(@NonNull HLIdentity o) {
		if (Utils.areStringsValid(this.getName(), o.getName()))
			return this.getName().compareTo(o.getName());

		return 0;
	}


	public HLIdentity deserializeToClass(JSONObject json) {
		return (HLIdentity) deserialize(json, HLIdentity.class);
	}


	public static void switchIdentity(Context context, final HLIdentity object, HLWebSocketAdapter.ConnectionObserver observer) {

		// progress needs to be closed directly in the listeners of the subscription
		if (context instanceof HLActivity) {
			((HLActivity) context).setProgressMessage(R.string.switching_profile);
			((HLActivity) context).handleProgress(true);
		}

		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			realm.executeTransaction(realm1 -> {
				HLUser mUser = new HLUser().readUser(realm1);

				mUser.setSelectedIdentityId(mUser.getUserId().equals(object.getId()) ? null : object.getId());
				mUser.setSelectedIdentity(mUser.getUserId().equals(object.getId()) ? null : object);

				if (mUser.getSelectedFeedFilters() == null)
					mUser.setSelectedFeedFilters(new RealmList<>());
				else
					mUser.getSelectedFeedFilters().clear();
			});

			HLApp.resetPaginationIds();
			HLPosts.getInstance().resetCollectionsForSwitch(realm);

			HLNotifications.getInstance().resetCollectionsForSwitch();
			HLApp.identityChanged = true;

			HLApp.subscribedToSocket = false;
			HLApp.subscribedToSocketChat = false;

			HLSocketConnection.getInstance().attachSubscriptionObservers(observer);
			HLSocketConnection.getInstance().subscribeSockets(context);
		}
		catch (Exception e) { e.printStackTrace(); }
		finally { RealmUtils.closeRealm(realm); }
	}

	public static void performOpsAfterReconnection(Context context, boolean isChat) {
		if (Utils.isContextValid(context)) {
			if (isChat) {
//				HandleChatsUpdateService.startService(context);
//				ChatSetUserOnlineService.startService(context);
			} else {
				GetTimelineService.startService(context, null);
//				GetConfigurationDataService.startService(context);
			}
		}
	}


	//region == Serialization methods ==

	@Override
	public JsonElement serializeWithExpose() {
		return JsonHelper.serializeWithExpose(this);
	}

	@Override
	public String serializeToStringWithExpose() {
		return JsonHelper.serializeToStringWithExpose(this);
	}

	@Override
	public JsonElement serialize() {
		return JsonHelper.serialize(this);
	}

	@Override
	public String serializeToString() {
		return JsonHelper.serializeToString(this);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JSONObject json, Class myClass) {
		return JsonHelper.deserialize(json, myClass);
	}

	@Override
	public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
		return null;
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


	//region == Getters and setters

	@Override
	public void reset() {}

	@Override
	public Object read(@Nullable Realm realm) {
		return null;
	}

	@Override
	public RealmModel read(Realm realm, Class<? extends RealmModel> model) {
		return null;
	}

	@Override
	public void deserializeStringListFromRealm() {}

	@Override
	public void serializeStringListForRealm() {}

	@Override
	public void write(@Nullable Realm realm) {}

	@Override
	public void write(Object object) {}

	@Override
	public void write(JSONObject json) {}

	@Override
	public void write(Realm realm, RealmModel model) {}

	@Override
	public void update() {}

	@Override
	public void update(Object object) {}

	@Override
	public void update(JSONObject json) {}

	@Override
	public RealmModelListener updateWithReturn() {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(Object object) {
		return null;
	}

	@Override
	public RealmModelListener updateWithReturn(JSONObject json) {
		return null;
	}

	//endregion


	//region == Getters and setters

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

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAvatarURL() {
		return avatarURL;
	}
	public void setAvatarURL(String avatarURL) {
		this.avatarURL = avatarURL;
	}

	public String getWallPictureURL() {
		return wallPictureURL;
	}
	public void setWallPictureURL(String wallPictureURL) {
		this.wallPictureURL = wallPictureURL;
	}

	public boolean isInterest() {
		return isInterest;
	}
	public void setInterest(boolean interest) {
		isInterest = interest;
	}

	public String getIdDBObject() {
		return idDBObject;
	}
	public void setIdDBObject(String idDBObject) {
		this.idDBObject = idDBObject;
	}

	public boolean isNonProfit() {
		return isNonProfit;
	}
	public void setNonProfit(boolean nonProfit) {
		isNonProfit = nonProfit;
	}

	public boolean hasActiveGiveSupportInitiative() {
		return hasActiveGiveSupportInitiative;
	}
	public void setHasActiveGiveSupportInitiative(boolean hasActiveGiveSupportInitiative) {
		this.hasActiveGiveSupportInitiative = hasActiveGiveSupportInitiative;
	}

	public int getTotHeartsAvailable() {
		return totHeartsAvailable;
	}
	public void setTotHeartsAvailable(int totHeartsAvailable) {
		this.totHeartsAvailable = totHeartsAvailable;
	}

	//endregion

}
