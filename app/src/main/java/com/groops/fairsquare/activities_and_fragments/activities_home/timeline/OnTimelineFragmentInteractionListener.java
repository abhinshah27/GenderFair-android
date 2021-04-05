/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_home.timeline;

import android.content.res.Resources;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity;
import com.groops.fairsquare.base.HLActivity;
import com.groops.fairsquare.models.HLPosts;
import com.groops.fairsquare.models.Post;
import com.groops.fairsquare.utility.helpers.FullScreenHelper;
import com.groops.fairsquare.utility.helpers.MediaHelper;

import io.realm.Realm;


/**
 * This interface must be implemented by activities that contain this
 * {@link TimelineFragment} to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p/>
 *
 * @author mbaldrighi on 10/18/2017.
 */
public interface OnTimelineFragmentInteractionListener {

	/**
	 * Communicates taps on Fragment's mainView to {@link HomeActivity} when media ratio would better
	 * fit in a landscape mode.
	 */
	void actionsForLandscape(@NonNull String postId, View view);

	void setLastAdapterPosition(int position);
	Integer getLastAdapterPosition();

	FullScreenHelper getFullScreenListener();
	Toolbar getToolbar();
	View getBottomBar();

	MediaHelper getMediaHelper();

	void goToInteractionsActivity(@NonNull String postId);
	void goToProfile(@NonNull String userId, boolean isInterest);
	void saveFullScreenState();

	int getPageIdToCall();
	int getLastPageID();
	void setLastPageID(int lastPageID);

	default int getFeedSkip(@Nullable Realm realm, @Nullable TimelineFragment.FragmentUsageType type,
	                        boolean wantsTop) {
		if (wantsTop)
			return 0;
		else {
			HLPosts posts = HLPosts.getInstance();
			return posts.getFeedPostsSkip(type);
		}
	}

	/**
	 * @return The {@link HLActivity} instance underneath the interface.
	 */
	HLActivity getActivity();
	/**
	 * @return The {@link Resources} instance underneath the interface.
	 */
	Resources getResources();

	void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener);

	boolean isPostSheetOpen();
	boolean closePostSheet();
	void openPostSheet(@NonNull String postId, boolean isUserAuthor);

	void viewAllTags(Post post);

}
