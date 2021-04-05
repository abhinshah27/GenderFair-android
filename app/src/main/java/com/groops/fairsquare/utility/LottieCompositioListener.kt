/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility

import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieListener
import com.groops.fairsquare.base.HLApp

abstract class LottieCompositioListener: LottieListener<LottieComposition> {

    override fun onResult(result: LottieComposition?) {
        HLApp.siriComposition = result
    }
}