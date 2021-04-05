/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.models.enums;

import com.groops.fairsquare.utility.Utils;

/**
 * @author mbaldrighi on 4/10/2019.
 */
public enum InterestTypeEnum {

	BRAND("Brand"),
	COMPANY("Company"),
	CHANNEL("Channels"),
	ORGANIZATION("Organizations");

	private String value;

	InterestTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static InterestTypeEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

		InterestTypeEnum[] statuses = InterestTypeEnum.values();
		for (InterestTypeEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

}
