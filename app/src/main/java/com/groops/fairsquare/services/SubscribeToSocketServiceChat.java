/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.models.HLUser;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.realm.RealmUtils;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListenerWithIdOperation;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;

import java.util.Objects;

import io.realm.Realm;

/**
 * {@link Service} subclass whose duty is to subscribe client to Real-Time communication with socket.
 *
 * @author mbaldrighi on 11/01/2017.
 */
public class SubscribeToSocketServiceChat extends Service implements OnServerMessageReceivedListenerWithIdOperation {

    public static final String LOG_TAG = SubscribeToSocketServiceChat.class.getCanonicalName();
    public static final int SUCCESS_CHAT = 1;

    private ServerMessageReceiver receiver;

    private String idOperation = null;

    private static Handler successHandler;


    public static void startService(Context context, @Nullable Handler handler) {
        LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket CHAT: startService()");
        try {
            successHandler = handler;
            context.startService(new Intent(context, SubscribeToSocketServiceChat.class));
        } catch (IllegalStateException e) {
            LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (receiver == null)
            receiver = new ServerMessageReceiver();
        receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (receiver == null)
            receiver = new ServerMessageReceiver();
        receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));

        Realm realm = null;
        try {
            realm = RealmUtils.getCheckedRealm();
            HLUser user = new HLUser().readUser(realm);
            if (user != null && user.isValid()) {
                String userId = user.getUserId();
                callSubscription(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
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

    private void callSubscription(@NonNull String id) {
        Object[] results = HLServerCalls.subscribeToSocket(this, id, true);

        if (results.length == 3)
            idOperation = (String) results[2];

        LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket CHAT - idOperation: " + idOperation);

        if (!((boolean) results[0])) {
            HLApp.subscribedToSocketChat = false;
            stopSelf();
        }
    }


    //region == Receiver Callback ==

    @Override
    public void handleSuccessResponse(String operationUUID, int operationId, JSONArray responseObject) {
        switch (operationId) {
            case Constants.SERVER_OP_SOCKET_SUBSCR:
                if (Objects.equals(idOperation, operationUUID)) {

                    if (successHandler != null)
                        successHandler.sendEmptyMessage(SUCCESS_CHAT);

                    HLApp.subscribedToSocketChat = true;

                    LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket CHAT SUCCESS");

                    stopSelf();
                }
                break;
        }
    }

    @Override
    public void handleSuccessResponse(int operationId, JSONArray responseObject) {
//        switch (operationId) {
//            case Constants.SERVER_OP_SOCKET_SUBSCR:
//                HLApp.subscribedToSocketChat = true;
//
//                LogUtils.i(LOG_TAG, "HLApp.subscribedToSocketChat = " + HLApp.subscribedToSocketChat);
//                LogUtils.i(LOG_TAG, "HLApp.subscribedToSocket = " + HLApp.subscribedToSocket);
//
//                LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket CHAT SUCCESS");
//
//                if (Utils.isStringValid(userId)) callSetUserOnline(userId);
//                break;
//        }
    }

    @Override
    public void handleErrorResponse(int operationId, int errorCode) {

        HLApp.subscribedToSocketChat = false;

        switch (operationId) {
            case Constants.SERVER_OP_SOCKET_SUBSCR:
                LogUtils.e(LOG_TAG, "SUBSCRIPTION to socket CHAT FAILED");
                stopSelf();
                break;
        }
    }

    //endregion

}
