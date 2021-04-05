/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.utility.Utils
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 10/18/2018.
 */
abstract class BaseHelper(context: Context) {

    val contextRef: WeakReference<Context> by lazy {
        WeakReference<Context>(context)
    }

    open fun onCreate(view: View? = null) {}
    open fun onCreateView(view: View) {}
    open fun onViewCreated(view: View) {}
    open fun onActivityCreated(activity: HLActivity) {}
    open fun onStart() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onDestroy() {}

    open fun onSaveInstanceState(outState: Bundle) {}
    open fun onRestoreInstanceState(savedInstanceState: Bundle?) {}

    open fun onBackPressed() {}

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {}

    protected fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) {
        if (Utils.isContextValid(contextRef.get()) && receiver != null && filter != null)
            LocalBroadcastManager.getInstance(contextRef.get()!!).registerReceiver(receiver, filter)
    }

    protected fun unregisterReceiver(receiver: BroadcastReceiver?) {
        try {
            if (Utils.isContextValid(contextRef.get()) && receiver != null)
                LocalBroadcastManager.getInstance(contextRef.get()!!).unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

}