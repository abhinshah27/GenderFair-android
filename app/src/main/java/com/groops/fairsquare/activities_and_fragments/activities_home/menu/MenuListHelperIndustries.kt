package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.content.Context
import android.os.Message
import com.groops.fairsquare.R
import com.groops.fairsquare.adapters.MenuIndustriesAdapterDiff
import com.groops.fairsquare.adapters.MenuLandingDiffCallback
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.models.MenuIndustry
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.websocket_connection.HLServerCalls
import org.json.JSONArray
import org.json.JSONException

class MenuListHelperIndustries(private val fragment: MenuContract.MenuIndustriesView, context: Context):
        MenuListHelper(fragment, context) {

    companion object {
        val LOG_TAG = MenuListHelperIndustries::class.qualifiedName
    }


    private var loadedData = mutableListOf<MenuIndustry>()

    override fun init() {
        super.init()

        adapter = MenuIndustriesAdapterDiff(MenuLandingDiffCallback(), fragment)
    }

    override fun getNoResultString(): Int {
        return R.string.no_result_landing
    }


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        if (operationId == Constants.SERVER_OP_GET_INDUSTRIES) {
            setData(responseObject)
        }
    }


    override fun runCallBlockForServer(): Array<Any>? {
        var results: Array<Any>? = null

        try {
            results = HLServerCalls.getMenuIndustries()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return results
    }

    override fun customPopulateActions(jsonResponse: JSONArray?) {
        loadedData.clear()
        if (jsonResponse != null && jsonResponse.length() > 0) {
            for (i in 0 until jsonResponse.length()) {
                val item = MenuIndustry.get(jsonResponse.optJSONObject(i))
                loadedData.add(item)
            }
            loadedData.sort()
        }
    }


    override fun customPopulateHandling(msg: Message?) {
        if (loadedData.isNotEmpty())
            (adapter as? MenuIndustriesAdapterDiff)?.submitList(loadedData)
        else {
            (contextRef.get() as? HLActivity)?.runOnUiThread {
                menuView?.handleRVVisibility(false)
            }
        }
    }
}