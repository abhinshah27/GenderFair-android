/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_home.settings;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.groops.fairsquare.models.HLIdentity;
import com.groops.fairsquare.models.MarketPlace;
import com.groops.fairsquare.models.SettingsHelpElement;
import com.groops.fairsquare.models.enums.PrivacyPostVisibilityEnum;

import java.util.ArrayList;

/**
 * @author mbaldrighi on 3/19/2018.
 */
public interface SettingsActivityListener {

	void setToolbarTitle(@StringRes int titleResId);
	void setToolbarTitle(@NonNull String title);

	void refreshProfilePicture(@NonNull String link);
	void refreshProfilePicture(@NonNull Drawable drawable);

	void addRemoveTopPaddingFromFragmentContainer(boolean add);
	void setToolbarVisibility(boolean visible);
	void setBottomBarVisibility(boolean visible);


	void closeScreen();


	/* FRAGMENTS section */

	void showSettingsAccountFragment();
	void showSettingsInnerCircleFragment();
	void showSettingsFoldersFragment();
	void showSettingsPrivacyFragment();
	void showSettingsSecurityFragment();

	void showInnerCircleCirclesFragment();
	void showInnerCircleSingleCircleFragment(@NonNull String circleName, @Nullable String filter);
	void showInnerCircleTimelineFeedFragment();
	void showFoldersPostFragment(String listName);
	void showPrivacySelectionFragment(Fragment target, int privacyEntry, String title);
	void showPrivacyBlockedUsersFragment();
	void showPrivacyPostVisibilitySelectionFragment(SettingsPrivacySelectionFragment.PrivacySubItem item,
	                                                PrivacyPostVisibilityEnum type);
	void showSecurityDeleteAccountFragment();
	void showSecurityLegacyContactTriggerFragment();
	void showSecurityLegacy2StepFragment(boolean isSelection, @Nullable ArrayList<String> filters);
	void showSettingsHelpListFragment(@NonNull SettingsHelpElement element);
	void showSettingsYesNoUIFragment(@NonNull SettingsHelpElement element);
	void showSettingsHelpContactFragment();
	void showSettingsYesNoUIFragmentStatic(@NonNull SettingsHelpElement element, @NonNull String itemId, SettingsHelpYesNoUIFragment.UsageType type);

	void goToUserGuide();

	void showSettingsRedeemHeartsSelectFragment(@NonNull HLIdentity identity);
	void showSettingsRedeemHeartsConfirmFragment(@NonNull HLIdentity identity, @NonNull MarketPlace marketPlace, double heartsValue);

}
