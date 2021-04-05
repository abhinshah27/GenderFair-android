/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_home.global_search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.groops.fairsquare.R;
import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.menu.HomeMenuActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.menu.MenuActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.profile.ProfileActivity;
import com.groops.fairsquare.activities_and_fragments.activities_home.profile.ProfileHelper;
import com.groops.fairsquare.adapters.GlobalSearchListsAdapter;
import com.groops.fairsquare.adapters.GlobalSearchObject;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.base.HLFragment;
import com.groops.fairsquare.models.InterestCategory;
import com.groops.fairsquare.models.PostList;
import com.groops.fairsquare.models.UsersBundle;
import com.groops.fairsquare.models.enums.GlobalSearchTypeEnum;
import com.groops.fairsquare.utility.ComputeAndPopulateHandlerThread;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.utility.helpers.AnimatedSearchHelper;
import com.groops.fairsquare.utility.helpers.SearchHelper;
import com.groops.fairsquare.websocket_connection.HLRequestTracker;
import com.groops.fairsquare.websocket_connection.HLServerCalls;
import com.groops.fairsquare.websocket_connection.OnMissingConnectionListener;
import com.groops.fairsquare.websocket_connection.OnServerMessageReceivedListener;
import com.groops.fairsquare.websocket_connection.ServerMessageReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Massimo on 4/10/2018.
 */
public class GlobalSearchFragment extends HLFragment implements GlobalSearchListsAdapter.OnGlobalSearchActionListener,
		SearchHelper.OnQuerySubmitted, OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = GlobalSearchFragment.class.getCanonicalName();

	private View focusCatcher;
	private EditText searchBox;
	private String query;

	private SwipeRefreshLayout srl;

	private RecyclerView globalSearchView;
	private LinearLayoutManager llm;
	private GlobalSearchListsAdapter mAdapter;
	private List<GlobalSearchObject> globalSearchList = new ArrayList<>();

	private TextView noResult;

	private Integer scrollPosition;
	private SparseArray<WeakReference<HorizontalScrollView>> scrollViews = new SparseArray<>();
	private SparseArray<int[]> scrollViewsPositions = new SparseArray<>();

	private SearchHelper mSearchHelper = null;
	private AnimatedSearchHelper mAnimatedHelper = null;

	private View menu;
	private boolean menuVisible = false;


	public GlobalSearchFragment() {}

	public static GlobalSearchFragment newInstance() {
		Bundle args = new Bundle();
		GlobalSearchFragment fragment = new GlobalSearchFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);

		setRetainInstance(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.home_fragment_global_search, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		callGlobalActions(
				(Utils.isStringValid(query) && query.length() >= 3) ?
						HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
				query
		);
		setLayout();
	}

	@Override
	public void onPause() {

		onSaveInstanceState(new Bundle());

		if (mAnimatedHelper.getSearchOn()) mAnimatedHelper.closeSearch();
		else Utils.closeKeyboard(searchBox);

		int position = llm.findFirstCompletelyVisibleItemPosition();
		scrollPosition = position == -1 ? llm.findFirstVisibleItemPosition() : position;
		if (scrollViews != null && scrollViews.size() > 0) {
			for (int i = 0; i < scrollViews.size(); i++) {
				int key = scrollViews.keyAt(i);
				WeakReference<HorizontalScrollView> hsv = scrollViews.get(key);
				HorizontalScrollView scrollView = hsv.get();
				if (scrollView != null && scrollView.getTag() instanceof Integer) {
					scrollViewsPositions.put(
							((Integer) scrollView.getTag()),
							new int[] {
									scrollView.getScrollX(),
									scrollView.getScrollY()
							}
					);
				}
			}
		}

		super.onPause();
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_SEARCH_GLOBAL:
				setData(responseObject, true);
				resetPositions(false);
				break;

			case Constants.SERVER_OP_SEARCH_GLOBAL_MOST_POP:
				setData(responseObject, false);
				resetPositions(false);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
		resetPositions(false);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		// RecView scroll position
		if (scrollPosition != null)
			outState.putInt(Constants.EXTRA_PARAM_1, scrollPosition);

		// HorScrollViews management
		if (scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			outState.putInt(Constants.EXTRA_PARAM_2, scrollViewsPositions.size());

			for (int i = 0; i < scrollViewsPositions.size(); i++) {
				int[] arr = scrollViewsPositions.valueAt(i);
				if (arr != null && arr.length == 2) {
					outState.putIntArray(String.valueOf(i), arr);
				}
			}
		}

		outState.putBoolean(Constants.EXTRA_PARAM_3, mAnimatedHelper.getSearchOn());
		outState.putString(Constants.EXTRA_PARAM_4, query);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			scrollPosition = savedInstanceState.getInt(Constants.EXTRA_PARAM_1, 0);

			int scrollViewsSize = savedInstanceState.getInt(Constants.EXTRA_PARAM_2, 0);
			if (scrollViewsSize > 0) {
				scrollViewsPositions = new SparseArray<>();
				for (int i = 0; i < scrollViewsSize; i++) {
					if (savedInstanceState.containsKey(String.valueOf(i))) {
						scrollViewsPositions.put(i, savedInstanceState.getIntArray(String.valueOf(i)));
					}
				}
			}

			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				query = savedInstanceState.getString(Constants.EXTRA_PARAM_4);

		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		view.findViewById(R.id.search_container).setPadding(0, 0, 0, 0);

		focusCatcher = view.findViewById(R.id.focus_catcher);

		// INFO: 3/19/19    hide search
		View searchLayout = view.findViewById(R.id.search_box);
		searchLayout.setVisibility(View.GONE);

		// INFO: 4/8/19     the searchLayout to use is the one contained in animated_search
		View searchLayout1 = view.findViewById(R.id.searchBox);

		searchBox = searchLayout1.findViewById(R.id.search_field);
		searchBox.setHint(R.string.global_search_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout1, searchBox);

		if (mAnimatedHelper == null)
			mAnimatedHelper = new AnimatedSearchHelper(view.getContext(), false);
		mAnimatedHelper.configureViews(view);

		srl = Utils.getGenericSwipeLayout(view, () -> {
			resetPositions(true);

			Utils.setRefreshingForSwipeLayout(srl, true);

			callGlobalActions(
					hasValidQuery() ? HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
					searchBox.getText().toString()
			);
		});

		globalSearchView = view.findViewById(R.id.generic_rv);
		mAdapter = new GlobalSearchListsAdapter(globalSearchList, this);
		globalSearchView.setAdapter(mAdapter);
		globalSearchView.setLayoutManager(llm = new LinearLayoutManager(view.getContext(), RecyclerView.VERTICAL, false));

		noResult = view.findViewById(R.id.no_result);

		View menuBtn = view.findViewById(R.id.menu);
		menuBtn.setOnClickListener(menu -> {
			if (Utils.isContextValid(getActivity())) {
				startActivity(new Intent(menu.getContext(), HomeMenuActivity.class));
				getActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.no_animation);
			}
		});

		View searchBtn = view.findViewById(R.id.globalSearchContainer);
		searchBtn.setOnClickListener(btn -> {
			if (mAnimatedHelper != null) {
				if (!mAnimatedHelper.getSearchOn()) mAnimatedHelper.openSearch();
				else mAnimatedHelper.closeSearch();
			}
		});

	}

	@Override
	protected void setLayout() {
		focusCatcher.requestFocus();

		srl.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_margin_lg), 0, 0);

		if (mAnimatedHelper.getSearchOn() || Utils.isStringValid(query))
			mAnimatedHelper.openSearch();
	}


	@Override
	public void onQueryReceived(String query) {
		this.query = query;
		callGlobalActions(
				Utils.isStringValid(query) ?
						HLServerCalls.GlobalSearchAction.SEARCH : HLServerCalls.GlobalSearchAction.MOST_POPULAR,
				query
		);

		if (mAnimatedHelper != null && Utils.isStringValid(query))
			mAnimatedHelper.closeSearch();
	}

	private boolean hasValidQuery() {
		String text = searchBox.getText().toString();
		return searchBox != null && Utils.isStringValid(text)/* && text.length() >= 3*/;
	}


	private void restorePositions() {
		if (scrollPosition != null) {
			llm.scrollToPosition(scrollPosition);
		}
	}

	private void resetPositions(boolean resetScrollViews) {
		scrollPosition = null;

		if (resetScrollViews) {
			if (scrollViewsPositions == null)
				scrollViewsPositions = new SparseArray<>();
			else
				scrollViewsPositions.clear();
		}
	}

	public void resetSearch() {
		query = "";
		searchBox.setText("");
		if (mAnimatedHelper != null)
			mAnimatedHelper.closeSearch();
	}

	@Override
	public void saveScrollView(int position, HorizontalScrollView scrollView) {
		if (scrollViews == null)
			scrollViews = new SparseArray<>();

		scrollViews.put(position, new WeakReference<>(scrollView));
	}

	@Override
	public void restoreScrollView(int position) {
		if (scrollViews != null && scrollViews.size() > 0 &&
				scrollViewsPositions != null && scrollViewsPositions.size() > 0) {
			if (scrollViews.size() >= position) {
				final HorizontalScrollView hsv = scrollViews.get(position).get();
				if (hsv != null) {
					final int[] coords = scrollViewsPositions.get(position);
					if (coords != null) {
						new Handler().post(() -> hsv.scrollTo(coords[0], coords[1]));
					}
				}
			}
		}
	}


	private void callGlobalActions(HLServerCalls.GlobalSearchAction action, String query) {
		Object[] result = null;

		try {
			result = HLServerCalls.actionsOnGlobalSearch(mUser.getId(), query, action, null, -1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}


	private void setData(JSONArray response, boolean fromSearch) {

		noResult.setText(fromSearch ? R.string.no_search_result : R.string.no_result_landing);

		if (response == null || response.length() == 0 || response.optJSONObject(0).length() == 0) {
			globalSearchView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		globalSearchView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (globalSearchList == null)
			globalSearchList = new ArrayList<>();
		else
			globalSearchList.clear();

		new PopulateGlobalThread("populateGlobal", response, fromSearch).start();
	}


	//region -- Adapter interface --

	@Override
	public void goToTimeline(@NonNull String listName, @Nullable String postId) {

		GlobalSearchActivity.openGlobalSearchTimelineFragment(getContext(), listName, postId, mUser.getId(),
				mUser.getCompleteName(), mUser.getAvatarURL(), searchBox.getText().toString());
	}

	@Override
	public void goToInterestsUsersList(GlobalSearchTypeEnum returnType, String title) {

		GlobalSearchActivity.openGlobalSearchUsersInterestsFragment(getContext(), searchBox.getText().toString(),
				returnType, title);
	}

	@Override
	public void goToInterestUserProfile(String id, boolean isInterest) {
		ProfileActivity.openProfileCardFragment(
				getContext(),
				isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND,
				id,
				HomeActivity.PAGER_ITEM_GLOBAL_SEARCH
		);
	}

	@Override
	public void goToShop() {
		if (!Utils.checkAndOpenLogin(getActivity(), mUser) && Utils.isContextValid(getContext()))
			MenuActivity.Companion.openMenuLandingFragment(getContext(), MenuActivity.FlowType.SHOP);
	}

	//endregion


	private class PopulateGlobalThread extends ComputeAndPopulateHandlerThread {

		private final boolean fromSearch;

		PopulateGlobalThread(String name, JSONArray response, boolean fromSearch) {
			super(name, response);

			this.fromSearch = fromSearch;
		}

		@Override
		public void customActions(@org.jetbrains.annotations.Nullable JSONArray jsonResponse) {
			if (jsonResponse != null) {
				JSONObject lists = jsonResponse.optJSONObject(0);
				if (lists != null && lists.length() > 0) {
					Iterator<String> iter = lists.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						if (Utils.isStringValid(key)) {
							JSONObject list = lists.optJSONObject(key);
							if (list != null) {
								GlobalSearchObject obj = new GlobalSearchObject(
										list.optString("searchResultType"),
										list.optString("type"),
										list.optString("UIType")
								);

								Object mainObject = null;
								if (obj.isUsers()) {
									mainObject = new UsersBundle().deserializeToClass(list);
									((UsersBundle) mainObject).setNameToDisplay(key);
								} else if (obj.isInterests()) {
									mainObject = new InterestCategory().deserializeToClass(list);
									((InterestCategory) mainObject).setNameToDisplay(key);
								} else if (obj.isPosts()) {
									mainObject = new PostList().deserializeToClass(list);
									((PostList) mainObject).setNameToDisplay(key);
								}
								obj.setMainObject(mainObject);
								globalSearchList.add(obj);
							}
						}
					}

					if (!fromSearch) {
						// add dummy item to signal the presence of the Button to the adapter

						GlobalSearchObject dummy = new GlobalSearchObject();
						dummy.setSortOrder(
								globalSearchList.get(globalSearchList.size()-1).getSortOrder()
						);
						dummy.setButton(true);
						globalSearchList.add(dummy);
					}

					Collections.sort(globalSearchList);
				}
			}
		}

		@Override
		public boolean customHandling(@org.jetbrains.annotations.Nullable Message msg) {
			getActivity().runOnUiThread(() -> {
				mAdapter.notifyDataSetChanged();
				restorePositions();
			});

			return true;
		}
	}

}
