/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.services

import android.app.IntentService
import android.content.Intent
import android.os.Handler
import android.os.Message
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.realm.RealmUtils
import io.realm.Realm
import org.json.JSONArray

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
abstract class BaseHandlePushedBlockService(name: String) : IntentService(name) {

    enum class Type(val value: Int) { DATA(0), CHANNELS(1) }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val jsonArray =
                if (intent.hasExtra(Constants.EXTRA_PARAM_1)) JSONArray(intent.getStringExtra(Constants.EXTRA_PARAM_1))
                else JSONArray()

        if (jsonArray.length() > 0) {
            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()

                val jsonObject = jsonArray.optJSONObject(0)
                if (jsonObject != null && jsonObject.length() > 0) {
                    var hasInsert = false
                    for (it in jsonObject.keys()) {
                        val newArray = jsonObject.optJSONArray(it)
                        when (it) {
                            "arrayDelete" -> getDeleteOp(newArray, realm)
                            "arrayInsert" -> {
                                hasInsert = newArray.length() > 0
                                getInsertOp(newArray, realm)
                            }
                            "arrayUpdate" -> getUpdateOp(newArray, realm)
                            "arrayUpdateHearts" -> getUpdateHeartsOp(newArray, null)
                        }
                    }

                    getHandler()?.sendMessage(Message.obtain(getHandler(), getType().value, if (hasInsert) 1 else 0))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                realm?.close()
            }
        }
    }

    abstract fun getType(): Type

    abstract fun getHandler(): Handler?

    abstract fun getDeleteOp(jsonArray: JSONArray?, realm: Realm?)
    abstract fun getInsertOp(jsonArray: JSONArray?, realm: Realm?)
    abstract fun getUpdateOp(jsonArray: JSONArray?, realm: Realm?)
    abstract fun getUpdateHeartsOp(jsonArray: JSONArray?, realm: Realm?)

}
