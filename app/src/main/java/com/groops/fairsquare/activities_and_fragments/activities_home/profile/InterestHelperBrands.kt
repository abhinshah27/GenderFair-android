package com.groops.fairsquare.activities_and_fragments.activities_home.profile

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Message
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.R
import com.groops.fairsquare.adapters.InterestBrandDiffCallback
import com.groops.fairsquare.adapters.InterestBrandsAdapterDiff
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLApp
import com.groops.fairsquare.models.InterestBrand
import com.groops.fairsquare.utility.ComputeAndPopulateHandlerThread
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.helpers.BaseHelper
import com.groops.fairsquare.websocket_connection.*
import org.json.JSONArray
import org.json.JSONException

class InterestHelperBrands(context: Context, private val contractView: InterestProfileContract.InterestProfileView?):
        BaseHelper(context),
        OnServerMessageReceivedListener, OnMissingConnectionListener,                                                          // interfaces
        InterestProfileContract.InterestProfilePresenter {                                                                      // interfaces

    companion object {
        val LOG_TAG = InterestHelperBrands::class.qualifiedName
    }

    private var messageReceiver: ServerMessageReceiver? = null

    var loadedData = mutableListOf<InterestBrand>()
    private var adapter: InterestBrandsAdapterDiff? = null


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        contractView?.handleSRL(false)

        if (operationId == Constants.SERVER_OP_GET_BRANDS) {
            setData(responseObject)
        }

    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        contractView?.handleSRL(false)

        if (operationId == Constants.SERVER_OP_GET_BRANDS) {
            (contextRef.get() as? HLActivity)?.showGenericError()
        }
    }

    override fun onMissingConnection(operationId: Int) {
        contractView?.handleSRL(false)
    }


    override fun callServer() {
        contractView?.handleSRL(true)

        var results: Array<Any>? = null

        try {
            results = HLServerCalls.getCompaniesOrBrands(contractView?.getCompanyId(), false)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (contextRef.get() is HLActivity) {
            HLRequestTracker.getInstance((contextRef.get() as Activity).application as HLApp)
                    .handleCallResult(this, contextRef.get() as HLActivity, results, false, false)
        }
    }

    private fun setData(responseObject: JSONArray? = null) {
        val resultValid = responseObject != null && responseObject.length() != 0

        contractView?.handleRVVisibility(resultValid)

        if (!resultValid) return
        else PopulateBrandsHandler(responseObject).start()
    }


    override fun init() {
        adapter = InterestBrandsAdapterDiff(InterestBrandDiffCallback(), contractView)

        messageReceiver = null
        messageReceiver = ServerMessageReceiver().also { it.setListener(this@InterestHelperBrands) }

        contractView?.init()
    }

    override fun getRVAdapter(): RecyclerView.Adapter<*>? {
        return adapter
    }

    override fun getNoResultString(): Int {
        return R.string.no_result_brands
    }

    override fun handleServerReceiver(register: Boolean) {
        if (register)
            registerReceiver(messageReceiver, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))
        else
            unregisterReceiver(messageReceiver)
    }

    inner class PopulateBrandsHandler(jsonResponse: JSONArray?):
            ComputeAndPopulateHandlerThread("populateBrands", jsonResponse) {

        override fun customActions(jsonResponse: JSONArray?) {
            loadedData.clear()
            if (jsonResponse != null && jsonResponse.length() > 0) {
                for (i in 0 until jsonResponse.length()) {
                    val brand = InterestBrand.get(jsonResponse.optJSONObject(i))
                    loadedData.add(brand)
                }
                loadedData.sort()
            }
        }

        override fun customHandling(msg: Message?): Boolean {
            if (loadedData.isNotEmpty())
                adapter?.submitList(loadedData)
            else {
                (contextRef.get() as? HLActivity)?.runOnUiThread {
                    contractView?.handleRVVisibility(false)
                }
            }
            return true
        }
    }
}