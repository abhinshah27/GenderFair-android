package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLFragment
import kotlinx.android.synthetic.main.fragment_onboarding_checkboxes.*

class OnBoardingCheckboxesFragment: HLFragment() {

    companion object {

        val LOG_TAG = OnBoardingCheckboxesFragment::class.qualifiedName

        fun newInstance(): OnBoardingCheckboxesFragment {
            return OnBoardingCheckboxesFragment()
        }
    }


    private val checkedWatcher = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        onBoardingActivityListener.setCheckBoxes(
                when (buttonView.id) {
                    R.id.checkBox -> 0
                    R.id.checkBox1 -> 1
                    R.id.checkBox2 -> 2
                    R.id.checkBox3 -> 3
                    else -> -1
                },
                isChecked
        )
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_checkboxes, container, false)
    }

    override fun onStart() {
        super.onStart()

        setLayout()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {}
    override fun configureResponseReceiver() {}
    override fun configureLayout(view: View) {}

    override fun setLayout() {
        val allCB = onBoardingActivityListener.getCheckBoxes()

        with(checkBox) {
            setOnCheckedChangeListener(null)
            isChecked = allCB[0]
            setOnCheckedChangeListener(checkedWatcher)
        }
        with(checkBox1) {
            setOnCheckedChangeListener(null)
            isChecked = allCB[1]
            setOnCheckedChangeListener(checkedWatcher)
        }
        with(checkBox2) {
            setOnCheckedChangeListener(null)
            isChecked = allCB[2]
            setOnCheckedChangeListener(checkedWatcher)
        }
        with(checkBox3) {
            setOnCheckedChangeListener(null)
            isChecked = allCB[3]
            setOnCheckedChangeListener(checkedWatcher)
        }
    }
}