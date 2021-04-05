/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.groops.fairsquare.R;
import com.groops.fairsquare.activities_and_fragments.activities_home.settings.SettingsInnerEntriesFragment;
import com.groops.fairsquare.models.enums.SecurityEntriesEnum;
import com.groops.fairsquare.utility.Utils;

import java.util.List;

/**
 * @author mbaldrighi on 3/19/2018.
 */
public class SettingsListViewAdapter extends ArrayAdapter<CharSequence> {

	private @LayoutRes int resourceId;
	private SettingsInnerEntriesFragment.ViewType mViewType;

	public SettingsListViewAdapter(@NonNull Context context, int resource, @NonNull CharSequence[] objects,
	                               @Nullable SettingsInnerEntriesFragment.ViewType viewType) {
		super(context, resource, objects);

		resourceId = resource;
		mViewType = viewType;
	}

	public SettingsListViewAdapter(@NonNull Context context, int resource, @NonNull List<CharSequence> objects) {
		super(context, resource, objects);

		resourceId = resource;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		CharSequence entry = getItem(position);

		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
		}

		TextView text = convertView.findViewById(R.id.action_text);
		if (entry != null && Utils.isStringValid(entry.toString())) {
			text.setText(entry);

			// INFO: 4/15/19    removes LEGACY CONTACT from app
			// enables light gray background if row has even position number
//			convertView.setActivated((position % 2) == 0);

			if (mViewType != null && mViewType == SettingsInnerEntriesFragment.ViewType.SECURITY) {

				// INFO: 4/15/19    LEGACY CONTACT LOGIC NO LONGER APPLIES >> position 1 assumed
				SecurityEntriesEnum entryEnum = SecurityEntriesEnum.toEnum(1);

				if (entryEnum != null && entryEnum == SecurityEntriesEnum.DELETE_ACCOUNT)
					Utils.applyFontToTextView(text, R.string.osSemiBold);
			}
		}


		return convertView;
	}
}
