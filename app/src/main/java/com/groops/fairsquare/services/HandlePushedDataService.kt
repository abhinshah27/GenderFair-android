/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.groops.fairsquare.models.HLPosts
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.LogUtils
import io.realm.Realm
import org.json.JSONArray

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class HandlePushedDataService : BaseHandlePushedBlockService(HandlePushedDataService::class.qualifiedName ?: "") {

    companion object {

        var mHandler: Handler? = null

        @JvmStatic
        fun startService(context: Context, handler: Handler, jsonString: String) {
            try {
                mHandler = handler
                context.startService(
                        Intent(context, HandlePushedDataService::class.java).apply { putExtra(Constants.EXTRA_PARAM_1, jsonString) }
                )
            } catch (e: IllegalStateException) {
                LogUtils.e(HandleChatsUpdateService.LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }


    override fun getType(): Type {
        return Type.DATA
    }

    override fun getHandler(): Handler? {
        return mHandler
    }

    override fun getDeleteOp(jsonArray: JSONArray?, realm: Realm?) {
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()

            for (i in 0 until jsonArray.length()) {
                posts.deletePost(jsonArray.optString(i), realm, true)
            }
        }
    }

    override fun getInsertOp(jsonArray: JSONArray?, realm: Realm?) {
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()
            posts.setPosts(jsonArray, realm, true)
        }
    }

    override fun getUpdateOp(jsonArray: JSONArray?, realm: Realm?) {
        getInsertOp(jsonArray, realm)
    }

    override fun getUpdateHeartsOp(jsonArray: JSONArray?, realm: Realm?) {
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.optJSONObject(i)
                if (json != null && json.length() > 0 && json.has("userID") && json.has("totHearts")) {
                    val uid = json.optString("userID")
                    val hearts = json.optInt("totHearts")
                    posts.updateAuthorHeartsForAllPosts(uid, hearts, -1)
                }
            }
        }
    }

}
