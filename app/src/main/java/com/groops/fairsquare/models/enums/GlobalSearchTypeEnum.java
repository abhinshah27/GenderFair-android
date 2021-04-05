/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.models.enums;

import com.groops.fairsquare.utility.Utils;

import java.io.Serializable;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public enum GlobalSearchTypeEnum implements Serializable {

	USERS("users"),
	INTERESTS("interests"),
	COMPANIES("company"),
	BRANDS("brand"),
	STORIES("story"),
	CHANNELS("channels"),
	ORGANIZATIONS("organizations");
//	P_PUBLIC("publicposts"),
//	P_MY("myposts"),
//	P_MY_FEED("myfeedposts");

	private String value;

	GlobalSearchTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static GlobalSearchTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        GlobalSearchTypeEnum[] statuses = GlobalSearchTypeEnum.values();
		for (GlobalSearchTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

}
