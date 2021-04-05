package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.groops.fairsquare.R
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.OnBackPressedListener
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.FragmentsUtils
import kotlinx.android.synthetic.main.activity_menu_action_list.*
import kotlinx.android.synthetic.main.gf_toolbar_simple_back.*

class MenuActivity: HLActivity(), MenuActivityListener {

    enum class FlowType { SHOP, WORK, INVEST }

    private var flowType: FlowType? = null
    private var industryName: String? = null

    var backListener: OnBackPressedListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_action_list)
        setRootContent(R.id.rootContent)
        setProgressIndicator(R.id.genericProgressIndicator)

        back_arrow.setOnClickListener { onBackPressed() }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount > 1 && !industryName.isNullOrBlank()) showSubtitle()
            else hideSubtitle()
        }

        manageIntent()
    }

    override fun onResume() {
        super.onResume()

        sectionTitle.text = when (flowType) {
            FlowType.SHOP -> getString(R.string.menu_title_shop)
            FlowType.WORK -> getString(R.string.menu_title_work)
            FlowType.INVEST -> getString(R.string.menu_title_invest)
            else -> null
        }
    }

    override fun configureResponseReceiver() {}

    override fun manageIntent() {
        if (intent == null) return

        val showFragment = intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
                Constants.FRAGMENT_INVALID)
        val requestCode = intent.getIntExtra(Constants.REQUEST_CODE_KEY, Constants.NO_RESULT)
        val extras = intent.extras
        val userId: String? = null
        val name: String? = null
        val avatar: String? = null

        when (showFragment) {
            Constants.FRAGMENT_MENU_LIST -> {
                flowType = extras?.getSerializable(Constants.EXTRA_PARAM_1) as? FlowType

                if (flowType == null) { finish(); return }

                val type = extras?.getSerializable(Constants.EXTRA_PARAM_2) as? MenuListFragment.Type

                // intent handles only first LANDING fragment. no "itemID" required
//                val itemID = extras?.getString(Constants.EXTRA_PARAM_3)

                if (type != null)
                    addMenuListFragment(type, null, target = null, requestCode = requestCode)
            }
        }
    }


    override fun onBackPressed() {
        when {
            backListener != null -> backListener!!.onBackPressed()
            supportFragmentManager.backStackEntryCount == 1 -> {
                finish()
//                overridePendingTransition(R.anim.no_animation, R.anim.slide_out_right)
            }
            else -> super.onBackPressed()
        }
    }

    override fun getFlowType(): FlowType? {
        return flowType
    }

    override fun getCloseButton(): View? {
        return this@MenuActivity.closeBtn
    }

    override fun showSubtitle() {
        this@MenuActivity.subtitle?.text = industryName
        this@MenuActivity.subtitleGroup?.visibility = View.VISIBLE
    }

    override fun hideSubtitle() {
        industryName = null
        this@MenuActivity.subtitleGroup?.visibility = View.GONE
    }


    //region == Fragment section ==

    override fun showMenuListFragment(type: MenuListFragment.Type, itemID: String?, industryName: String) {
        if (this.industryName == null)
            this.industryName = industryName

        addMenuListFragment(type, itemID)
    }

    private fun addMenuListFragment(type: MenuListFragment.Type, itemID: String?, target: Fragment? = null, requestCode: Int = Constants.NO_RESULT, animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right)

        var fragment = supportFragmentManager.findFragmentByTag(MenuListFragment.LOG_TAG) as? MenuListFragment
//        if (fragment == null) {
            fragment = MenuListFragment.newInstance(type, itemID)
            FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
                    MenuListFragment.LOG_TAG, target, requestCode, MenuListFragment.LOG_TAG)
//        } else
//            FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode)

        fragmentTransaction.commit()
    }

    //endregion



    companion object {

        fun openMenuLandingFragment(context: Context, flowType: FlowType) {
            val bundle = Bundle()
            bundle.putSerializable(Constants.EXTRA_PARAM_1, flowType)
            bundle.putSerializable(Constants.EXTRA_PARAM_2, MenuListFragment.Type.LANDING)
            FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_MENU_LIST,
                    Constants.NO_RESULT, MenuActivity::class.java, R.anim.slide_in_right, R.anim.no_animation)
        }

    }

}

interface MenuActivityListener {
    fun getFlowType(): MenuActivity.FlowType?
    fun showMenuListFragment(type: MenuListFragment.Type, itemID: String?, industryName: String)
    fun getCloseButton(): View?
    fun showSubtitle()
    fun hideSubtitle()
}