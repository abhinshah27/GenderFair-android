/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.transition.Fade;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import com.groops.fairsquare.R;
import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity;
import com.groops.fairsquare.activities_and_fragments.activities_onboarding.OnBoardingActivity;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.base.OnApplicationContextNeeded;
import com.groops.fairsquare.models.HLUser;
import com.groops.fairsquare.services.FetchingOperationsService;
import com.groops.fairsquare.services.GetTimelineService;
import com.groops.fairsquare.services.SendFCMTokenService;
import com.groops.fairsquare.utility.AnalyticsUtils;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.FieldValidityWatcher;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.SharedPrefsUtils;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.utility.realm.RealmUtils;
import com.groops.fairsquare.websocket_connection.HLRequestTracker;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.HLSocketConnection;
import com.groops.fairsquare.websocket_connection.HLWebSocketAdapter;
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListenerWithErrorDescription;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;
import com.groops.fairsquare.websocket_connection.session.SessionWrapper;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A login screen that offers login via email/password and sign up via first name, last name, email and password.
 * It also displays the CTA to the service documents (ToS, PP, ...) and to the password recovery.
 *
 * @author mbaldrighi on 10/2/2017
 */
public class LoginActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener,
		DatePickerDialog.OnDateSetListener, Handler.Callback, HLWebSocketAdapter.ConnectionObserver {

	public static final String LOG_TAG = LoginActivity.class.getCanonicalName();

	private static final String URI_SECTION_KEY = "section";
	private static final String URI_SECTION_PARAM_RESET = "resetPassword";
	private static final String URI_SECTION_PARAM_CONFIRM = "confirm";

	public enum Gender { MALE, FEMALE, NONE }
	private Gender genderEnum = Gender.NONE;

	// UI references.
	private TextView mTitle;
	private EditText mFirstNameView;
	private EditText mLastNameView;
	private EditText mEmailViewSUp;
	private EditText mPasswordViewSUp;
	private EditText mPasswordViewSUpConfirm;
	private EditText mEmailViewLIn;
	private EditText mPasswordViewLIn;
	private TextView mBirthDate;

	// INFO: 2019-05-07    made optional for signup process
	private Date selectedBDate;

	// INFO: 2019-05-07    checkboxes are HIDDEN statically in layout
	private View chkMale, chkFemale;
	
	private TextView chkMaleTxt, chkFemaleTxt;
	private TextView mSwitchButton, mSwitchText;
	private TextView onboardingMessage;

	private Button mainButton;

	private View mSignUpFormView;
	private View mLoginFormView;

	private boolean signupActive = true;
	private boolean forceLogin = false;
	private String sectionParam = null;

	private String email, password;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_new);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		HLApp.fcmTokenSent = false;
		SendFCMTokenService.startService(this);

		/* SETS ENTER FADE TRANSITION */
		if (Utils.hasLollipop())
			getWindow().setEnterTransition(new Fade());


		/* CLOSE SPLASH ACTIVITY */
		new Handler().postDelayed(() -> sendBroadcast(new Intent(Constants.BROADCAST_CLOSE_SPLASH)), Constants.TIME_UNIT_SECOND);

		selectedBDate = null;

		mTitle = findViewById(R.id.title);

		// Set up the sign up form.
		View firstName = findViewById(R.id.first_name);
		mFirstNameView = firstName.findViewById(R.id.edit_text);
		mFirstNameView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		mFirstNameView.setHint(R.string.prompt_first_name);
		mFirstNameView.addTextChangedListener(new FieldValidityWatcher(R.id.first_name, mFirstNameView));
		View lastName = findViewById(R.id.last_name);
		mLastNameView = lastName.findViewById(R.id.edit_text);
		mLastNameView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		mLastNameView.setHint(R.string.prompt_last_name);
		mLastNameView.addTextChangedListener(new FieldValidityWatcher(R.id.last_name, mLastNameView));

		View emailSU= findViewById(R.id.email_signup);
		mEmailViewSUp = emailSU.findViewById(R.id.edit_text);
		mEmailViewSUp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		mEmailViewSUp.setHint(R.string.prompt_email);
		mEmailViewSUp.addTextChangedListener(new FieldValidityWatcher(R.id.email_signup, mEmailViewSUp));
		View pwdSU = findViewById(R.id.password_signup);
		mPasswordViewSUp = pwdSU.findViewById(R.id.edit_text);
		mPasswordViewSUp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordViewSUp.setHint(R.string.prompt_password);
		mPasswordViewSUp.addTextChangedListener(new FieldValidityWatcher(R.id.password_signup, mPasswordViewSUp));
		mPasswordViewSUp.setOnEditorActionListener(new OnEditorAction(R.id.password_signup));
		mPasswordViewSUp.setTypeface(ResourcesCompat.getFont(this, R.font.raleway));
		mPasswordViewSUp.setTransformationMethod(new PasswordTransformationMethod());
		View pwdSUConfirm = findViewById(R.id.password_signup_confirm);
		mPasswordViewSUpConfirm = pwdSUConfirm.findViewById(R.id.edit_text);
		mPasswordViewSUpConfirm.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mPasswordViewSUpConfirm.setImeActionLabel(getString(R.string.go), EditorInfo.IME_ACTION_DONE);
		mPasswordViewSUpConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordViewSUpConfirm.setHint(R.string.prompt_password_confirm);
		mPasswordViewSUpConfirm.addTextChangedListener(new FieldValidityWatcher(R.id.password_signup_confirm, mPasswordViewSUpConfirm));
		mPasswordViewSUpConfirm.setOnEditorActionListener(new OnEditorAction(R.id.password_signup_confirm));
		mPasswordViewSUpConfirm.setTypeface(ResourcesCompat.getFont(this, R.font.raleway));
		mPasswordViewSUpConfirm.setTransformationMethod(new PasswordTransformationMethod());

		// Set up the login form.
		View emailLI = findViewById(R.id.email_login);
		mEmailViewLIn = emailLI.findViewById(R.id.edit_text);
		mEmailViewLIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		mEmailViewLIn.setHint(R.string.prompt_email);
		mEmailViewLIn.addTextChangedListener(new FieldValidityWatcher(R.id.email_login, mEmailViewLIn));
		View pwdLI = findViewById(R.id.password_login);
		mPasswordViewLIn = pwdLI.findViewById(R.id.edit_text);
		mPasswordViewLIn.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mPasswordViewLIn.setImeActionLabel(getString(R.string.go), EditorInfo.IME_ACTION_DONE);
		mPasswordViewLIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordViewLIn.setHint(R.string.prompt_password);
		mPasswordViewLIn.addTextChangedListener(new FieldValidityWatcher(R.id.password_login, mPasswordViewLIn));
		mPasswordViewLIn.setOnEditorActionListener(new OnEditorAction(R.id.password_login));
		mPasswordViewLIn.setTypeface(ResourcesCompat.getFont(this, R.font.raleway));
		mPasswordViewLIn.setTransformationMethod(new PasswordTransformationMethod());

		onboardingMessage = findViewById(R.id.onboarding_message);

		mainButton = findViewById(R.id.main_button);
		mainButton.setOnClickListener(this);

		mLoginFormView = findViewById(R.id.login_form);
		mLoginFormView.setVisibility(View.VISIBLE);
		mSignUpFormView = findViewById(R.id.signup_form);
		mSignUpFormView.setVisibility(View.GONE);

		mBirthDate = findViewById(R.id.birthdate);
		mBirthDate.setActivated(false);

		chkMale = findViewById(R.id.check_male);
		chkFemale = findViewById(R.id.check_female);
		mSwitchButton = findViewById(R.id.switch_btn);
		mSwitchText = findViewById(R.id.switch_text);

		chkMaleTxt = chkMale.findViewById(R.id.text);
		chkFemaleTxt = chkFemale.findViewById(R.id.text);

		mSwitchButton.setOnClickListener(this);
		chkMale.setOnClickListener(this);
		chkFemale.setOnClickListener(this);


		/* CLICKABLESPAN */
		final ClickableSpan recoverySpan = new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
				overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
			}
		};
		TextView mRecovery = findViewById(R.id.password_recovery);
		makeLinks(mRecovery,
				new String[] { getString(R.string.click_recovery) },
				new ClickableSpan[] { recoverySpan }
		);

		ClickableSpan tosSpan = new ClickableSpan() {
			@Override
			public void onClick(View widget) {



				Utils.fireBrowserIntentForDocs(LoginActivity.this, Constants.URL_TOS, getString(R.string.click_agreement_tos), null);
			}
		};
		ClickableSpan privacySpan = new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				Utils.fireBrowserIntentForDocs(LoginActivity.this, Constants.URL_PRIVACY, getString(R.string.click_agreement_privacy), null);
			}
		};
		ClickableSpan cookieSpan = new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				Utils.fireBrowserIntentForDocs(LoginActivity.this, Constants.URL_COOKIES, getString(R.string.click_agreement_cookie), null);
			}
		};
		TextView mAgreement = findViewById(R.id.end_user_agreement);

		// INFO: 2019-06-28    removes clickable span on Cookies
		makeLinks(mAgreement,
				new String[] { getString(R.string.click_agreement_tos),
						getString(R.string.click_agreement_privacy)
//						, getString(R.string.click_agreement_cookie)
				},
				new ClickableSpan[] { tosSpan, privacySpan/*, cookieSpan*/ }
		);

        manageIntent();
	}

	@Override
	protected void onNewIntent(Intent intent) {

		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null) {
				sectionParam = uri.getQueryParameter(URI_SECTION_KEY);
				forceLogin = Utils.isStringValid(sectionParam) && sectionParam.equals(URI_SECTION_PARAM_CONFIRM);
			}
			else if (intent.hasExtra(Constants.EXTRA_PARAM_1))
				forceLogin = intent.getBooleanExtra(Constants.EXTRA_PARAM_1, false);
		}

		super.onNewIntent(intent);
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();

		mTitle.setText(Utils.getFormattedHtml(getResources(), R.string.login_title_2_onboarding));
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.LOGIN_SIGNUP);

		chkMaleTxt.setText(R.string.g_male);
		chkFemaleTxt.setText(R.string.g_female);

		if (forceLogin || !signupActive) {
			clearForms();
			showSignUp(false);
		}
		else showSignUp(true);


		// TODO: 5/7/2018    complete ONBOARDING implementation when ready (and IF)
		onboardingMessage.setVisibility(View.GONE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		Utils.closeKeyboard(mPasswordViewSUp);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Constants.RESULT_SIGNUP && resultCode == RESULT_OK) {

			// before showing the dialog we restore the sign in section, ready to be completed
			forceLogin = true;


			// INFO: 2019-06-22    NO POPUP: modification in last screen for on-boarding already gives info of checking email
//			new MaterialDialog.Builder(this)
//					.content(R.string.signup_email_redirect)
//					.cancelable(true)
//					.positiveText(R.string.yes)
//					.onPositive((dialog1, which) -> {
//						Utils.fireOpenEmailIntent(LoginActivity.this);
//						dialog1.dismiss();
//					})
//					.negativeText(R.string.no)
//					.onNegative((dialog1, which) -> dialog1.dismiss())
//					.show();
		}

	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		switch (id) {
			case R.id.switch_btn:
				showSignUp(!signupActive);
				break;
			case R.id.main_button:
				try {
					attemptRegistration(!signupActive);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			case R.id.birthdate_layout:
				Calendar date = Calendar.getInstance();
				date.setTime(selectedBDate == null ? new Date() : selectedBDate);
				DatePickerDialog dpd = DatePickerDialog.newInstance(
						this,
						date.get(Calendar.YEAR),
						date.get(Calendar.MONTH),
						date.get(Calendar.DAY_OF_MONTH)
				);
				dpd.setVersion(DatePickerDialog.Version.VERSION_2);
				dpd.setAccentColor(Utils.getColor(this, R.color.colorAccent));
				dpd.setTitle(getString(R.string.signup_pick_bdate));
				dpd.setMaxDate(date);

				dpd.show(getSupportFragmentManager(), "DatePickerDialog");
				break;

			case R.id.check_male:
			case R.id.check_female:
				if (v.getId() == R.id.check_male) {
					genderEnum = !chkMale.isSelected() ? Gender.MALE : Gender.NONE;
					chkMale.setSelected(!chkMale.isSelected());
					if (chkFemale.isSelected())
						chkFemale.setSelected(false);
				}
				else if (v.getId() == R.id.check_female) {
					genderEnum = !chkFemale.isSelected() ? Gender.FEMALE : Gender.NONE;
					chkFemale.setSelected(!chkFemale.isSelected());
					if (chkMale.isSelected())
						chkMale.setSelected(false);
				}
				mPasswordViewSUp.requestFocus();
				break;
		}
	}

	@Override
	public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);
		mBirthDate.setText(
				new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(selectedBDate = cal.getTime())
		);
		mBirthDate.setActivated(true);
	}


	@Override
	protected void manageIntent() {

		if (getIntent() != null) {

			// INFO: 2019-05-30    EXTRA_PARAM_1 used for redirection from email
			String errorMessage = getIntent().getStringExtra(Constants.EXTRA_PARAM_2);
			if (Utils.isStringValid(errorMessage))
				showAlert(errorMessage);
		}

	}


	/**
	 * It applies a {@link ClickableSpan} to the provided set of {@link TextView} objects.
	 * @param textView the provided {@link TextView}
	 * @param links the {@link String[]} containing the strings to be matched within the {@link TextView#getText()}.
	 * @param clickableSpans the {@link ClickableSpan} objects corresponding to the links.
	 */
	public void makeLinks(TextView textView, String[] links, ClickableSpan[] clickableSpans) {
		SpannableString spannableString = new SpannableString(textView.getText());
		for (int i = 0; i < links.length; i++) {
			ClickableSpan clickableSpan = clickableSpans[i];
			String link = links[i];

			int startIndexOfLink = textView.getText().toString().indexOf(link);
			spannableString.setSpan(clickableSpan, startIndexOfLink, startIndexOfLink + link.length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText(spannableString, TextView.BufferType.SPANNABLE);
	}


	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptRegistration(boolean login) throws JSONException {
		boolean cancel = false;
		View focusView = null;

		String passwordConfirm = null, fName = null, lName = null;
		@StringRes int error = R.string.error_unknown;

		if (login) {
			// Reset errors.
			mEmailViewLIn.setError(null);
			mPasswordViewLIn.setError(null);

			// Store values at the time of the login attempt.
			email = mEmailViewLIn.getText().toString();
			password = mPasswordViewLIn.getText().toString();
			// Check for a valid password, if the user entered one.
		}
		else {
			// Reset errors.
			mFirstNameView.setError(null);
			mLastNameView.setError(null);
			mEmailViewSUp.setError(null);
			mPasswordViewSUp.setError(null);
			mPasswordViewSUpConfirm.setError(null);

			// Store values at the time of the login attempt.
			fName = mFirstNameView.getText().toString().trim();
			lName = mLastNameView.getText().toString().trim();
			email = mEmailViewSUp.getText().toString();
			password = mPasswordViewSUp.getText().toString();
			passwordConfirm = mPasswordViewSUpConfirm.getText().toString();

			// INFO: 2019-06-17    first and last names HIDDEN
//			if (TextUtils.isEmpty(fName)) {
////				mFirstNameView.setError(getString(R.string.error_field_required));
////				focusView = mFirstNameView;
//				cancel = true;
//				error = R.string.error_all_fields_required;
//			}
//			if (TextUtils.isEmpty(lName)) {
////				mLastNameView.setError(getString(R.string.error_field_required));
////				focusView = mLastNameView;
//				cancel = true;
//				error = R.string.error_all_fields_required;
//			}

			// INFO: 3/22/19    birthdate becomes optional, while gender info is HIDDEN
//			if (selectedBDate == null) {
//				cancel = true;
//				error = R.string.error_signup_dob;
//			}
//
//			if (genderEnum == Gender.NONE) {
//				cancel = true;
//				error = R.string.error_signup_gender;
//			}
		}

		if (login) {
			if (!Utils.isStringValid(password)) {
				cancel = true;
				error = R.string.error_all_fields_required;
			}
		} else {
			if (!Utils.areStringsValid(password, passwordConfirm)) {
				cancel = true;
				error = R.string.error_all_fields_required;
			}
			else if (!Utils.isPasswordValid(password)) {
//				mPasswordViewSUp.setError(getString(R.string.error_invalid_password));
//				focusView = mPasswordViewSUp;
				cancel = true;
				error = R.string.error_invalid_password;
			}
			else if (!Utils.isPasswordValid(passwordConfirm)) {
//				mPasswordViewSUp.setError(getString(R.string.error_invalid_password));
//				focusView = mPasswordViewSUp;
				cancel = true;
				error = R.string.error_invalid_password;
			}
			else if (!passwordConfirm.equals(password)) {
				cancel = true;
				error = R.string.error_not_same_pwd;
			}
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(email)) {
//				mEmailViewSUp.setError(getString(R.string.error_field_required));
//				focusView = mEmailViewSUp;
			cancel = true;
			error = R.string.error_req_fields_signup;
		} else if (!Utils.isEmailValid(email)) {
//				mEmailViewSUp.setError(getString(R.string.error_invalid_email));
//				focusView = mEmailViewSUp;
			cancel = true;
			error = R.string.error_invalid_email;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			if (focusView != null)
				focusView.requestFocus();

			showAlert(error);
		}
		else {
			openProgress();

			if (login) {

				Object[] results;
				results = HLServerCalls.signIn(this, email, password);

				HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
						.handleCallResult(this, this, results);
			}
			else {

				Object[] results = HLServerCalls.validateSignUpEmail(email, password);
				HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
						.handleCallResult(this, this, results);

				// INFO: 2019-06-17    no longer to SignupConfirmActivity
//				Intent intent = new Intent(this, OnBoardingActivity.class);
//				intent.putExtra(Constants.EXTRA_PARAM_1, email);
//				intent.putExtra(Constants.EXTRA_PARAM_2, password);
//				startActivity(intent);
//				overridePendingTransition(R.anim.slide_in_right, R.anim.no_animation);
			}
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showSignUp(final boolean show) {
		int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

		mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		mLoginFormView.animate().setDuration(shortAnimTime).alpha(
				show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			}
		});

		mSignUpFormView.setVisibility(show ? View.VISIBLE : View.GONE);
		mSignUpFormView.animate().setDuration(shortAnimTime).alpha(
				show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mSignUpFormView.setVisibility(show ? View.VISIBLE: View.GONE);
			}
		});

		signupActive = show;
		mainButton.setText(show ? R.string.action_sign_up_2 : R.string.action_login);
		mSwitchText.setText(show ? R.string.login_already_account : R.string.login_no_account);
		mSwitchButton.setText(show ? R.string.action_login : R.string.action_sign_up);
	}

	private void clearForms() {
		String email = mEmailViewSUp.getText().toString();

		mFirstNameView.setText("");
		mLastNameView.setText("");
		mEmailViewSUp.setText("");
		mPasswordViewSUp.setText("");
		mPasswordViewSUpConfirm.setText("");

		mBirthDate.setText(R.string.signup_dob);
		mBirthDate.setActivated(false);

		chkMale.setSelected(false);
		chkFemale.setSelected(false);

		mPasswordViewLIn.setText("");
		if (Utils.isStringValid(email))
			mEmailViewLIn.setText(email);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		@StringRes int message;
		switch (operationId) {
			case Constants.SERVER_OP_VALIDATE_EMAIL:
				closeProgress();

				String userID = responseObject.optJSONObject(0).optString("_id");

				if (Utils.isStringValid(userID))
					OnBoardingActivity.openOnBoardingLanding(this, email, password, userID);
				else
					showGenericError();
				break;
			case Constants.SERVER_OP_SIGNIN_V2:
				if (responseObject.length() == 1) {
					try {
						final JSONObject json = responseObject.getJSONObject(0);

						//retrieve user sessionId
						String sessionID = json.optString("sessionID");
						HLUser user = new HLUser().deserializeToClass(json.toString());
						String id = user.getId();
						LogUtils.d(LOG_TAG, "USER_ID: " + id);

						realm.executeTransaction(realm -> {
							SessionWrapper.saveNewSession(realm, sessionID);

							RealmUtils.deleteTable(realm, HLUser.class);
							RealmUtils.writeToRealmNoTransaction(realm, user);

							// no longer needed but stays here
							SharedPrefsUtils.storeUserTokenId(getApplicationContext(), id);
							SharedPrefsUtils.storeLastPostSeen(getApplicationContext(), "");

							// prepares app for sending new token after login
							HLApp.fcmTokenSent = false;
							HLApp.subscribedToSocket = false;
							HLApp.subscribedToSocketChat = false;
							HLApp.refreshAllowed = true;

							// having the USER in Realm: populate sockets header
							HLSocketConnection.getInstance().updateSocketHeader();
                            HLSocketConnection.getInstance().attachSubscriptionObservers(LoginActivity.this);
						});
					}
					catch (JSONException e) {
						LogUtils.e(LOG_TAG, e.getMessage(), e);
						showGenericError();
						closeProgress();
					}
				} else {
					LogUtils.e(LOG_TAG, "More than one User in response");
					showAlert(R.string.error_generic_operation);
					closeProgress();
				}
				break;

			case Constants.SERVER_OP_SOCKET_SUBSCR:
				LogUtils.e(LOG_TAG, "SUBSCRIPTION to socket SUCCESS");
				break;

			// TODO: 10/6/2017   handle PWD RECOVERY
//			case Constants.SERVER_OP_PWD_RECOVERY:
//				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		closeProgress();

		@StringRes int message = R.string.error_unknown;
		if (errorCode == Constants.SERVER_ERROR_GENERIC) {
			showAlert(message);
		}
		else {
			switch (operationId) {
				case Constants.SERVER_OP_VALIDATE_EMAIL:
				case Constants.SERVER_OP_SIGNIN_V2:
					switch (errorCode) {
						case Constants.SERVER_ERROR_SIGNIN_EMAIL_NOT_CONFIRMED:
							message = R.string.error_email_not_confirmed;
							break;
						case Constants.SERVER_ERROR_SIGNIN_WRONG_USERNAME:
							message = R.string.error_invalid_credentials;
							break;
						case Constants.SERVER_ERROR_SIGNUP_USER_ALREADY_EXISTS:
							message = R.string.error_email_already_in_use;
							break;
						case Constants.SERVER_ERROR_SIGNIN_WRONG_PWD:
							message = R.string.error_signin_wrong_pwd;
							break;
					}
					showAlert(description);

					break;

				case Constants.SERVER_OP_SOCKET_SUBSCR:
					LogUtils.e(LOG_TAG, "SUBSCRIPTION to socket FAILED");
					break;

				// TODO: 10/6/2017   handle PWD RECOVERY
//			case Constants.SERVER_OP_PWD_RECOVERY:
//				break;
			}
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {}

	@Override
	public void onMissingConnection(int operationId) {
		closeProgress();
	}


	@Override
	public boolean handleMessage(Message msg) {
		if (msg != null && msg.what == 0) {
			goToHome();
			return true;
		}
		return false;
	}

	private int count = 0;
	@Override
	public void onConnectionEstablished(boolean isChat) {
		String id = new HLUser().readUser(realm).getId();
		if (Utils.isStringValid(id)) {
//			if (isChat) {
//				/* This is a VALID SESSION: call for the chat updates */
//				HandleChatsUpdateService.startService(getApplicationContext());
//			}
//			else {
				/* This is a VALID SESSION: call for the User's posts */
				GetTimelineService.startService(getApplicationContext(), new Handler(LoginActivity.this));
				/* This is a VALID SESSION: call for the post login operations */
				FetchingOperationsService.startService(getApplicationContext());
//			}
		}

		// count increased when callback is received
		++count;
		goToHome();
	}

	private boolean alreadyTransitioning = false;
	private void goToHome() {
		if (count == 2 && !alreadyTransitioning) {
			alreadyTransitioning = true;

			Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_TIMELINE);
			startActivity(intent);

			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);

			closeProgress();
		}
	}

	private class OnEditorAction implements TextView.OnEditorActionListener {

		private @IdRes int parentId;

		OnEditorAction(@IdRes int parentId) {
			this.parentId = parentId;
		}

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

			if (actionId == EditorInfo.IME_ACTION_DONE) {
				switch (parentId) {
					case R.id.password_login:
					case R.id.password_signup:
						try {
							attemptRegistration(parentId == R.id.password_login);
						} catch (JSONException e) {
							LogUtils.e(LOG_TAG, e.getMessage(), e);
							showGenericError();
						}
						return true;
				}
			}

			return false;
		}
	}

}