/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_user_guide

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager

import com.groops.fairsquare.R
import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.models.HLUser
import com.groops.fairsquare.utility.AnalyticsUtils
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.SharedPrefsUtils
import com.groops.fairsquare.utility.Utils
import kotlinx.android.synthetic.main.activity_user_guide_pager.*

/**
 * A screen showing introductory guide to Highlanders app.
 *
 * Consider relating to [this link](https://guides.codepath.com/android/Working-with-the-ImageView) and
 * [this link](https://androidsnippets.wordpress.com/2012/10/25/how-to-scale-a-bitmap-as-per-device-width-and-height)
 * to improve UX with scaled bitmaps.
 */
class UserGuideActivity : HLActivity() {
    private var mType: ViewType? = null

    internal var currentItem = 0

    private var mPager: ViewPager? = null

    private var drawBack: Drawable? = null
    private var drawNext: Drawable? = null

    enum class ViewType {
        FIRST_OPEN, SETTINGS
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_guide_pager)
        setRootContent(R.id.root_content)

        val decorView = window.decorView
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
        decorView.systemUiVisibility = uiOptions
        setImmersiveValue(true)

        manageIntent()

        with(btnBack) {
            this.setOnClickListener {
                mPager!!.currentItem = --currentItem
            }
            drawBack = this?.compoundDrawablesRelative?.get(0)
        }
        with(btnNext) {
            this.setOnClickListener {
                if (currentItem == 3 && it.isActivated) {

                    SharedPrefsUtils.setGuideSeen(this@UserGuideActivity, true)
                    HLUser().write(realm)

                    startActivity(Intent(this@UserGuideActivity, HomeActivity::class.java))
                    finish()
                    overridePendingTransition(0, R.anim.alpha_out)
                } else {
                    mPager!!.currentItem = ++currentItem
                }
            }
            drawNext = this?.compoundDrawablesRelative?.get(2)
        }

        mPager = findViewById(R.id.pager)
        mPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                currentItem = position

                btnBack!!.isEnabled = position != 0
                btnBack!!.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                btnNext!!.isActivated = position == 3
                btnNext!!.setText(if (position == 3) R.string.action_go_to_app else R.string.action_next)


                if (!Utils.hasMarshmallow())
                    drawNext!!.setColorFilter(
                            Utils.getColor(
                                    this@UserGuideActivity,
                                    if (position == 3) R.color.white else R.color.colorAccent
                            ),
                            PorterDuff.Mode.SRC_ATOP
                    )
                else
                    btnNext!!.compoundDrawableTintList = resources.getColorStateList(R.color.state_list_guide_button, null)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        mPager!!.adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> PhotoGuideFragment.newInstance(R.drawable.user_guide_0)
                    1 -> PhotoGuideFragment.newInstance(R.drawable.user_guide_1)
                    2 -> PhotoGuideFragment.newInstance(R.drawable.user_guide_2)
                    3 -> PhotoGuideFragment.newInstance(R.drawable.user_guide_3)
                    else -> PhotoGuideFragment.newInstance(R.drawable.user_guide_0)
                }
            }

            override fun getCount(): Int {
                return 4
            }
        }

        mPager!!.currentItem = 0
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(this, AnalyticsUtils.SETTINGS_PRIVACY_USER_GUIDE)

        btnsBackground.visibility = if (mType == ViewType.SETTINGS) View.GONE else View.VISIBLE
        btnBack.visibility = if (currentItem == 0) View.GONE else View.VISIBLE
        with(btnNext) {
            this.isActivated = currentItem == 3
            this.setText(if (currentItem == 3) R.string.action_go_to_app else R.string.action_next)
        }

        if (!Utils.hasMarshmallow()) {
            drawNext!!.setColorFilter(
                    Utils.getColor(
                            this@UserGuideActivity,
                            if (currentItem == 3) R.color.white else R.color.colorAccent
                    ),
                    PorterDuff.Mode.SRC_ATOP
            )
            drawBack!!.setColorFilter(Utils.getColor(this, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP)
        } else
            btnNext!!.compoundDrawableTintList = resources.getColorStateList(R.color.state_list_guide_button, null)

    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            if (intent.hasExtra(Constants.EXTRA_PARAM_1))
                mType = intent.getSerializableExtra(Constants.EXTRA_PARAM_1) as ViewType
        }
    }

    companion object {
        val LOG_TAG = UserGuideActivity::class.qualifiedName
    }

}