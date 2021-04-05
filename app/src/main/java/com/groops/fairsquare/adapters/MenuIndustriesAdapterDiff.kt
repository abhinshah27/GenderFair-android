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
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.R
import com.groops.fairsquare.models.MenuIndustry
import com.groops.fairsquare.utility.helpers.MediaHelper


class MenuIndustriesAdapterDiff(diffUtilCallback: MenuLandingDiffCallback, val callback: OnMenuLandingItemClickListener?):
        ListAdapter<MenuIndustry, MenuIndustriesAdapterDiff.MenuIndustryItemVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuIndustryItemVH {
        return MenuIndustryItemVH(LayoutInflater.from(parent.context).inflate(R.layout.item_menu_list, parent, false))
    }

    override fun onBindViewHolder(holder: MenuIndustryItemVH, position: Int) {
        holder.setItem(getItem(position))
    }

    override fun submitList(list: List<MenuIndustry>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }


    inner class MenuIndustryItemVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var profilePicture: ImageView? = null
        private var userName: TextView? = null

        private var currentItem: MenuIndustry? = null


        init {
            with (itemView) {
                profilePicture = this.findViewById(R.id.profilePicture)
                userName = this.findViewById(R.id.name)

                setOnClickListener { callback?.onMenuLandingItemClicked(currentItem) }
            }
        }


        fun setItem(brand: MenuIndustry?) {

            with (brand) {
                if (brand == null) return@with

                currentItem = brand

                if (!this?.avatarURL.isNullOrBlank())
                    MediaHelper.loadProfilePictureWithPlaceholder(profilePicture?.context, this?.avatarURL, profilePicture, true)
                else
                    profilePicture?.setImageResource(R.drawable.ic_interest_placeholder)

                userName?.text = this?.name
            }
        }

    }

    interface OnMenuLandingItemClickListener {
        fun onMenuLandingItemClicked(item: MenuIndustry?)
    }

}


class MenuLandingDiffCallback: DiffUtil.ItemCallback<MenuIndustry>() {

    override fun areItemsTheSame(oldItem: MenuIndustry, newItem: MenuIndustry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MenuIndustry, newItem: MenuIndustry): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                        oldItem.name == newItem.name &&
                        oldItem.id == newItem.id
                )
    }
}