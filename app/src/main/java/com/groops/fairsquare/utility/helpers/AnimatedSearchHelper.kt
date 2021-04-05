/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility.helpers

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.groops.fairsquare.R
import com.groops.fairsquare.utility.Utils
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 4/8/2018.
 */

class AnimatedSearchHelper(context: Context, private val wantsOverlay: Boolean = false): BaseHelper(context) {

    private val resources by lazy { context.resources }

    private var searchBoxReference: WeakReference<View>? = null
    private var searchOverlayReference: WeakReference<View>? = null
    private var searchFieldReference: WeakReference<EditText>? = null

    var searchDrawable: TransitionDrawable? = null
    var searchOn = false
    private val animationSetOn by lazy {
        AnimatorSet().also {
            it.duration = 350
            if (wantsOverlay) it.playTogether(animationOverlayOn, animationSearchOn)
            else it.play(animationSearchOn)
        }
    }
    private val animationSetOff by lazy {
        AnimatorSet().also {
            it.duration = 350
            if (wantsOverlay) it.playTogether(animationOverlayOff, animationSearchOff)
            else it.play(animationSearchOff)
        }
    }

    private val animationOverlayOn by lazy {
        ObjectAnimator.ofFloat(searchOverlayReference?.get(), "alpha",1f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {
                    searchOverlayReference?.get()?.visibility = View.VISIBLE
                    searchOn = true
                }
            })
        }
    }

    private val animationOverlayOff by lazy {
        ObjectAnimator.ofFloat(searchOverlayReference?.get(), "alpha",0f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    searchOverlayReference?.get()?.visibility = View.GONE
                    searchOn = false
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
    }

    private val animationSearchOn by lazy {
        ObjectAnimator.ofFloat(searchBoxReference?.get(), "translationY", resources.getDimensionPixelSize(R.dimen.toolbar_height).toFloat()).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    searchOn = true
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    searchDrawable?.startTransition(0)
                }
            })
        }
    }

    private val animationSearchOff by lazy {
        ObjectAnimator.ofFloat(searchBoxReference?.get(), "translationY", 0f).also {
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    searchOn = false
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    searchDrawable?.reverseTransition(0)
                }
            })
        }
    }


    fun configureViews(view: View) {
        searchBoxReference = WeakReference(view.findViewById(R.id.searchBox))
        searchFieldReference = WeakReference(searchBoxReference!!.get()!!.findViewById(R.id.search_field))
        searchOverlayReference = WeakReference(view.findViewById(R.id.overlay))

        val toolbar = view.findViewById(R.id.toolbar) as View?
        val btn = toolbar?.findViewById(R.id.globalSearchBtn) as ImageView?
        searchDrawable = ((btn?.drawable) as? TransitionDrawable)?.also {
            it.isCrossFadeEnabled = true
        }

        searchOverlayReference?.get()?.setOnClickListener { closeSearch() }
    }

    fun openSearch() {
        animationSetOn.start()
        Utils.openKeyboard(searchFieldReference?.get())
    }

    fun closeSearch() {
        animationSetOff.start()
        Utils.closeKeyboard(searchFieldReference?.get())
    }


}