/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility.helpers;

/**
 * @author mbaldrighi on 12/23/2017.
 */
public interface OnNotificationsClickListener {
	void onNotificationButtonClick(boolean close);
	boolean isNotificationPanelOpen();
}
