package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.Utils
import kotlinx.android.synthetic.main.fragment_onboarding_plain_text.*

class OnBoardingTextFragment: HLFragment() {

    private var refStep = 0

    companion object {

        val LOG_TAG = OnBoardingTextFragment::class.qualifiedName

        const val END_POINT = "http://www.genderfair.com/"
        const val WEB_TITLE = "Gender Fair"

        fun newInstance(step: Int): OnBoardingTextFragment {
            val args = Bundle()
            args.putInt(Constants.EXTRA_PARAM_1, step)

            return OnBoardingTextFragment().also { it.arguments = args }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_onboarding_plain_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        val pair: Pair<Int, Boolean> = if (refStep == 6) {
            (mainAction as Button).setText(R.string.onboarding_button_6)
            R.string.onboarding_text_6 to true
        }
        else {
            when (refStep) {
                0 -> R.string.onboarding_text_0 to false
                3 -> R.string.onboarding_text_3 to false
                5 -> R.string.onboarding_text_5 to false
                7 -> R.string.onboarding_text_7 to false
                10 -> R.string.onboarding_text_10 to false
                else -> 0 to false
            }
        }

        with(text) {
            text = Utils.getFormattedHtml(resources, pair.first)

            if (refStep == 3 || refStep == 10) {
                (layoutParams as? LinearLayout.LayoutParams)?.let {
                    it.marginStart += Utils.dpToPx(50f, resources)
                    it.marginEnd = it.marginStart

                    if (refStep == 10)
                        it.topMargin = Utils.dpToPx(100f, resources)
                }
            }
        }

        mainAction.visibility = if (pair.second) View.VISIBLE else View.GONE
        (mainAction as Button).isAllCaps = false
    }


    override fun onResume() {
        super.onResume()

        if (refStep == 10) onBoardingActivityListener.callSignup()
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        refStep = savedInstanceState?.getInt(Constants.EXTRA_PARAM_1) ?: 0
    }

    override fun configureResponseReceiver() {}

    override fun configureLayout(view: View) {
        mainAction.setOnClickListener {
            if (refStep == 6) {
                Utils.fireBrowserIntent(mainAction.context, END_POINT, WEB_TITLE)
            }
        }
    }

    override fun setLayout() {}
}