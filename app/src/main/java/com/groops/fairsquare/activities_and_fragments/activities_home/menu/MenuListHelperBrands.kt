package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.content.Context
import android.os.Message
import com.groops.fairsquare.R
import com.groops.fairsquare.adapters.InterestBrandDiffCallback
import com.groops.fairsquare.adapters.InterestBrandsAdapterDiff
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.models.InterestBrand
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.websocket_connection.HLServerCalls
import org.json.JSONArray
import org.json.JSONException

class MenuListHelperBrands(private val fragment: MenuContract.MenuBrandsView, context: Context):
        MenuListHelper(fragment, context) {

    companion object {
        val LOG_TAG = MenuListHelperBrands::class.qualifiedName
    }


    private var loadedData = mutableListOf<InterestBrand>()


    override fun init() {
        super.init()

        adapter = InterestBrandsAdapterDiff(InterestBrandDiffCallback(), fragment)
    }

    override fun getNoResultString(): Int {
        return R.string.no_result_brands
    }


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        if (operationId == Constants.SERVER_OP_GET_BRANDS) {
            setData(responseObject)
        }
    }


    override fun runCallBlockForServer(): Array<Any>? {
        var results: Array<Any>? = null

        try {
            results = HLServerCalls.getCompaniesOrBrands(menuView?.getItemID(), true)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return results
    }

    override fun customPopulateActions(jsonResponse: JSONArray?) {
        loadedData.clear()
        if (jsonResponse != null && jsonResponse.length() > 0) {
            for (i in 0 until jsonResponse.length()) {
                val brand = InterestBrand.get(jsonResponse.optJSONObject(i))
                loadedData.add(brand)
            }

            // INFO: 2019-05-07    server sorts elements
//            loadedData.sortWith(ScoreComparator)
        }
    }


    override fun customPopulateHandling(msg: Message?) {
        if (loadedData.isNotEmpty())
            (adapter as? InterestBrandsAdapterDiff)?.submitList(loadedData)
        else {
            (contextRef.get() as? HLActivity)?.runOnUiThread {
                menuView?.handleRVVisibility(false)
            }
        }
    }
}