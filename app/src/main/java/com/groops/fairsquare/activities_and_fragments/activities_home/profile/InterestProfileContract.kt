package com.groops.fairsquare.activities_and_fragments.activities_home.profile

import com.groops.fairsquare.adapters.InterestBrandsAdapterDiff
import com.groops.fairsquare.base.BaseContract

/**
 * Declares the View-Presenter contract for the new Interest Profile feature (DETAIL/PRODUCTS).
 * @author mbaldrighi on 2019-04-25.
 */
interface InterestProfileContract {

    interface InterestProfilePresenter: BaseContract.BasePresenter

    interface InterestProfileView: BaseContract.BaseView, InterestBrandsAdapterDiff.OnBrandClickListener {
        fun getCompanyId(): String?
    }

}