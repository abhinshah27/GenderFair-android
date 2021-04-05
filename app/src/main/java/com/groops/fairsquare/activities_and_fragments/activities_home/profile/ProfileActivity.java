/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_home.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.groops.fairsquare.R;
import com.groops.fairsquare.activities_and_fragments.ViewAllTagsActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.notifications.NotificationsFragment;
import com.groops.fairsquare.activities_and_fragments.activities_home.timeline.OnTimelineFragmentInteractionListener;
import com.groops.fairsquare.activities_and_fragments.activities_home.timeline.PostOverlayActionActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.timeline.TimelineFragment;
import com.groops.fairsquare.base.BasicInteractionListener;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.OnBackPressedListener;
import com.groops.fairsquare.models.GenericUserFamilyRels;
import com.groops.fairsquare.models.HLPosts;
import com.groops.fairsquare.models.HLUser;
import com.groops.fairsquare.models.Interest;
import com.groops.fairsquare.models.Post;
import com.groops.fairsquare.models.Tag;
import com.groops.fairsquare.models.enums.SearchTypeEnum;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.FragmentsUtils;
import com.groops.fairsquare.utility.helpers.FullScreenHelper;
import com.groops.fairsquare.utility.helpers.MediaHelper;
import com.groops.fairsquare.utility.helpers.NotificationAndRequestHelper;
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;

import java.util.ArrayList;

import io.realm.RealmModel;

/**
 * @author mbaldrighi on 12/7/2017.
 */
public class ProfileActivity extends HLActivity implements
		View.OnClickListener, OnServerMessageReceivedListener,
		ProfileFragment.OnProfileFragmentInteractionListener, ProfileActivityListener,
		OnMissingConnectionListener, OnTimelineFragmentInteractionListener,
		BasicInteractionListener, NotificationsFragment.OnNotificationsFragmentInteractionListener,
		NotificationAndRequestHelper.OnNotificationHelpListener {

	/**
	 * Serves for TIMELINE pagination purpose.
	 */
	private int lastPageID = 1;
	private Integer lastAdapterPosition = null;

	private FragmentManager fragmentManager = getSupportFragmentManager();

	private MediaHelper mediaHelper;
	private FullScreenHelper fullScreenListener;

	private FullScreenHelper.FullScreenType fullScreenSavedState;
	private FullScreenHelper.RestoreFullScreenStateListener fsStateListener;

	private OnBackPressedListener backListener;

	private NotificationAndRequestHelper notificationHelper;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View rootView;
		setContentView(rootView = LayoutInflater.from(this).inflate(R.layout.activity_profile, null, false));
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		mediaHelper = new MediaHelper();
		fullScreenListener = new FullScreenHelper(this, false);

		notificationHelper = new NotificationAndRequestHelper(this, this);

		manageIntent();
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		HLPosts.getInstance().resetPropertiesForDiaryOrGlobalSearch();
		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {

			// caught when returning from PostOverlayActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS:
				if (fsStateListener != null)
					fsStateListener.restoreFullScreenState(fullScreenSavedState);
				break;


			case Constants.RESULT_SINGLE_POST:
				break;

			case Constants.RESULT_CREATE_POST:
				Intent intent = new Intent(this, HomeActivity.class);
				intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_TIMELINE);
				startActivity(intent);
				finish();
				break;

			case Constants.RESULT_SELECT_IDENTITY:
				UserDetailFragment fragmentUser =
						(UserDetailFragment) fragmentManager.findFragmentByTag(UserDetailFragment.LOG_TAG);
				InterestDetailFragment fragmentInterest =
						(InterestDetailFragment) fragmentManager.findFragmentByTag(InterestDetailFragment.LOG_TAG);
				ProfileFragment fragmentProfile =
						(ProfileFragment) fragmentManager.findFragmentByTag(ProfileFragment.LOG_TAG);

				if (fragmentProfile != null && fragmentProfile.isVisible()) {
					Intent intent1 = new Intent(this, HomeActivity.class);
					intent1.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
					startActivity(intent1);
					finish();
				}
				else {
					switch (mUser.getProfileType()) {
						case ME:
							if (fragmentUser == null && fragmentInterest != null) {
//								showProfileCardFragment(ProfileHelper.ProfileType.INTEREST_ME, null);
								showUserDetailFragment(null);
								fragmentManager.beginTransaction().remove(fragmentInterest).commit();
							}
							break;
						case INTEREST_ME:
							if (fragmentUser != null && fragmentInterest == null) {
//								showProfileCardFragment(ProfileHelper.ProfileType.INTEREST_ME, null);

								RealmModel obj = mUser.getSelectedObject();
								if (obj instanceof Interest) {

									showInterestWebviewFragment(
											InterestProfileFragmentWebview.Type.DETAIL,
											mUser.getId(),
											mUser.getAvatarURL(),
											mUser.getCoverPhotoURL(),
											((Interest) obj).getScore()
									);
									fragmentManager.beginTransaction().remove(fragmentUser).commit();
								}
							}
							break;
					}
				}
		}
	}

	@Override
	public void onBackPressed() {
		if (backListener != null)
			backListener.onBackPressed();
		else if (fragmentManager.getBackStackEntryCount() == 1)
			finish();
		else
			super.onBackPressed();
	}

	//region == ProfileHelper interface ==

	@NonNull
	@Override
	public HLUser getUser() {
		return mUser;
	}

	//endregion


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if(intent == null)
			return;

		int showFragment = intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
				Constants.FRAGMENT_INVALID);
		int requestCode = intent.getIntExtra(Constants.REQUEST_CODE_KEY, Constants.NO_RESULT);
		Bundle extras = intent.getExtras();
		String userId = null;
		String name = null;
		String avatar = null;

		switch (showFragment) {
			case Constants.FRAGMENT_USER_DETAILS:
				addUserDetailFragment(null, requestCode, null, false);
				break;
			case Constants.FRAGMENT_PROFILE_CARD:
				ProfileHelper.ProfileType type = ProfileHelper.ProfileType.NOT_FRIEND;

				int selItem = -1;

				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						type = (ProfileHelper.ProfileType) extras.getSerializable(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						userId = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						selItem = extras.getInt(Constants.EXTRA_PARAM_3);
				}
				addProfileCardFragment(null, requestCode, type, userId, selItem, false);
				break;

			case Constants.FRAGMENT_INNER_CIRCLE:
				userId = null;
				boolean switchToContacts = false;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						userId = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar = extras.getString(Constants.EXTRA_PARAM_3);
					if (extras.containsKey(Constants.EXTRA_PARAM_4))
						switchToContacts = extras.getBoolean(Constants.EXTRA_PARAM_4);
				}
				addInnerCircleFragment(null, requestCode, userId, name, avatar, switchToContacts, true);
				break;

			case Constants.FRAGMENT_INTERESTS:
				userId = null;
				name = null;
				avatar = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						userId = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar = extras.getString(Constants.EXTRA_PARAM_3);
				}
				addInterestsFragment(null, requestCode, userId, name, avatar, true);
				break;

			case Constants.FRAGMENT_DIARY:
				userId = null;
				name = null;
				avatar = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						userId = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar = extras.getString(Constants.EXTRA_PARAM_3);
				}
				addDiaryFragment(null, userId, name, avatar, requestCode, true);
				break;

			case Constants.FRAGMENT_INTEREST_DETAILS:
				addInterestDetailFragment(null, requestCode, null, false);
				break;

			case Constants.FRAGMENT_INTEREST_SIMILAR:
				userId = null;
				name = null;
				avatar = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						userId = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar = extras.getString(Constants.EXTRA_PARAM_3);
				}
				addSimilarForInterestFragment(null, requestCode, userId, name, avatar, true);
				break;

			case Constants.FRAGMENT_INTEREST_FOLLOWERS:
				userId = null;
				name = null;
				avatar = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						userId = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar = extras.getString(Constants.EXTRA_PARAM_3);
				}
				addFollowersFragment(null, requestCode, userId, name, avatar, true);
				break;

			case Constants.FRAGMENT_NOTIFICATIONS:
				addNotificationsFragment(null, requestCode, true);
				break;

			case Constants.FRAGMENT_INTEREST_BRANDS:
				String id1 = null;
				String name1 = null;
				String avatar1 = null;
				String wall = null;
				String score = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						id1 = extras.getString(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						name1 = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar1 = extras.getString(Constants.EXTRA_PARAM_3);
					if (extras.containsKey(Constants.EXTRA_PARAM_4))
						wall = extras.getString(Constants.EXTRA_PARAM_4);
					if (extras.containsKey(Constants.EXTRA_PARAM_5))
						score = extras.getString(Constants.EXTRA_PARAM_5);
				}
				addBrandsFragment(id1, name1, avatar1, wall, score, null, requestCode, true);
				break;

			case Constants.FRAGMENT_INTEREST_WEBVIEW:
				InterestProfileFragmentWebview.Type type1 = null;
				String id2 = null;
				String avatar2 = null;
				String wall2 = null;
				String score2 = null;
				if (extras != null) {
					if (extras.containsKey(Constants.EXTRA_PARAM_1))
						type1 = (InterestProfileFragmentWebview.Type) extras.getSerializable(Constants.EXTRA_PARAM_1);
					if (extras.containsKey(Constants.EXTRA_PARAM_2))
						id2 = extras.getString(Constants.EXTRA_PARAM_2);
					if (extras.containsKey(Constants.EXTRA_PARAM_3))
						avatar2 = extras.getString(Constants.EXTRA_PARAM_3);
					if (extras.containsKey(Constants.EXTRA_PARAM_4))
						wall2 = extras.getString(Constants.EXTRA_PARAM_4);
					if (extras.containsKey(Constants.EXTRA_PARAM_5))
						score2 = extras.getString(Constants.EXTRA_PARAM_5);
				}
				addWebviewFragment(type1, id2, avatar2, wall2, score2, null, requestCode, true);
				break;


			case Constants.FRAGMENT_INVALID:
				if (intent.hasExtra(Constants.EXTRA_PARAM_5) && intent.getBooleanExtra(Constants.EXTRA_PARAM_5, false))
					addNotificationsFragment(null, requestCode, true);
				break;
		}
	}


	//region == Profile fragment interface ==

	@Override
	public void onProfileFragmentInteraction(Uri uri) {}

	//endregion


	//region == Notifications fragment interface ==

//	@Override
//	public NotificationAndRequestHelper getNotificationHelper() {
//		return notificationHelper;
//	}
//
//	@Override
//	public View getBottomBarNotificationDot() {
//		return null;
//	}
//
//	@Override
//	public String getUserId() {
//		return mUser.getId();
//	}

	//endregion


	//region == Fragment section ==

	/* USER DETAIL FRAGMENT */
	public static void openUserDetailFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_USER_DETAILS, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openUserDetailFragment(Context context, int requestCode) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_USER_DETAILS, requestCode, ProfileActivity.class);
	}

	@Override
	public void showUserDetailFragment(String userId) {
		addUserDetailFragment(null, Constants.NO_RESULT, userId, true);
	}

	private void addUserDetailFragment(Fragment target, int requestCode, String userId, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		UserDetailFragment fragment = (UserDetailFragment) fragmentManager.findFragmentByTag(UserDetailFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = UserDetailFragment.newInstance(userId);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				UserDetailFragment.LOG_TAG, target, requestCode, UserDetailFragment.LOG_TAG);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode/*,
//					UserDetailFragment.LOG_TAG*/);
		fragmentTransaction.commit();
	}

	/* PROFILE CARD FRAGMENT */
	public static void openProfileCardFragment(Context context, ProfileHelper.ProfileType type,
	                                           String objectId, int bottomBarSelItem) {
		Bundle bundle = new Bundle();
		bundle.putSerializable(Constants.EXTRA_PARAM_1, type);
		bundle.putString(Constants.EXTRA_PARAM_2, objectId);
		bundle.putInt(Constants.EXTRA_PARAM_3, bottomBarSelItem);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_PROFILE_CARD, Constants.NO_RESULT, ProfileActivity.class);
	}

	@Override
	public void showProfileCardFragment(@Nullable ProfileHelper.ProfileType type, String objectId) {
		addProfileCardFragment(null, Constants.NO_RESULT, type, objectId, HomeActivity.PAGER_ITEM_PROFILE, true);
	}

	private void addProfileCardFragment(Fragment target, int requestCode, ProfileHelper.ProfileType type,
	                                    String personId, int bottomBarSelItem, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		ProfileFragment fragment = (ProfileFragment) fragmentManager.findFragmentByTag(ProfileFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = ProfileFragment.newInstance(type, personId, bottomBarSelItem);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				ProfileFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* INNER CIRCLE FRAGMENT */
	public static void openInnerCircleFragment(Context context, String userId, String name, String avatarUrl) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INNER_CIRCLE, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openInnerCircleFragment(Context context, String userId, String name,
	                                           String avatarUrl, boolean switchToContacts) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		bundle.putBoolean(Constants.EXTRA_PARAM_4, switchToContacts);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INNER_CIRCLE, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openInnerCircleFragment(Context context, String userId, String name,
	                                           String avatarUrl, int requestCode) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INNER_CIRCLE, requestCode, ProfileActivity.class);
	}

	@Override
	public void showInnerCircleFragment(String userId, String name, String avatarUrl) {
		addInnerCircleFragment(null, Constants.NO_RESULT, userId, name, avatarUrl, false, true);
	}

	private void addInnerCircleFragment(Fragment target, int requestCode, String userId, String name,
	                                    String avatarUrl, boolean switchToContacts, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InnerCircleFragment fragment = (InnerCircleFragment) fragmentManager.findFragmentByTag(InnerCircleFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = InnerCircleFragment.newInstance(userId, name, avatarUrl, switchToContacts);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				InnerCircleFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* MY INTERESTS FRAGMENT */
	public static void openMyInterestsFragment(Context context, String userId, String name, String avatar) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatar);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTERESTS, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openMyInterestsFragment(Context context, String userId, String name, String avatar,
	                                           int requestCode) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatar);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTERESTS, requestCode, ProfileActivity.class);
	}

	@Override
	public void showInterestsFragment(String userId, String name, String avatarUrl) {
		addInterestsFragment(null, Constants.NO_RESULT, userId, name, avatarUrl, true);
	}

	private void addInterestsFragment(Fragment target, int requestCode, String userId, String name,
	                                  String avatarUrl, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestsFragment fragment = (InterestsFragment) fragmentManager.findFragmentByTag(InterestsFragment.LOG_TAG);
		if (fragment == null) {
			fragment = InterestsFragment.newInstance(userId, name, avatarUrl);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					InterestsFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* FOLLOW NEW INTEREST */
	public static void openFollowInterestFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTERESTS_NEW, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openFollowInterestFragment(Context context, String userId, String name,
	                                              String avatarUrl, int requestCode) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTERESTS_NEW, requestCode, ProfileActivity.class);
	}

	@Override
	public void showFollowInterestFragment(String userId, String name, String avatarUrl) {
		addFollowInterestFragment(null, userId, name, avatarUrl, Constants.NO_RESULT, true);
	}

	private void addFollowInterestFragment(Fragment target,
	                                       String userId, String name, String avatarUrl,
	                                       int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		FollowInterestFragment fragment = (FollowInterestFragment) fragmentManager.findFragmentByTag(FollowInterestFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = FollowInterestFragment.newInstance(userId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				FollowInterestFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showBrowseInterestByCategoryFragment(String categoryId, String categoryName) {
		addBrowseInterestByCategoryFragment(null, categoryId, categoryName, Constants.NO_RESULT, true);
	}

	private void addBrowseInterestByCategoryFragment(Fragment target, String categoryId, String categoryName,
	                                                 int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		BrowseInterestsByCategoryFragment fragment = (BrowseInterestsByCategoryFragment)
				fragmentManager.findFragmentByTag(BrowseInterestsByCategoryFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = BrowseInterestsByCategoryFragment.newInstance(categoryId, categoryName);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				BrowseInterestsByCategoryFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* DIARY FRAGMENT */

	public static void openDiaryFragment(Context context, String userId, String name,
	                                     String avatarUrl) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_DIARY, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openDiaryFragment(Context context, String userId, String name,
	                                     String avatarUrl, int requestCode) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, userId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_DIARY, requestCode, ProfileActivity.class);
	}

	@Override
	public void showDiaryFragment(String userId, String name, String avatarUrl) {
		addDiaryFragment(null, userId, name, avatarUrl, Constants.NO_RESULT, true);
	}

	private void addDiaryFragment(Fragment target, String userId, String name, String avatarUrl,
	                              int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right,
					R.anim.slide_in_right, R.anim.slide_out_left);

		DiaryFragment fragment = (DiaryFragment) fragmentManager.findFragmentByTag(DiaryFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = DiaryFragment.newInstance(userId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				DiaryFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* TIMELINE FRAGMENT */

	@Override
	public void showDiaryTimelineFragment(String listName, String postId, String userId, String name, String avatarUrl) {
		addDiaryTimelineFragment(null, listName, postId, userId, name, avatarUrl, Constants.NO_RESULT, true);
	}

	private void addDiaryTimelineFragment(Fragment target, String listName, String postId,
	                                      String userId, String name, String avatarUrl,
	                                      int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right,
					R.anim.slide_in_right, R.anim.slide_out_left);

		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = TimelineFragment.newInstance(TimelineFragment.FragmentUsageType.DIARY,
				userId, name, avatarUrl, postId, listName, null);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				TimelineFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* FOLLOW NEW INTEREST */

	@Override
	public void showSearchFragment(String query, SearchTypeEnum type, String userId, String name, String avatarUrl) {
		addSearchFragment(null, query, type, userId, name, avatarUrl, Constants.NO_RESULT, true);
	}

	private void addSearchFragment(Fragment target, String query, SearchTypeEnum type, String userId,
	                               String name, String avatarUrl, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		ProfileSearchFragment fragment = (ProfileSearchFragment) fragmentManager.findFragmentByTag(ProfileSearchFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = ProfileSearchFragment.newInstance(query, type, userId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				ProfileSearchFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* CIRCLE VIEW MORE FRAGMENT */

	@Override
	public void showCircleViewMoreFragment(String circleName, String userId, String name, String avatarUrl) {
		addCircleViewMoreFragment(null, circleName, userId, name, avatarUrl, Constants.NO_RESULT, true);
	}

	private void addCircleViewMoreFragment(Fragment target, String circleName, String userId,
	                                       String name, String avatarUrl, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, 0,
					0, R.anim.slide_out_down);

		CircleViewMoreFragment fragment = (CircleViewMoreFragment) fragmentManager.findFragmentByTag(CircleViewMoreFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = CircleViewMoreFragment.newInstance(circleName, userId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				CircleViewMoreFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* INTEREST DETAIL */

	public static void openInterestDetailFragment(Context context) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_DETAILS, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openInterestDetailFragment(Context context, int requestCode) {
		Bundle bundle = new Bundle();
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_DETAILS, requestCode, ProfileActivity.class);
	}

	@Override
	public void showInterestDetailFragment(String interestId) {
		addInterestDetailFragment(null, Constants.NO_RESULT, interestId, true);
	}

	private void addInterestDetailFragment(Fragment target, int requestCode, String interestId, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestDetailFragment fragment = (InterestDetailFragment) fragmentManager.findFragmentByTag(InterestDetailFragment.LOG_TAG);
		if (fragment == null) {
			fragment = InterestDetailFragment.newInstance(interestId);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					InterestDetailFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* FOLLOWERS */

	public static void openFollowersFragment(Context context, String interestId, String name,
	                                         String avatarUrl) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, interestId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_FOLLOWERS, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openFollowersFragment(Context context, String interestId, String name,
	                                         String avatarUrl, int requestCode) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, interestId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_FOLLOWERS, requestCode, ProfileActivity.class);
	}

	@Override
	public void showFollowersFragment(String interestId, String name, String avatarUrl) {
		addFollowersFragment(null, Constants.NO_RESULT, interestId, name, avatarUrl, true);
	}

	private void addFollowersFragment(Fragment target, int requestCode, String interestId,
	                                  String name, String avatarUrl,  boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestFollowersFragment fragment = (InterestFollowersFragment) fragmentManager.findFragmentByTag(InterestFollowersFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = InterestFollowersFragment.newInstance(interestId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				InterestFollowersFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SIMILAR */

	public static void openSimilarForInterestFragment(Context context, String interestId, String name, String avatarUrl) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, interestId);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_SIMILAR, Constants.NO_RESULT, ProfileActivity.class);
	}

	@Override
	public void showSimilarEmptyDiaryFragment(String interestId, String name, String avatarUrl) {
		addSimilarForInterestFragment(null, Constants.NO_RESULT,
				SimilarForInterestFragment.ViewType.SIMILAR_EMPTY_DIARY, interestId,
				name, avatarUrl, true, true);
	}

	@Override
	public void showSimilarForInterestFragment(String interestId, String name, String avatarUrl) {
		addSimilarForInterestFragment(null, Constants.NO_RESULT, interestId, name, avatarUrl, true);
	}
	private void addSimilarForInterestFragment(Fragment target, int requestCode,
	                                           String interestId, String name,
	                                           String avatarUrl, boolean animate) {
		addSimilarForInterestFragment(target, requestCode,
				SimilarForInterestFragment.ViewType.SIMILAR, interestId, name,
				avatarUrl, animate, false);
	}


	private void addSimilarForInterestFragment(Fragment target, int requestCode,
	                                           SimilarForInterestFragment.ViewType type,
	                                           String interestId, String name,
	                                           String avatarUrl, boolean animate,
	                                           boolean reverseAnimation) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (animate) {
			@AnimRes int a1 = reverseAnimation ? R.anim.slide_in_left: R.anim.slide_in_right;
			@AnimRes int a2 = reverseAnimation ? R.anim.slide_out_right : R.anim.slide_out_left;
			@AnimRes int a3 = reverseAnimation ? R.anim.slide_in_right : R.anim.slide_in_left;
			@AnimRes int a4 = reverseAnimation ? R.anim.slide_out_left : R.anim.slide_out_right;
			fragmentTransaction.setCustomAnimations(a1, a2, a3, a4);
		}

		SimilarForInterestFragment fragment = (SimilarForInterestFragment) fragmentManager.findFragmentByTag(SimilarForInterestFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = SimilarForInterestFragment.newInstance(type, interestId, name, avatarUrl);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				SimilarForInterestFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* CLAIM INTEREST PAGE */

	@Override
	public void showClaimInterestFragment(String interestId, String name, String avatarUrl) {
		addInterestClaimFragment(null, Constants.NO_RESULT, interestId, name, avatarUrl, true);
	}

	private void addInterestClaimFragment(Fragment target, int requestCode, String interestId,
	                                      String name, String avatarUrl, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestClaimFragment fragment = (InterestClaimFragment) fragmentManager.findFragmentByTag(InterestClaimFragment.LOG_TAG);
		if (fragment == null) {
			fragment = InterestClaimFragment.newInstance(interestId, name, avatarUrl);
			FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
					InterestClaimFragment.LOG_TAG, target, requestCode);
		} else
			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* FAMILY RELATIONSHIPS PAGES */

	@Override
	public void showFamiyRelationsStep1Fragment() {
		addFamilyRelationsStep1Fragment(null, Constants.NO_RESULT, true);
	}

	private void addFamilyRelationsStep1Fragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		ProfileFamilyRelationsStep1Fragment fragment = (ProfileFamilyRelationsStep1Fragment) fragmentManager.findFragmentByTag(ProfileFamilyRelationsStep1Fragment.LOG_TAG);
//		if (fragment == null) {
		fragment = ProfileFamilyRelationsStep1Fragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				ProfileFamilyRelationsStep1Fragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showFamilyRelationsStep2Fragment(GenericUserFamilyRels selectedUser) {
		addFamilyRelationsStep2Fragment(null, selectedUser, Constants.NO_RESULT, true);
	}

	private void addFamilyRelationsStep2Fragment(Fragment target, GenericUserFamilyRels selectedUser, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		ProfileFamilyRelationsStep2Fragment fragment = (ProfileFamilyRelationsStep2Fragment) fragmentManager.findFragmentByTag(ProfileFamilyRelationsStep2Fragment.LOG_TAG);
//		if (fragment == null) {
		fragment = ProfileFamilyRelationsStep2Fragment.newInstance(selectedUser);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				ProfileFamilyRelationsStep2Fragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* NOTIFICATIONS */

	public static void openNotificationsFragment(Context context) {
		FragmentsUtils.openFragment(context, null, Constants.FRAGMENT_NOTIFICATIONS, Constants.NO_RESULT, ProfileActivity.class);
	}

	private void addNotificationsFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		NotificationsFragment fragment = (NotificationsFragment) fragmentManager.findFragmentByTag(NotificationsFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = NotificationsFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				NotificationsFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* INTEREST BRANDS / DETAIL(WV) / PRODUCTS (WV) */

	public static void openInterestBrandsFragment(Context context, String companyID, String name, String avatarUrl, String wallPicture, String score) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_PARAM_1, companyID);
		bundle.putString(Constants.EXTRA_PARAM_2, name);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		bundle.putString(Constants.EXTRA_PARAM_4, wallPicture);
		bundle.putString(Constants.EXTRA_PARAM_5, score);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_BRANDS, Constants.NO_RESULT, ProfileActivity.class);
	}

	public static void openInterestWebviewFragment(Context context, InterestProfileFragmentWebview.Type type,
												   String companyID, String avatarUrl,
												   String wallPicture, String score) {
		Bundle bundle = new Bundle();
		bundle.putSerializable(Constants.EXTRA_PARAM_1, type);
		bundle.putString(Constants.EXTRA_PARAM_2, companyID);
		bundle.putString(Constants.EXTRA_PARAM_3, avatarUrl);
		bundle.putString(Constants.EXTRA_PARAM_4, wallPicture);
		bundle.putString(Constants.EXTRA_PARAM_5, score);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_INTEREST_WEBVIEW, Constants.NO_RESULT, ProfileActivity.class);
	}

	@Override
	public void showInterestBrandsFragment(String companyID, String name, String avatarUrl, String wallPicture, String score) {
		addBrandsFragment(companyID, name, avatarUrl, wallPicture, score, null, Constants.NO_RESULT, true);
	}

	private void addBrandsFragment(String companyID, String name, String avatarUrl, String wallPicture, String score,
											Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestProfileFragmentBrands fragment = (InterestProfileFragmentBrands) fragmentManager.findFragmentByTag(InterestProfileFragmentBrands.getLOG_TAG());
//		if (fragment == null) {
		fragment = InterestProfileFragmentBrands.newInstance(companyID, name, avatarUrl, wallPicture, score);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				InterestProfileFragmentBrands.getLOG_TAG(), target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	@Override
	public void showInterestWebviewFragment(InterestProfileFragmentWebview.Type type, String companyID, String avatarUrl, String wallPicture, String score) {
		addWebviewFragment(type, companyID, avatarUrl, wallPicture, score, null, Constants.NO_RESULT, true);
	}

	private void addWebviewFragment(InterestProfileFragmentWebview.Type type, String companyID, String avatarUrl, String wallPicture, String score,
									Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		InterestProfileFragmentWebview fragment = (InterestProfileFragmentWebview) fragmentManager.findFragmentByTag(InterestProfileFragmentWebview.getLOG_TAG());
//		if (fragment == null) {
		fragment = InterestProfileFragmentWebview.newInstance(type, companyID, avatarUrl, wallPicture, score);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				InterestProfileFragmentWebview.getLOG_TAG(), target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	//endregion


	//region == Timeline interface ==

//	@Override
//	public void onManageTapForFullScreen(View clickedView, ValueAnimator maskOnUp, ValueAnimator maskOnLow, ValueAnimator maskOffUp, ValueAnimator maskOffLow) {
//
//	}

	@Override
	public void actionsForLandscape(@NonNull String postId, View view) {

		// TODO: 12/14/2017     needs implementation

	}

	@Override
	public void setLastAdapterPosition(int position) {
		lastAdapterPosition = position;
	}

	@Override
	public Integer getLastAdapterPosition() {
		return null;
	}

	@Override
	public FullScreenHelper getFullScreenListener() {
		return fullScreenListener;
	}

	@Override
	public Toolbar getToolbar() {
		return null;
	}

	@Override
	public View getBottomBar() {
		return null;
	}

	@Override
	public MediaHelper getMediaHelper() {
		if (mediaHelper == null)
			mediaHelper = new MediaHelper();

		return mediaHelper;
	}

	@Override
	public void goToInteractionsActivity(@NonNull String postId) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		startActivityForResult(intent, Constants.RESULT_TIMELINE_INTERACTIONS);
	}


	@Override
	public void saveFullScreenState() {
		fullScreenSavedState = fullScreenListener.getFullScreenType();
	}

	@Override
	public int getPageIdToCall() {
		return lastPageID + 1;
	}

	@Override
	public int getLastPageID() {
		return lastPageID;
	}

	@Override
	public void setLastPageID(int lastPageID) {
		this.lastPageID = lastPageID;
	}

	@Override
	public HLActivity getActivity() {
		return this;
	}

	@Override
	public void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener) {
		this.fsStateListener = fsStateListener;
	}

	@Override public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				fragment.getBottomSheetHelper().openPostSheet(postId, isOwnPost);
		}
	}

	@Override
	public boolean closePostSheet() {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				return fragment.getBottomSheetHelper().closePostSheet();
		}

		return false;
	}

	@Override
	public boolean isPostSheetOpen() {
		TimelineFragment fragment = (TimelineFragment) fragmentManager.findFragmentByTag(TimelineFragment.LOG_TAG);
		if (fragment != null && fragment.isVisible()) {
			if (fragment.getBottomSheetHelper() != null)
				return fragment.getBottomSheetHelper().isPostSheetOpen();
		}

		return false;
	}

	@Override
	public void goToProfile(@NonNull String userId, boolean isInterest) {
		if (mUser.getId().equals(userId)) {
			Intent intent = new Intent(this, HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
			startActivity(intent);
			finish();
		}
		else {
			ProfileHelper.ProfileType type = isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND;
			showProfileCardFragment(type, userId);
		}
	}

	@Override
	public void viewAllTags(Post post) {
		if (post != null) {
			if (post.hasTags()) {
				ArrayList<Tag> temp = new ArrayList<>();
				temp.addAll(post.getTags());

				Intent intent = new Intent(this, ViewAllTagsActivity.class);
				intent.putParcelableArrayListExtra(Constants.EXTRA_PARAM_1, temp);
				intent.putExtra(Constants.EXTRA_PARAM_2, true);
				startActivity(intent);
			}
		}
	}

	//endregion

	//region == Notifications helper listener //

	@Override
	public View getBottomBarNotificationDot() {
		return null;
	}

	@Override
	public String getUserId() {
		return mUser.getId();
	}

	@Override
	public View getTopIconNotificationDot() {
		return null;
	}

	@Override
	public NotificationAndRequestHelper getNotificationHelper() {
		return notificationHelper;
	}

	//endregion


	//region == Getters and setters ==

	public OnBackPressedListener getBackListener() {
		return backListener;
	}
	public void setBackListener(OnBackPressedListener backListener) {
		this.backListener = backListener;
	}


	//endregion

}
