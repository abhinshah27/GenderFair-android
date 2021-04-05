package com.groops.fairsquare.websocket_connection.session

import com.groops.fairsquare.base.HLApp
import com.groops.fairsquare.base.OnApplicationContextNeeded
import com.groops.fairsquare.models.HLUser
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.realm.RealmUtils
import com.groops.fairsquare.websocket_connection.HLServerCalls
import com.groops.fairsquare.websocket_connection.Request
import io.realm.Realm
import org.json.JSONException
import java.util.concurrent.ConcurrentHashMap

object SessionHandler {

    val LOG_TAG = SessionHandler::class.qualifiedName

    internal var renewalProgressing = false
    var logoutProgressing = false

    private val pendingRequests = ConcurrentHashMap<String, Request>()

    @Synchronized
    fun isSessionAvailable(
            context: OnApplicationContextNeeded,
            errorCode: Int,
            errorDescription: String?,
            request: Request?
    ): Boolean {

        /*
         * Realm logic is put before because we are in thread different from UIThread:
         * userId -> close Realm instance -> reuse obtained String
         */
        var userId: String? = null
        val realm = RealmUtils.getCheckedRealm()
        executeRealmQuery(realm) {
            userId = HLUser().readUser(realm).id
        }

        var isSessionAvailable = true

        if (isSessionExpired(errorCode)) {

            LogUtils.d("RENEW_SESSION", "Handler: SESSION EXPIRED")
            LogUtils.d("RENEW_SESSION", "Handler IS RENEWING: $renewalProgressing")

            isSessionAvailable = false

            if (request != null) pendingRequests[request.id] = request
            if (!renewalProgressing) {
                renewalProgressing = true
                val sessionId = SessionWrapper.sessionID
                LogUtils.d("CRYPTO", "SESSION EXPIRED: $sessionId")
                renewSession(userId, sessionId)
            }
        }

        if (isForceLogout(errorCode)) {

            isSessionAvailable = false

            if (!logoutProgressing) {
                logoutProgressing = true
                LogUtils.d("CRYPTO", "FORCING LOGOUT")
                forceLogout(context, userId, errorDescription)
            }
        }

        return isSessionAvailable
    }


    @Synchronized
    fun checkPendingAndRetry() {
        LogUtils.d(LOG_TAG, "Inside checkPending... method FOR RENEW")

        try {
            for (entry in pendingRequests) {
                if (entry.value.isValid) {
                    if (entry.value.isChat)
                        HLApp.getSocketConnection().sendMessageChat(entry.value.correctBody!!)
                    else
                        HLApp.getSocketConnection().sendMessage(entry.value.correctBody!!)
                }

                pendingRequests.remove(entry.key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(LOG_TAG, e.message, e)
        }

    }

    @Synchronized
    fun clearPending() {
        pendingRequests.clear()
    }


    private fun isSessionExpired(errorCode: Int): Boolean {
        return errorCode == Constants.SERVER_ERROR_SESSSION_EXPIRED
    }

    private fun isForceLogout(errorCode: Int) = errorCode == Constants.SERVER_ERROR_FORCE_LOGOUT

    private fun renewSession(userId: String?, sessionId: String?) {
        val serverRequest = execute { HLServerCalls.renewSession(userId, sessionId) }
        SessionSubject.setSessionState(SessionState.EXPIRED, null, serverRequest)
    }

    private fun forceLogout(context: OnApplicationContextNeeded, userId: String?, errorDescription: String?) {
        val serverRequest = execute { HLServerCalls.logout(context.hlContext, userId) }
        SessionSubject.setSessionState(SessionState.FORCE_LOGOUT, errorDescription, serverRequest)
    }

    private fun execute(serverCall: () -> Array<Any>) = try {
        serverCall.invoke()
    } catch (e: JSONException) {
        e.printStackTrace()
        emptyArray<Any>()
    }

    private fun executeRealmQuery(realm: Realm, action: () -> Unit) {
        try {
            action.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            RealmUtils.closeRealm(realm)
        }
    }

}