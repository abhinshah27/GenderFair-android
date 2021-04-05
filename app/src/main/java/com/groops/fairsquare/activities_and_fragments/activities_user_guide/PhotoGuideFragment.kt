/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_user_guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.GlideApp
import com.groops.fairsquare.utility.Utils


/**
 * @author mbaldrighi on 5/4/2018.
 */
class PhotoGuideFragment : HLFragment() {

    @DrawableRes
    internal var source: Int = 0
    private var main: ImageView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        onRestoreInstanceState(savedInstanceState ?: arguments)

        main = ImageView(inflater.context)
        val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        main!!.layoutParams = lp
        main!!.setBackgroundColor(Utils.getColor(inflater.context, R.color.colorAccent))
        main!!.scaleType = ImageView.ScaleType.CENTER_CROP

        return main
    }

    override fun onResume() {
        super.onResume()

        setLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(Constants.EXTRA_PARAM_1, source)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
                source = savedInstanceState.getInt(Constants.EXTRA_PARAM_1, 0)
        }
    }

    override fun configureResponseReceiver() {}

    override fun configureLayout(view: View) {}

    override fun setLayout() {
        if (source != 0)
            GlideApp.with(this).load(source).into(main!!)
    }

    companion object {

        fun newInstance(@DrawableRes source: Int): PhotoGuideFragment {
            val args = Bundle()
            args.putInt(Constants.EXTRA_PARAM_1, source)
            val fragment = PhotoGuideFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
