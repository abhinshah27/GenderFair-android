package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.groops.fairsquare.R
import com.groops.fairsquare.activities_and_fragments.activities_home.profile.InterestProfileFragmentWebview
import com.groops.fairsquare.activities_and_fragments.activities_home.profile.ProfileActivity
import com.groops.fairsquare.activities_and_fragments.activities_home.profile.ProfileHelper
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.models.InterestBrand
import com.groops.fairsquare.models.InterestCompany
import com.groops.fairsquare.models.MenuIndustry
import com.groops.fairsquare.utility.AnalyticsUtils
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.BaseHelper
import kotlinx.android.synthetic.main.layout_search_plus_rv.*

class MenuListFragment: HLFragment(),
        MenuContract.MenuBrandsView,
        MenuContract.MenuCompaniesView,
        MenuContract.MenuIndustriesView {

    companion object {
        val LOG_TAG = MenuListFragment::class.qualifiedName

        fun newInstance(type: Type, itemID: String?): MenuListFragment {
            val fragment = MenuListFragment()
            fragment.arguments = Bundle().apply {
                this.putSerializable(Constants.EXTRA_PARAM_1, type)
                this.putString(Constants.EXTRA_PARAM_2, itemID)
            }

            return fragment
        }
    }

    enum class Type { LANDING, INNER }

    private var type: Type? = null
    private var itemID: String? = null
    private var scrollPosition = 0

    private var menuPresenter: MenuContract.MenuPresenter? = null

    private var menuListHelper: BaseHelper? = null

    private var rootView: View? = null
    private var srl: SwipeRefreshLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.layout_search_plus_rv, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onRestoreInstanceState(savedInstanceState ?: arguments)

        srl = Utils.getGenericSwipeLayout(view) {
            Utils.setRefreshingForSwipeLayout(srl, true)
            menuPresenter?.callServer()
        }

        generic_rv!!.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        generic_rv!!.adapter = menuPresenter?.getRVAdapter()
        generic_rv!!.addItemDecoration(DividerItemDecoration(view.context, LinearLayout.VERTICAL))
    }


    override fun onStart() {
        super.onStart()

        rootView?.setPadding(0, 0, 0, 0)

        search_box.visibility = View.GONE

        generic_rv.post { this@MenuListFragment.generic_rv.scrollToPosition(scrollPosition) }

        no_result?.setText(menuPresenter?.getNoResultString() ?: 0)
    }


    override fun onResume() {
        super.onResume()

        if (type != null) {
            AnalyticsUtils.trackScreen(
                    context,
                    // TODO: 2019-05-08    in case of further changes (WORK/INVEST flows re-instated) review this logic
                    if (type == Type.LANDING) AnalyticsUtils.HOME_MENU_INDUSTRIES else AnalyticsUtils.HOME_MENU_BRANDS
            )
        }

        menuPresenter?.handleServerReceiver(true)
        menuPresenter?.callServer()
    }

    override fun onPause() {
        scrollPosition = (generic_rv.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0
        super.onPause()
    }

    override fun onStop() {
        menuPresenter?.handleServerReceiver(false)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        menuListHelper?.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        type = savedInstanceState?.getSerializable(Constants.EXTRA_PARAM_1) as? Type
        itemID = savedInstanceState?.getString(Constants.EXTRA_PARAM_2)
        scrollPosition = savedInstanceState?.getInt(Constants.EXTRA_PARAM_3, scrollPosition) ?: scrollPosition

        if (Utils.isContextValid(context) && type != null) {
            menuPresenter = when (type) {
                Type.LANDING -> MenuListHelperIndustries(this, context!!)
                Type.INNER -> {
                    when (menuActivityListener.getFlowType()) {
                        MenuActivity.FlowType.SHOP -> MenuListHelperCompanies(this, context!!)
                        MenuActivity.FlowType.WORK -> MenuListHelperCompanies(this, context!!)
                        MenuActivity.FlowType.INVEST -> MenuListHelperCompanies(this, context!!)
                        else -> null
                    }
                }
                else -> null
            }

            menuPresenter?.init()
        }

        menuPresenter?.onRestoreInstanceState(savedInstanceState)
    }


    override fun onMenuLandingItemClicked(item: MenuIndustry?) {
        menuActivityListener.showMenuListFragment(Type.INNER, item?.id, item?.name ?: "")
    }

    override fun onMenuCompanyItemClicked(company: InterestCompany?) {
        if (Utils.isContextValid(context))
            ProfileActivity.openProfileCardFragment(context!!, ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED, company?.id, -1)
    }

    // INFO: 2019-05-15    from this flow only companies are showed
    override fun onBrandClicked(brand: InterestBrand?) {
        if (Utils.isContextValid(context)) {
            ProfileActivity.openInterestWebviewFragment(
                    context!!,
                    InterestProfileFragmentWebview.Type.PRODUCTS,
                    brand?.id,
                    brand?.avatarURL,
                    brand?.wallImageLink,
                    brand?.score
            )
        }
    }


    override fun init() {}

    override fun handleSRL(show: Boolean) {
        Utils.setRefreshingForSwipeLayout(srl, show)
    }

    override fun handleRVVisibility(show: Boolean) {
        generic_rv?.visibility = if (show) View.VISIBLE else View.GONE
        no_result?.visibility = if (!show) View.VISIBLE else View.GONE
    }

    override fun getItemID(): String? {
        return itemID
    }


    fun handleActivitySubtitle(show: Boolean, name: String?) {
        if (show && !name.isNullOrBlank()) menuActivityListener?.showSubtitle()
        else menuActivityListener?.hideSubtitle()
    }


    /*  NO OPS */
    override fun configureResponseReceiver() {}
    override fun configureLayout(view: View) {}
    override fun setLayout() {}

}