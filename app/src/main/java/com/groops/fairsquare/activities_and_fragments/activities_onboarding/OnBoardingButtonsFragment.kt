package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.base.OnApplicationContextNeeded
import com.groops.fairsquare.models.OnBoardingButtonElement
import com.groops.fairsquare.utility.ComputeAndPopulateHandlerThread
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.websocket_connection.HLRequestTracker
import com.groops.fairsquare.websocket_connection.HLServerCalls
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver
import kotlinx.android.synthetic.main.fragment_onboarding_buttons.*
import kotlinx.android.synthetic.main.fragment_onboarding_plain_text.text
import org.json.JSONArray

class OnBoardingButtonsFragment: HLFragment(), OnMissingConnectionListener,
        OnBoardingButtonsAdapter.OnButtonClickListener {

    companion object {

        val LOG_TAG = OnBoardingButtonsFragment::class.qualifiedName

        fun newInstance(step: Int): OnBoardingButtonsFragment {
            val args = Bundle()
            args.putInt(Constants.EXTRA_PARAM_1, step)

            return OnBoardingButtonsFragment().also { it.arguments = args }
        }
    }


    enum class ButtonType { CHANNELS, INTERESTS }


    private var refStep = 0

    private var buttonsType: ButtonType? = null
    private lateinit var buttonsPair: Pair<Int, ButtonType?>

    private var callChannels: () -> Unit = {
        Utils.setRefreshingForSwipeLayout(srl, true)
        val results = HLServerCalls.callButtons(ButtonType.CHANNELS)
        HLRequestTracker
                .getInstance(context?.applicationContext as OnApplicationContextNeeded)
                .handleCallResult(this, (context as? HLActivity), results)
    }

    private var callInterests: () -> Unit = {
        Utils.setRefreshingForSwipeLayout(srl, true)
        val results = HLServerCalls.callButtons(ButtonType.INTERESTS)
        HLRequestTracker
                .getInstance(context?.applicationContext as OnApplicationContextNeeded)
                .handleCallResult(this, (context as? HLActivity), results)
    }

    private val items = mutableListOf<OnBoardingButtonElement>()

    private lateinit var adapter: OnBoardingButtonsAdapter

    private lateinit var srl: SwipeRefreshLayout


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_onboarding_buttons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        configureResponseReceiver()

        text.text = Utils.getFormattedHtml(resources, buttonsPair.first)

    }

    override fun onResume() {
        super.onResume()

        when (buttonsType) {
            ButtonType.CHANNELS -> callChannels.invoke()
            ButtonType.INTERESTS -> callInterests.invoke()
            else -> activityListener.showGenericError()
        }
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        refStep = savedInstanceState?.getInt(Constants.EXTRA_PARAM_1) ?: 0

        buttonsPair = when (refStep) {
            1 -> R.string.onboarding_title_1 to ButtonType.CHANNELS
            8 -> R.string.onboarding_title_8 to ButtonType.INTERESTS
            else -> 0 to null
        }.also { buttonsType = it.second }
    }

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        if (operationId == Constants.SERVER_OP_ONBOARDING_GET_CHANNELS ||
                operationId == Constants.SERVER_OP_ONBOARDING_GET_INTERESTS) {
            setData(responseObject)
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)
        Utils.setRefreshingForSwipeLayout(srl, false)
    }

    override fun onMissingConnection(operationId: Int) {
        Utils.setRefreshingForSwipeLayout(srl, false)
    }


    override fun configureLayout(view: View) {
        adapter = OnBoardingButtonsAdapter(items, this, buttonsType!!)
        buttonsList.adapter = adapter
        buttonsList.layoutManager = LinearLayoutManager(view.context)

        srl = Utils.getGenericSwipeLayout(view) {
            when (buttonsType) {
                ButtonType.CHANNELS -> callChannels.invoke()
                ButtonType.INTERESTS -> callInterests.invoke()
                else -> activityListener.showGenericError()
            }
        }

    }

    override fun setLayout() {}


    private fun setData(jsonResponse: JSONArray?) {
        if (jsonResponse == null || jsonResponse.length() == 0) {
            buttonsList.visibility = View.GONE
            noResult.visibility = View.VISIBLE
        }

        buttonsList.visibility = View.VISIBLE
        noResult.visibility = View.GONE

        PopulateButtonsHandler(jsonResponse).start()
    }


    override fun onButtonSelected(elementID: String) {
        if (buttonsType == ButtonType.CHANNELS) onBoardingActivityListener.addRemoveSelectedChannel(elementID, false)
        else onBoardingActivityListener.addRemoveSelectedInterest(elementID, false)
    }

    override fun onButtonDeselected(elementID: String) {
        if (buttonsType == ButtonType.CHANNELS) onBoardingActivityListener.addRemoveSelectedChannel(elementID, true)
        else onBoardingActivityListener.addRemoveSelectedInterest(elementID, true)
    }


    inner class PopulateButtonsHandler(jsonResponse: JSONArray?):
            ComputeAndPopulateHandlerThread("populateOnBoardingButtons", jsonResponse) {

        override fun customActions(jsonResponse: JSONArray?) {
            items.clear()
            if (jsonResponse != null && jsonResponse.length() > 0) {
                for (i in 0 until jsonResponse.length()) {
                    val button = OnBoardingButtonElement.get(jsonResponse.optJSONObject(i))
                    if (!button.id.isNullOrBlank()) {
                        button.selected =
                                if (buttonsType == ButtonType.CHANNELS) onBoardingActivityListener.isChannelSelected(button.id!!)
                                else onBoardingActivityListener.isInterestSelected(button.id!!)

                        items.add(button)
                    }
                }
            }
        }

        override fun customHandling(msg: Message?): Boolean {
            (context as? HLActivity)?.runOnUiThread {

                if (items.isNotEmpty())
                    adapter.notifyDataSetChanged()
                else {
                    buttonsList.visibility = View.GONE
                    noResult.visibility = View.VISIBLE
                }

                Utils.setRefreshingForSwipeLayout(srl, false)
            }
            return true
        }
    }

}