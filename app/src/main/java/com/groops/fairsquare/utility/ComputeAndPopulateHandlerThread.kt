/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import org.json.JSONArray

/**
 * @author mbaldrighi on 12/3/2018.
 */
abstract class ComputeAndPopulateHandlerThread(name: String,
                                           private val jsonResponse: JSONArray?
): HandlerThread(name), Handler.Callback {

    var handler: Handler? = null

    /**
     * Place where the [Handler] for the class is instantiated.
     */
    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = if (Utils.hasPie())
            Handler.createAsync(looper, this)
        else
            Handler(looper, this)

        if (handler != null) {
            handler!!.post {
                customActions(jsonResponse)
            }

            handler?.sendEmptyMessage(0)
            quitSafely()
        }
    }

    override fun handleMessage(msg: Message?): Boolean {
        var returnValue = false

        Handler(Looper.getMainLooper()).post {
            returnValue = customHandling(msg)
        }

        return returnValue
    }

    abstract fun customHandling(msg: Message?): Boolean

    abstract fun customActions(jsonResponse: JSONArray?)

}