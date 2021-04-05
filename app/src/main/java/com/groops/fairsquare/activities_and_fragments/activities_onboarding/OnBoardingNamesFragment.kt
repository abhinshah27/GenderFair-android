package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLFragment
import kotlinx.android.synthetic.main.component_onboarding_edittext.view.*
import kotlinx.android.synthetic.main.fragment_onboarding_names.*

class OnBoardingNamesFragment: HLFragment() {

    companion object {

        val LOG_TAG = OnBoardingNamesFragment::class.qualifiedName

        fun newInstance(): OnBoardingNamesFragment {
            return OnBoardingNamesFragment()
        }
    }


    private val firstNameWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                onBoardingActivityListener.setFirstName(s.toString())
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    private val lastNameWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                onBoardingActivityListener.setLastName(s.toString())
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_names, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }

    override fun onStart() {
        super.onStart()

        setLayout()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {}

    override fun configureResponseReceiver() {}

    override fun configureLayout(view: View) {

        with(firstName.field as EditText) {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setImeActionLabel(getString(R.string.action_next), EditorInfo.IME_ACTION_NEXT)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setHint(R.string.prompt_first_name)
            nextFocusDownId = lastName.field.id

//            this.setOnEditorActionListener { _, actionId: Int, _ ->
//                if (actionId == EditorInfo.IME_ACTION_NEXT)
//                    this.next
//            }
        }

        with(lastName.field as EditText) {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setImeActionLabel(getString(R.string.done), EditorInfo.IME_ACTION_DONE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setHint(R.string.prompt_last_name)
//            this.setOnEditorActionListener { _, actionId: Int, _ ->
//                if (actionId == EditorInfo.IME_ACTION_NEXT)
//                    this.next
//            }
        }

    }

    override fun setLayout() {
        val names = onBoardingActivityListener.getNames()

        with(firstName.field) {
            removeTextChangedListener(firstNameWatcher)
            setText(names.first)
            addTextChangedListener(firstNameWatcher)
        }
        with(lastName.field) {
            removeTextChangedListener(lastNameWatcher)
            setText(names.second)
            addTextChangedListener(lastNameWatcher)
        }
    }
}