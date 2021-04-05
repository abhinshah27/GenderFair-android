package com.groops.fairsquare.websocket_connection

import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.models.Session
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.websocket_connection.session.SessionWrapper
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference


class Request(val id: String, private var body: String?, val isChat: Boolean = false, caller: HLActivity) {
    private val caller: WeakReference<HLActivity> = WeakReference(caller)

    val isValid: Boolean
        get() = Utils.isContextValid(caller.get()) && !body.isNullOrBlank()

    val correctBody: String?
        get() {
            if (!body.isNullOrBlank()) {
                try {
                    val jBody = JSONObject(body)
                    val event = jBody.getJSONObject("event")

                    if (Session.get(null) != null) {
                        event.put("sessionID", SessionWrapper.sessionID)
                        jBody.put("event", event)

                        body = jBody.toString()
                        return body
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            return null
        }

}