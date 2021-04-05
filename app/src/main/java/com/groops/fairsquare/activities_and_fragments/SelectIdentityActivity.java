/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.groops.fairsquare.R;
import com.groops.fairsquare.adapters.BasicAdapterInteractionsListener;
import com.groops.fairsquare.adapters.IdentitiesAdapter;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.models.HLIdentity;
import com.groops.fairsquare.utility.AnalyticsUtils;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.helpers.MediaHelper;
import com.groops.fairsquare.websocket_connection.HLRequestTracker;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.HLWebSocketAdapter;
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import io.realm.RealmList;

/**
 * Identity selection activity.
 */
public class SelectIdentityActivity extends HLActivity implements View.OnClickListener,
		BasicAdapterInteractionsListener, OnServerMessageReceivedListener, OnMissingConnectionListener,
		HLWebSocketAdapter.ConnectionObserver {

	public static final String LOG_TAG = SelectIdentityActivity.class.getCanonicalName();

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private RecyclerView mRecView;
	private List<HLIdentity> mList = new RealmList<>();
	private LinearLayoutManager llm;
	private IdentitiesAdapter mAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_identity);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		mAdapter = new IdentitiesAdapter(mList, this);
		mAdapter.setSelectedIdentityID(mUser.getId());

		mRecView = findViewById(R.id.identities_rv);

		configureResponseReceiver();

		configureToolbar(null, null, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_IDENTITIES_SELECTION);

		toolbarTitle.setText(R.string.title_activity_select_identity);
		MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.getAvatarURL(), profilePicture, mUser.isActingAsInterest());

		callIdentities();

		mRecView.setLayoutManager(llm);
		mRecView.setAdapter(mAdapter);

		setData();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.back_arrow) {
			setResult(RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
		}
	}

	@Override
	public void onItemClick(final Object object) {
		if (!(object instanceof HLIdentity)) return;

		MediaHelper.loadProfilePictureWithPlaceholder(this, ((HLIdentity) object).getAvatarURL(), profilePicture, ((HLIdentity) object).isInterest());
		mAdapter.setSelectedIdentityID(((HLIdentity) object).getId());
		mAdapter.notifyDataSetChanged();

		HLIdentity.switchIdentity(this, (HLIdentity) object, this);
	}

	@Override
	public void onItemClick(Object object, View view) {}


	/*
	 * NO NEED TO OVERRIDE THIS
	 */
	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_GET_IDENTITIES_V2:
				realm.executeTransaction(realm -> {
					mUser.setIdentities(responseObject);
					setData();
				});
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	int count = 0;
	@Override
	public void onConnectionEstablished(boolean isChat) {
		HLIdentity.performOpsAfterReconnection(this, isChat);

		if (++count == 2) {
			setResult(RESULT_OK);
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top);
		}

		handleProgress(false);
	}


	@Override
	protected void manageIntent() {}


	//region == Class custom methods ==


	@Override
	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		toolbar = findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);

		View back = toolbar.findViewById(R.id.back_arrow);
		back.setOnClickListener(this);
		back.setRotation(-90f);
	}

	private void callIdentities() {
		Object[] result = null;

		try {
			result = HLServerCalls.getIdentities(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((HLApp) getApplication())).handleCallResult(this, this, result);
	}

	private void setData() {
		if (mList == null)
			mList = new RealmList<>();
		else
			mList.clear();

		mList.addAll(mUser.getIdentities());

		mAdapter.notifyDataSetChanged();
	}

	//endregion

}

