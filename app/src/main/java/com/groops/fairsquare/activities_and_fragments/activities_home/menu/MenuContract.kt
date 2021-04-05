package com.groops.fairsquare.activities_and_fragments.activities_home.menu

import com.groops.fairsquare.adapters.InterestBrandsAdapterDiff
import com.groops.fairsquare.adapters.MenuCompaniesAdapterDiff
import com.groops.fairsquare.adapters.MenuIndustriesAdapterDiff
import com.groops.fairsquare.base.BaseContract

interface MenuContract {

    interface MenuPresenter: BaseContract.BasePresenter

    interface MenuView: BaseContract.BaseView {
        fun getItemID(): String?
    }

    interface MenuBrandsView: MenuView, InterestBrandsAdapterDiff.OnBrandClickListener
    interface MenuCompaniesView: MenuView, MenuCompaniesAdapterDiff.OnMenuCompanyLandingItemClickListener
    interface MenuIndustriesView: MenuView, MenuIndustriesAdapterDiff.OnMenuLandingItemClickListener

}