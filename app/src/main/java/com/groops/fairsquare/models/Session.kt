package com.groops.fairsquare.models

import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.realm.RealmUtils
import com.groops.fairsquare.websocket_connection.session.SessionHandler
import io.realm.Realm
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONObject

@RealmClass
open class Session(var sessionID: String? = null): RealmModel, RealmModelListener {

    /**
     * Only one Session is admitted in Realm >> "final" de facto var with value 0
     */
    @PrimaryKey var id = 0


    companion object {

        /**
         * Depending on whether @param realm is null or not, this method is used to get an unmanaged
         * copy of the current session object or its managed version.
         *
         * @param realm the provided [Realm] instance if the managed version is wanted
         * @return The unmanaged copy of [Session] if a [Realm] is not provided, or a managed version
         * otherwise.
         */
        @JvmStatic fun get(realm: Realm? = null): Session? {
            return if (realm == null) {
                // returns unmanaged copy
                var currentSession: Session? = null
                var realm1: Realm? = null
                try {
                    realm1 = RealmUtils.getCheckedRealm()
                    val managed = RealmUtils.readFirstFromRealm(realm1, Session::class.java) as? Session
                    currentSession = if (managed != null) realm1.copyFromRealm(managed) else null
                }
                catch (e: Exception) { e.printStackTrace() }
                finally { RealmUtils.closeRealm(realm1) }
                currentSession
            } else {
                // returns managed version
                RealmUtils.readFirstFromRealm(realm, Session::class.java) as? Session
            }
        }


//        @JvmStatic fun isValid(): Boolean {
//            var valid = false
//            var realm: Realm? = null
//            try {
//                realm = RealmUtils.getCheckedRealm()
//                valid = !(RealmUtils.readFirstFromRealm(realm, Session::class.java) as? Session)?.sessionID.isNullOrBlank()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                RealmUtils.closeRealm(realm)
//            }
//
//            return valid
//        }

        /**
         * This method replaces the current [Session.sessionID].
         *
         * @param newSessionID       the provided new token.
         * @param alreadyTransaction whether the operation is called from an ongoing transaction or not.
         */
        @JvmStatic internal fun updateSessionID(newSessionID: String? = null, alreadyTransaction: Boolean = false) {

            SessionHandler.renewalProgressing = false

            LogUtils.d("RENEW_SESSION", "Handler IS RENEWING: ${SessionHandler.renewalProgressing}")

            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                val current = RealmUtils.readFirstFromRealm(realm, Session::class.java) as? Session

                LogUtils.d("CRYPTO", "KEY UPDATED: ${current?.sessionID} to $newSessionID")
                LogUtils.d("RENEW_SESSION", "KEY UPDATED: ${current?.sessionID} to $newSessionID")

                if (alreadyTransaction) current?.sessionID = newSessionID
                else realm.executeTransaction { current?.sessionID = newSessionID }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                RealmUtils.closeRealm(realm)
            }

        }

    }


    fun isValid(): Boolean {
        return !sessionID.isNullOrBlank()
    }


    override fun reset() {
        updateSessionID(null)
    }

    override fun read(realm: Realm?): Any? {
        return null
    }

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? {
        return null
    }

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {}

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {}

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {}

    override fun update(json: JSONObject?) {}

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener? {
        return null
    }
}