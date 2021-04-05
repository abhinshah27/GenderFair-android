package com.groops.fairsquare.activities_and_fragments.activities_home.profile

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.models.InterestBrand
import com.groops.fairsquare.utility.AnalyticsUtils
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.MediaHelper
import kotlinx.android.synthetic.main.custom_layout_rating_badge.view.*
import kotlinx.android.synthetic.main.layout_fragment_brand.*
import kotlinx.android.synthetic.main.layout_header_interest_profile.*
import kotlinx.android.synthetic.main.layout_search_plus_rv.*

class InterestProfileFragmentBrands: HLFragment(), InterestProfileContract.InterestProfileView {

    companion object {
        @JvmStatic val LOG_TAG = InterestProfileFragmentBrands::class.qualifiedName

        @JvmStatic fun newInstance(companyID: String, name: String, avatarUrl: String?, wallPicture: String?, score: String?):
                InterestProfileFragmentBrands {

            val fragment = InterestProfileFragmentBrands()
            fragment.arguments = Bundle().apply {
                this.putString(Constants.EXTRA_PARAM_1, companyID)
                this.putString(Constants.EXTRA_PARAM_2, name)
                this.putString(Constants.EXTRA_PARAM_3, avatarUrl)
                this.putString(Constants.EXTRA_PARAM_4, wallPicture)
                this.putString(Constants.EXTRA_PARAM_5, score)
            }

            return fragment
        }
    }

    private var helper: InterestProfileContract.InterestProfilePresenter? = null

    private var rootView: View? = null
    private var srl: SwipeRefreshLayout? = null

    private var companyID: String? = null
    private var name: String? = null
    private var avatarURL: String? = null
    private var wallURL: String? = null
    private var score: String? = null
    private var scrollPosition = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        onRestoreInstanceState(savedInstanceState ?: arguments)

        rootView = inflater.inflate(R.layout.fragment_interest_brands, container, false)

        srl = Utils.getGenericSwipeLayout(rootView!!) {
            Utils.setRefreshingForSwipeLayout(srl, true)
            helper?.callServer()
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeBtn.setOnClickListener { (view.context as? Activity)?.onBackPressed() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (helper == null)
           helper = InterestHelperBrands(context!!, this).also { it.init() }

        generic_rv.layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
        generic_rv.adapter = helper?.getRVAdapter()
        generic_rv.addItemDecoration(DividerItemDecoration(rootView!!.context, LinearLayout.VERTICAL))
    }

    override fun onStart() {
        super.onStart()

        MediaHelper.loadPictureWithGlide(context, wallURL, backgroundPicture as ImageView)
        MediaHelper.loadProfilePictureWithPlaceholder(context, avatarURL, profilePicture as ImageView, true)

        ratingBadge.visibility = if (!score.isNullOrBlank()) View.VISIBLE else View.GONE
        ratingBadge.score.text = score

        searchRVContainer.setPadding(0, 0, 0, 0)
        search_box.visibility = View.GONE

        generic_rv.post { this@InterestProfileFragmentBrands.generic_rv!!.scrollToPosition(scrollPosition) }

        companyName.text = name
        handleDividerView()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(context, AnalyticsUtils.INTEREST_BRANDS)

        helper?.handleServerReceiver(true)
        helper?.callServer()
    }

    override fun onPause() {
        scrollPosition = (generic_rv?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0

        onSaveInstanceState(Bundle())

        super.onPause()
    }

    override fun onStop() {
        helper?.handleServerReceiver(false)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(Constants.EXTRA_PARAM_1, companyID)
        outState.putString(Constants.EXTRA_PARAM_2, name)
        outState.putString(Constants.EXTRA_PARAM_3, avatarURL)
        outState.putString(Constants.EXTRA_PARAM_4, wallURL)
        outState.putString(Constants.EXTRA_PARAM_5, score)
        outState.putInt(Constants.EXTRA_PARAM_6, scrollPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        companyID = savedInstanceState?.getString(Constants.EXTRA_PARAM_1)
        name = savedInstanceState?.getString(Constants.EXTRA_PARAM_2)
        avatarURL = savedInstanceState?.getString(Constants.EXTRA_PARAM_3)
        wallURL = savedInstanceState?.getString(Constants.EXTRA_PARAM_4)
        score = savedInstanceState?.getString(Constants.EXTRA_PARAM_5)
        scrollPosition = savedInstanceState?.getInt(Constants.EXTRA_PARAM_6, scrollPosition) ?: scrollPosition
    }


    override fun onBrandClicked(brand: InterestBrand?) {
        if (brand != null) {
            profileActivityListener.showInterestWebviewFragment(
                    InterestProfileFragmentWebview.Type.PRODUCTS,
                    brand.id,
                    brand.avatarURL,
                    brand.wallImageLink,
                    brand.score
            )
        }
    }

    override fun getCompanyId(): String? {
        return companyID
    }


    override fun init() {}

    override fun handleSRL(show: Boolean) {
        Utils.setRefreshingForSwipeLayout(srl, show)
    }

    override fun handleRVVisibility(show: Boolean) {
        generic_rv?.visibility = if (show) View.VISIBLE else View.GONE
        no_result?.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun handleDividerView() {
        val bounds = Rect()
        companyName?.paint?.getTextBounds(name, 0, name?.length ?: 0, bounds)

        titlesDivider?.layoutParams?.width = bounds.width() - (Utils.dpToPx(R.dimen.activity_margin_lg, context?.resources) * 2)
    }


    /* NO OPS */
    override fun configureResponseReceiver() {}
    override fun configureLayout(view: View) {}
    override fun setLayout() {}

}