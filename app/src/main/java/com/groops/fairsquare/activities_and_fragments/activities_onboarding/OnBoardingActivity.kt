/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import com.groops.fairsquare.R
import com.groops.fairsquare.base.BasicInteractionListener
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.OnApplicationContextNeeded
import com.groops.fairsquare.base.OnBackPressedListener
import com.groops.fairsquare.services.DeleteTempFileService
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.FragmentsUtils
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.websocket_connection.HLRequestTracker
import com.groops.fairsquare.websocket_connection.HLServerCalls
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener
import kotlinx.android.synthetic.main.activity_generic_no_toolbar_progress_small.*
import kotlinx.android.synthetic.main.activity_onboarding.*
import org.json.JSONArray

/**
 * @author mbaldrighi on 6/14/2019.
 */
class OnBoardingActivity : HLActivity(), BasicInteractionListener, OnBoardingActivityListener,
        OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
        View.OnClickListener {

    internal var backListener: OnBackPressedListener? = null

    private var currentBackStackSize = 0

    private var currentUserID: String? = null
    private var email: String? = null
    private var password: String? = null

    private var currentStep: Int = -1
    private var avatarURL: String? = null
    private var firstName: String = ""
    private var lastName: String = ""
    private var allCheckBoxes = BooleanArray(4)

    private var firstNameChanged = false
    private var lastNameChanged = false
    private var channelsChanged: () -> Boolean = {
        if (tempChannels.size != selectedChannels.size) true
        else {
            var changed = false
            for (it in selectedChannels) {
                changed = !tempChannels.contains(it)
                if (changed) break
            }
            changed
        }
    }
    private var interestsChanged: () -> Boolean = {
        if (tempInterests.size != selectedInterests.size) true
        else {
            var changed = false
            for (it in selectedInterests) {
                changed = !tempInterests.contains(it)
                if (changed) break
            }
            changed
        }
    }

    private val selectedChannels = mutableSetOf<String>()
    private val selectedInterests = mutableSetOf<String>()
    private val tempChannels = mutableSetOf<String>()
    private val tempInterests = mutableSetOf<String>()

    private var callSendElements: (Boolean) -> Unit = { isChannels ->
        setProgressMessage(R.string.contacting_server)
        handleProgress(true)
        val results = HLServerCalls.sendSelectedElements(
                currentUserID,
                if (isChannels) OnBoardingButtonsFragment.ButtonType.CHANNELS else OnBoardingButtonsFragment.ButtonType.INTERESTS,
                if (isChannels) selectedChannels else selectedInterests
        )
        HLRequestTracker
                .getInstance(application as OnApplicationContextNeeded)
                .handleCallResult(this, this, results)
    }

    private var callSendNames: () -> Unit = {
        setProgressMessage(R.string.contacting_server)
        handleProgress(true)
        val results = HLServerCalls.sendUserNames(currentUserID, firstName, lastName)
        HLRequestTracker
                .getInstance(application as OnApplicationContextNeeded)
                .handleCallResult(this, this, results)
    }

    private var callSignup: () -> Unit = {

        signupCalled = true

        setProgressMessage(R.string.contacting_server)
        handleProgress(true)
        val results = HLServerCalls.signUpOnBoarding(this, currentUserID)
        HLRequestTracker
                .getInstance(application as OnApplicationContextNeeded)
                .handleCallResult(this, this, results)
    }
    private var signupCalled = false
    private var signupReceived = false


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        val decorView = window.decorView
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
        decorView.systemUiVisibility = uiOptions
        setImmersiveValue(true)

        genericProgressIndicator.setOnClickListener(this@OnBoardingActivity)
        backArrow.setOnClickListener(this)
        navigationAction.setOnClickListener(this)

        supportFragmentManager.addOnBackStackChangedListener {

            var tag = ""
            currentStep = when {
                (supportFragmentManager.backStackEntryCount > currentBackStackSize) -> {
                    tag = "UP"
                    currentStep + 1
                }
                (supportFragmentManager.backStackEntryCount < currentBackStackSize) -> {
                    tag = "BACK"
                    currentStep - 1
                }
                else -> 0
            }

            if (currentStep == 1) { tempChannels.clear(); tempChannels.addAll(selectedChannels) }
            else if (currentStep == 8) { tempInterests.clear(); tempInterests.addAll(selectedInterests) }

            currentBackStackSize = supportFragmentManager.backStackEntryCount

            LogUtils.d(LOG_TAG, "$tag: currentStep=$currentStep - backStackCount=$currentBackStackSize")

            updateActionButton()
        }

        manageIntent()
    }

    override fun onDestroy() {
        DeleteTempFileService.startService(this)
        super.onDestroy()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(Constants.EXTRA_PARAM_1, email)
        outState.putString(Constants.EXTRA_PARAM_2, password)
        outState.putString(Constants.EXTRA_PARAM_3, currentUserID)

        outState.putInt(Constants.EXTRA_PARAM_4, currentStep)
        outState.putStringArrayList(Constants.EXTRA_PARAM_5, ArrayList(selectedChannels))
        outState.putStringArrayList(Constants.EXTRA_PARAM_6, ArrayList(selectedInterests))
        outState.putString(Constants.EXTRA_PARAM_7, firstName)
        outState.putString(Constants.EXTRA_PARAM_8, lastName)
        outState.putBooleanArray(Constants.EXTRA_PARAM_9, allCheckBoxes)

        outState.putBoolean(Constants.EXTRA_PARAM_10, signupCalled)
        outState.putBoolean(Constants.EXTRA_PARAM_11, signupReceived)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        email = savedInstanceState?.getString(Constants.EXTRA_PARAM_1, "")
        password = savedInstanceState?.getString(Constants.EXTRA_PARAM_2, "")
        currentUserID = savedInstanceState?.getString(Constants.EXTRA_PARAM_3, "")

        currentStep = savedInstanceState?.getInt(Constants.EXTRA_PARAM_4, -1) ?: -1
        selectedChannels.addAll(savedInstanceState?.getStringArrayList(Constants.EXTRA_PARAM_5) ?: ArrayList())
        selectedInterests.addAll(savedInstanceState?.getStringArrayList(Constants.EXTRA_PARAM_6) ?: ArrayList())
        firstName = savedInstanceState?.getString(Constants.EXTRA_PARAM_7, "") ?: ""
        lastName = savedInstanceState?.getString(Constants.EXTRA_PARAM_8, "") ?: ""
        allCheckBoxes = savedInstanceState?.getBooleanArray(Constants.EXTRA_PARAM_9) ?: BooleanArray(4)

        signupCalled = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_10, false) ?: false
        signupReceived = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_11, false) ?: false
    }


    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        handleProgress(false)

        when (operationId) {
            Constants.SERVER_OP_ONBOARDING_STEP2 -> showNamesFragment()
            Constants.SERVER_OP_ONBOARDING_STEP3 -> showTextFragment(3)
            Constants.SERVER_OP_ONBOARDING_STEP4 -> showCheckboxesFragment()

            Constants.SERVER_OP_SIGNUP -> {
                signupReceived = true
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        handleProgress(false)
        when (operationId) {
            Constants.SERVER_OP_ONBOARDING_STEP2,
            Constants.SERVER_OP_ONBOARDING_STEP3,
            Constants.SERVER_OP_ONBOARDING_STEP4,
            Constants.SERVER_OP_SIGNUP -> {
                signupCalled = false
                showAlert(R.string.error_signup)
            }
        }
    }

    override fun onMissingConnection(operationId: Int) {
        signupCalled = false
        handleProgress(false)
    }

    override fun onBackPressed() {
        when {
            backListener != null -> backListener!!.onBackPressed()
            supportFragmentManager.backStackEntryCount == 1 -> finish()
            else -> super.onBackPressed()
        }
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            when (intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
                    Constants.FRAGMENT_INVALID)) {
                Constants.FRAGMENT_ONBOARDING_TEXT ->  {

                    email = intent.extras?.getString(Constants.EXTRA_PARAM_1)
                    password = intent.extras?.getString(Constants.EXTRA_PARAM_2)
                    currentUserID = intent.extras?.getString(Constants.EXTRA_PARAM_3)

                    showTextFragment(0)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        if (v == generic_progress_indicator) {
            // do not do anything
        } else {
            when (v?.id) {
                R.id.backArrow -> onBackPressed()
                R.id.navigationAction -> {
                    when (currentStep) {
                        0 -> showButtonsFragment(1)
                        1 -> {
                            LogUtils.d(LOG_TAG, "$selectedChannels")
                            if (channelsChanged.invoke()) callSendElements.invoke(true)
                            else showNamesFragment()
                        }
                        2 -> {
                            LogUtils.d(LOG_TAG, "firstName=$firstName && lastName=$lastName")
                            if (firstNameChanged or lastNameChanged) { callSendNames.invoke() }
                            else { showTextFragment(3) }

                            firstNameChanged = false
                            lastNameChanged = false
                        }
                        3 -> showProfilePictureFragment()
                        4, 5, 6 -> showTextFragment(currentStep + 1)
                        7 -> showButtonsFragment(8)
                        8 -> {
                            LogUtils.d(LOG_TAG, "$selectedInterests")
                            if (interestsChanged.invoke()) callSendElements.invoke(false)
                            else showCheckboxesFragment()
                        }
                        9 -> showTextFragment(10)
                        10 ->  {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        }
    }


    //region == Data callback section ==

    override fun updateActionButton() {
        navigationAction.setText(
                when (currentStep) {
                    4 -> if (!avatarURL.isNullOrBlank()) R.string.action_next else R.string.action_skip
                    10 -> R.string.action_login
                    else -> R.string.action_next
                }
        )

        // INFO: 2019-06-28    both first and last name are not mandatory
        navigationAction.isEnabled = when(currentStep) {
//            2 -> !firstName.isBlank()
            9 -> allBoxesChecked()
            else -> true
        }
    }

    private fun allBoxesChecked(): Boolean {
        var all = true
        for (it in allCheckBoxes) {
            if (!it) {
                all = false
                break
            }
        }

        return all
    }

    override fun getUserID(): String? {
        return currentUserID
    }

    override fun addRemoveSelectedChannel(id: String, remove: Boolean) {
        selectedChannels.apply {

            LogUtils.d(LOG_TAG, "BEFORE remove=$remove: \t selectedChannels.size() == ${selectedChannels.size}")

            if (remove) this.remove(id)
            else this.add(id)

            LogUtils.d(LOG_TAG, "AFTER remove=$remove: \t selectedChannels.size() == ${selectedChannels.size}")
        }
    }

    override fun addRemoveSelectedInterest(id: String, remove: Boolean) {
        selectedInterests.apply {

            LogUtils.d(LOG_TAG, "BEFORE remove=$remove: \t selectedInterests.size() == ${selectedInterests.size}")

            if (remove) this.remove(id)
            else this.add(id)

            LogUtils.d(LOG_TAG, "BEFORE remove=$remove: \t selectedInterests.size() == ${selectedInterests.size}")
        }
    }

    override fun isChannelSelected(id: String): Boolean {
        return selectedChannels.contains(id)
    }

    override fun isInterestSelected(id: String): Boolean {
        return selectedInterests.contains(id)
    }

    override fun getNames(): Pair<String?, String?> {
        return firstName to lastName
    }
    override fun setFirstName(fName: String) {
        if (fName != firstName) firstNameChanged = true
        firstName = fName
        updateActionButton()
    }
    override fun setLastName(lName: String) {
        if (lName != lastName) lastNameChanged = true
        lastName = lName
    }

    override fun getAvatar(): String? {
        return avatarURL
    }
    override fun setAvatar(url: String) {
        avatarURL = url
        runOnUiThread { updateActionButton() }
    }

    override fun getCheckBoxes(): BooleanArray {
        return allCheckBoxes
    }
    override fun setCheckBoxes(index: Int, value: Boolean) {
        allCheckBoxes[index] = value

        updateActionButton()
    }

    override fun callSignup() {
        if (!signupCalled and !signupReceived)
            callSignup.invoke()
    }

    //endregion



    //region == Fragments section ==

    override fun showTextFragment(step: Int) {
        addFragment(step, Constants.FRAGMENT_ONBOARDING_TEXT)
    }

    override fun showButtonsFragment(step: Int) {
        addFragment(step, fragmentCode = Constants.FRAGMENT_ONBOARDING_BUTTONS)
    }

    override fun showCheckboxesFragment() {
        addFragment(fragmentCode = Constants.FRAGMENT_ONBOARDING_CHECKBOX)
    }

    override fun showProfilePictureFragment() {
        addFragment(fragmentCode = Constants.FRAGMENT_ONBOARDING_PICTURE)
    }

    override fun showNamesFragment() {
        addFragment(fragmentCode = Constants.FRAGMENT_ONBOARDING_NAME)
    }


    private fun addFragment(step: Int? = null, fragmentCode: Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right)

        val pair = when (fragmentCode) {
            Constants.FRAGMENT_ONBOARDING_TEXT -> {
                if (step != null) OnBoardingTextFragment.newInstance(step) to OnBoardingTextFragment.LOG_TAG
                else null to null
            }
            Constants.FRAGMENT_ONBOARDING_BUTTONS -> {
                if (step != null) OnBoardingButtonsFragment.newInstance(step) to OnBoardingButtonsFragment.LOG_TAG
                else null to null
            }
            Constants.FRAGMENT_ONBOARDING_NAME -> OnBoardingNamesFragment.newInstance() to OnBoardingNamesFragment.LOG_TAG
            Constants.FRAGMENT_ONBOARDING_PICTURE -> OnBoardingPictureFragment.newInstance() to OnBoardingPictureFragment.LOG_TAG
            Constants.FRAGMENT_ONBOARDING_CHECKBOX -> OnBoardingCheckboxesFragment.newInstance() to OnBoardingCheckboxesFragment.LOG_TAG
            else -> null to null
        }

        FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, pair.first,
                pair.second, null, Constants.NO_RESULT, pair.second)
        fragmentTransaction.commit()
    }

    companion object {

        val LOG_TAG = OnBoardingActivity::class.qualifiedName

        @JvmStatic fun openOnBoardingLanding(context: Context, name: String, pwd: String, userID: String) {
            FragmentsUtils.openFragment(
                    context,
                    Bundle().apply {
                        this.putString(Constants.EXTRA_PARAM_1, name)
                        this.putString(Constants.EXTRA_PARAM_2, pwd)
                        this.putString(Constants.EXTRA_PARAM_3, userID)
                    },
                    Constants.FRAGMENT_ONBOARDING_TEXT,
                    Constants.RESULT_SIGNUP,
                    OnBoardingActivity::class.java,
                    R.anim.slide_in_right,
                    R.anim.no_animation
            )
        }
    }

    //endregion
}


interface OnBoardingActivityListener {
    fun showTextFragment(step: Int)
    fun showButtonsFragment(step: Int)
    fun showCheckboxesFragment()
    fun showProfilePictureFragment()
    fun showNamesFragment()

    fun updateActionButton()

    fun getUserID(): String?

    fun addRemoveSelectedChannel(id: String, remove: Boolean)
    fun addRemoveSelectedInterest(id: String, remove: Boolean)
    fun isChannelSelected(id: String): Boolean
    fun isInterestSelected(id: String): Boolean

    fun getNames(): Pair<String?, String?>
    fun setFirstName(fName: String)
    fun setLastName(lName: String)

    fun getAvatar(): String?
    fun setAvatar(url: String)

    fun getCheckBoxes(): BooleanArray
    fun setCheckBoxes(index: Int, value: Boolean)

    fun callSignup()
}
