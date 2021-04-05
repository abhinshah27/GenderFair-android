/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.R
import com.groops.fairsquare.models.InterestCompany
import com.groops.fairsquare.utility.helpers.MediaHelper


class MenuCompaniesAdapterDiff(diffUtilCallback: MenuCompanyDiffCallback, val callback: OnMenuCompanyLandingItemClickListener?):
        ListAdapter<InterestCompany, MenuCompaniesAdapterDiff.MenuCompanyItemVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuCompanyItemVH {
        return MenuCompanyItemVH(LayoutInflater.from(parent.context).inflate(R.layout.item_menu_list_progress, parent, false))
    }

    override fun onBindViewHolder(holder: MenuCompanyItemVH, position: Int) {
        holder.setItem(getItem(position))
    }

    override fun submitList(list: List<InterestCompany>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }


    inner class MenuCompanyItemVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var profilePicture: ImageView? = null
        private var userName: TextView? = null
        private var rating: ProgressBar? = null

        private var currentCompany: InterestCompany? = null


        init {
            with (itemView) {
                profilePicture = this.findViewById(R.id.profilePicture)
                userName = this.findViewById(R.id.name)

                setOnClickListener { callback?.onMenuCompanyItemClicked(currentCompany) }
            }
        }


        fun setItem(company: InterestCompany?) {

            with (company) {
                if (company == null) return@with

                currentCompany = company

                if (!this?.avatarURL.isNullOrBlank())
                    MediaHelper.loadProfilePictureWithPlaceholder(profilePicture?.context, this?.avatarURL, profilePicture, true)
                else
                    profilePicture?.setImageResource(R.drawable.ic_interest_placeholder)

                userName?.text = this?.name

                // INFO: 2019-05-15    bars are no more
//                if (company.getScore() <= 0) rating?.visibility = View.GONE
//                else rating?.progress = (company.getScore() * 10).toInt()
            }
        }

    }

    interface OnMenuCompanyLandingItemClickListener {
        fun onMenuCompanyItemClicked(company: InterestCompany?)
    }

}


class MenuCompanyDiffCallback: DiffUtil.ItemCallback<InterestCompany>() {

    override fun areItemsTheSame(oldItem: InterestCompany, newItem: InterestCompany): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: InterestCompany, newItem: InterestCompany): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                        oldItem.name == newItem.name &&
                        oldItem.wallImageLink == newItem.wallImageLink &&
                        oldItem.id == newItem.id &&
                        oldItem.score == newItem.score
                )
    }
}