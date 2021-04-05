/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_login;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;

import com.groops.fairsquare.R;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.OnApplicationContextNeeded;
import com.groops.fairsquare.utility.AnalyticsUtils;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.FieldValidityWatcher;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.websocket_connection.HLRequestTracker;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListenerWithErrorDescription;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * A screen for password reset.
 *
 * @author mbaldrighi on 4/20/2017
 */
public class ResetPasswordActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListenerWithErrorDescription, OnMissingConnectionListener {

	public static final String LOG_TAG = ResetPasswordActivity.class.getCanonicalName();

	// UI references.
	private EditText mEmailView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reset_password);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		mEmailView = findViewById(R.id.reset_pwd_email);
		mEmailView.addTextChangedListener(new FieldValidityWatcher(R.id.reset_pwd_email, mEmailView));

		findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
			}
		});

		findViewById(R.id.main_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				attemptPasswordRecovery();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.LOGIN_RESET_PWD);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Utils.closeKeyboard(mEmailView);
	}

	@Override
	public void onClick(View v) {}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void manageIntent() {}


	private void attemptPasswordRecovery() {
		String email = mEmailView.getText().toString();

		if (!Utils.isStringValid(email)) {
			showAlert(getString(R.string.error_recovery_email_empty));
			return;
		}
		else if (!Utils.isEmailValid(email)) {
			showAlert(R.string.error_invalid_email);
			return;
		}

		try {
			Object[] results = HLServerCalls.recoverPassword(email);
			HLRequestTracker.getInstance((OnApplicationContextNeeded) getApplication())
					.handleCallResult(this, this, results);
		}
		catch (JSONException e) {
			LogUtils.e(LOG_TAG, e.getMessage());
			e.printStackTrace();
			showGenericError();
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		closeProgress();

		switch (operationId) {
			case Constants.SERVER_OP_PWD_RECOVERY:
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						Utils.fireOpenEmailIntent(ResetPasswordActivity.this);
					}
				}, 1000);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {}

	@Override
	public void handleErrorResponse(int operationId, int errorCode, String description) {
		closeProgress();

		String message = getString(R.string.error_unknown);
		if (errorCode == Constants.SERVER_ERROR_GENERIC) {
			showAlert(message);
		}
		else {
			switch (operationId) {
				case Constants.SERVER_OP_PWD_RECOVERY:
					switch (errorCode) {
						case Constants.SERVER_ERROR_RECOVERY_NO_EMAIL:
							message = description;
					}
					showAlert(message);
					break;
			}
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		closeProgress();
	}

}