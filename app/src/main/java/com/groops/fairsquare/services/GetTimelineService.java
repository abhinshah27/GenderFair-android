/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.groops.fairsquare.models.HLPosts;
import com.groops.fairsquare.models.HLUser;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.realm.RealmUtils;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;
import org.json.JSONException;

import io.realm.Realm;

/**
 * {@link Service} subclass whose duty is to call for the timeline posts in a background thread,
 * before the actual display of the results.
 * This should prevent the {@link com.groops.fairsquare.activities_and_fragments.activities_home.timeline.TimelineFragment}
 * UI to glitch at
 * @author mbaldrighi on 11/01/2017.
 */
public class GetTimelineService extends Service implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = GetTimelineService.class.getCanonicalName();

	private ServerMessageReceiver receiver;
	private static Handler mHandler;


	public static void startService(Context context, Handler handler) {
		try {
			mHandler = handler;
			context.startService(new Intent(context, GetTimelineService.class));
		} catch (IllegalStateException e) {
			LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
		}
	}


	@Override
	public void onCreate() {
		super.onCreate();

		receiver = new ServerMessageReceiver();
	}

	@Override
	public void onDestroy() {
		try {
//			unregisterReceiver(receiver);
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			LogUtils.d(LOG_TAG, e.getMessage());
		}

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// initializes Receiver if it's null
		if (receiver == null)
			receiver = new ServerMessageReceiver();
		receiver.setListener(this);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));

		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			HLUser user = new HLUser().readUser(realm);
			callFilteredPosts(user, realm);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			RealmUtils.closeRealm(realm);
		}

		return Service.START_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void callFilteredPosts(HLUser user, Realm realm) {
		Object[] results = null;
		try {
			results = HLServerCalls.getTimeline(user.getId(), user.getCompleteName(), user.getAvatarURL(),
					user.getSettingSortOrder(), 0, user.getSelectedFeedFilters(), user.isActingAsInterest());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (results == null || !((boolean) results[0])) {
			try {
//				unregisterReceiver(receiver);
				LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			} catch (IllegalArgumentException e) {
				LogUtils.d(LOG_TAG, e.getMessage());
			}
		}
	}


	//region == Receiver Callback ==

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {

		switch (operationId) {
			case Constants.SERVER_OP_GET_TIMELINE:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				Realm realm = null;
				try {
					realm = RealmUtils.getCheckedRealm();

					HLPosts instance = HLPosts.getInstance();
					try {
						instance.cleanRealmPostsNewSession(realm);
						instance.setPosts(responseObject, realm, true);
					}
					catch (JSONException e) {
						LogUtils.e(LOG_TAG, e.getMessage(), e);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					RealmUtils.closeRealm(realm);
				}

				if (mHandler != null) {
					mHandler.sendEmptyMessage(0);
					mHandler = null;
				}
				stopSelf();
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {

		switch (operationId) {
			case Constants.SERVER_OP_GET_TIMELINE:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				LogUtils.e(LOG_TAG, "SERVER ERROR with error: " + errorCode);
				stopSelf();
				break;
		}
	}

	//endregion

}
