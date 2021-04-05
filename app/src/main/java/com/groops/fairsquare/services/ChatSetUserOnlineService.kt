/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.groops.fairsquare.models.HLUser
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.realm.RealmUtils
import com.groops.fairsquare.websocket_connection.HLServerCallsChat
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver
import io.realm.Realm
import org.json.JSONArray
import org.json.JSONException

/**
 * [Service] subclass whose duty is to send user online notification for CHAT.
 * @author mbaldrighi on 6/11/2019.
 */
class ChatSetUserOnlineService : Service(), OnServerMessageReceivedListener {

    companion object {
        val LOG_TAG = ChatSetUserOnlineService::class.qualifiedName

        @JvmStatic
        fun startService(context: Context) {
            try {
                context.startService(Intent(context, ChatSetUserOnlineService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    private lateinit var receiver: ServerMessageReceiver


    override fun onCreate() {
        super.onCreate()
        receiver = ServerMessageReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiver.setListener(this)
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_SOCKET_SUBSCRIPTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))

        var realm: Realm? = null
        try {
            realm = RealmUtils.getCheckedRealm()
            val user = HLUser().readUser(realm)
            if (user != null && user.isValid) {
                callSetUserOnline(user.userId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        } finally {
            RealmUtils.closeRealm(realm)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun callSetUserOnline(id: String) {
        var results: Array<Any?>? = null
        try {
            results = HLServerCallsChat.setUserOnline(id)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (results != null && !(results[0] as Boolean)) {
            exitOps()
        }
    }

    //region == Receiver Callback ==

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray) {
        when (operationId) {
            Constants.SERVER_OP_CHAT_SET_USER_ONLINE -> {
                LogUtils.d(LOG_TAG, "CHAT SET USER ONLINE SUCCESS")
                exitOps()
            }

        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {

        when (operationId) {
            Constants.SERVER_OP_CHAT_SET_USER_ONLINE -> {
                LogUtils.e(LOG_TAG, "CHAT SET USER ONLINE FAILED")
                exitOps()
            }
        }
    }

    //endregion


    private fun exitOps(stop: Boolean = true) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            LogUtils.d(LOG_TAG, e.message)
        }
        if (stop) stopSelf()
    }

}
