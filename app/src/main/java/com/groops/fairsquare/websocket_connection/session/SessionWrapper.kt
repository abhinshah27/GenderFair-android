package com.groops.fairsquare.websocket_connection.session

import com.groops.fairsquare.base.HLModel
import com.groops.fairsquare.models.Session
import com.groops.fairsquare.utility.realm.RealmUtils
import io.realm.Realm

object SessionWrapper: HLModel() {

    val LOG_TAG = SessionWrapper::class.qualifiedName

    var sessionID: String? = null
        get() {
            return if (!field.isNullOrBlank()) field
            else {
                val currentSession = read() as? Session
                field = currentSession?.sessionID
                field
            }
        }


    @JvmStatic fun updateSessionID(newSessionID: String?) {
        sessionID = newSessionID
        Session.updateSessionID(newSessionID)

        if (isValid()) SessionHandler.checkPendingAndRetry()
    }

    @JvmStatic fun isValid() = !sessionID.isNullOrBlank()

    /**
     * This method is only used during login when first [Session] instance has not been pushed to
     * Realm yet.
     */
    @JvmStatic fun saveNewSession(realm: Realm?, sessionID: String?) {
        if (RealmUtils.isValid(realm)) {
            this.sessionID = sessionID

            RealmUtils.deleteTable(realm, Session::class.java)
            val session = Session(SessionWrapper.sessionID)
            realm!!.insertOrUpdate(session)
        }
    }

    @JvmStatic fun resetSession() = reset()


    override fun reset() {
        super.reset()
        updateSessionID(null)
    }

    override fun read(): Any? {
        return Session.get()
    }


}