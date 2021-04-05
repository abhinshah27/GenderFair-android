package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Message
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLApp
import com.groops.fairsquare.utility.ComputeAndPopulateHandlerThread
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.helpers.BaseHelper
import com.groops.fairsquare.websocket_connection.HLRequestTracker
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver
import org.json.JSONArray

abstract class MenuListHelper(protected val menuView: MenuContract.MenuView?, context: Context):
        BaseHelper(context),                                                        // superclass
        OnServerMessageReceivedListener, OnMissingConnectionListener,               // interfaces
        MenuContract.MenuPresenter {                                                // interfaces

    companion object {
        val LOG_TAG = MenuListHelper::class.qualifiedName
    }

    private var messageReceiver: ServerMessageReceiver? = null
    protected var adapter: RecyclerView.Adapter<*>? = null


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        menuView?.handleSRL(false)

        if (
                operationId != Constants.SERVER_OP_GET_INDUSTRIES ||
                operationId != Constants.SERVER_OP_GET_BRANDS ||
                operationId != Constants.SERVER_OP_GET_COMPANIES
        ) return

    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        menuView?.handleSRL(false)
    }

    override fun onMissingConnection(operationId: Int) {
        menuView?.handleSRL(false)
    }


    override fun callServer() {
        menuView?.handleSRL(true)

        val results = runCallBlockForServer()

        if (contextRef.get() is HLActivity) {
            HLRequestTracker.getInstance((contextRef.get() as Activity).application as HLApp)
                    .handleCallResult(this, contextRef.get() as HLActivity, results)
        }
    }

    protected fun setData(responseObject: JSONArray? = null) {
        val resultValid = responseObject != null && responseObject.length() != 0

        menuView?.handleRVVisibility(resultValid)

        if (!resultValid) return
        else PopulateListHandler(responseObject).start()
    }

    override fun init() {
        messageReceiver = null
        messageReceiver = ServerMessageReceiver().also { it.setListener(this@MenuListHelper) }

        menuView?.init()
    }

    override fun getRVAdapter(): RecyclerView.Adapter<*>? {
        return adapter
    }

    override fun handleServerReceiver(register: Boolean) {
        if (register)
            registerReceiver(messageReceiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))
        else
            unregisterReceiver(messageReceiver)
    }

    abstract fun runCallBlockForServer(): Array<Any>?
    abstract fun customPopulateActions(jsonResponse: JSONArray?)
    abstract fun customPopulateHandling(msg: Message?)


    inner class PopulateListHandler(jsonResponse: JSONArray?):
            ComputeAndPopulateHandlerThread("populateMenuList", jsonResponse) {

        override fun customActions(jsonResponse: JSONArray?) {
            customPopulateActions(jsonResponse)
        }

        override fun customHandling(msg: Message?): Boolean {
            customPopulateHandling(msg)
            return true
        }
    }

}