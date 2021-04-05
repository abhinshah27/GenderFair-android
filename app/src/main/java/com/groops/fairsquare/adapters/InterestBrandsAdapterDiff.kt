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
import com.groops.fairsquare.models.InterestBrand
import com.groops.fairsquare.utility.helpers.MediaHelper


class InterestBrandsAdapterDiff(
        diffUtilCallback: InterestBrandDiffCallback,
        val callback: OnBrandClickListener?,
        private val wantsBars: Boolean = false
): ListAdapter<InterestBrand, InterestBrandsAdapterDiff.InterestBrandVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestBrandVH {
        return InterestBrandVH(
                LayoutInflater.from(parent.context)
                        .inflate(
                                if (wantsBars) R.layout.item_menu_list_progress else R.layout.item_brand,
                                parent,
                                false
                        )
        )
    }

    override fun onBindViewHolder(holder: InterestBrandVH, position: Int) {
        holder.setItem(getItem(position))
    }

    override fun submitList(list: List<InterestBrand>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }


    inner class InterestBrandVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var profilePicture: ImageView? = null
        private var userName: TextView? = null
        private var rating: ProgressBar? = null

        private var currentBrand: InterestBrand? = null


        init {
            with (itemView) {
                profilePicture = this.findViewById(R.id.profilePicture)
                userName = this.findViewById(R.id.name)

                rating = this.findViewById(R.id.rating)

                setOnClickListener { callback?.onBrandClicked(currentBrand) }
            }
        }


        fun setItem(brand: InterestBrand?) {

            with (brand) {
                if (brand == null) return@with

                currentBrand = brand

                if (!this?.avatarURL.isNullOrBlank())
                    MediaHelper.loadProfilePictureWithPlaceholder(profilePicture?.context, this?.avatarURL, profilePicture, true)
                else
                    profilePicture?.setImageResource(R.drawable.ic_interest_placeholder)

                userName?.text = this?.name

                // INFO: 2019-05-07    bars are no more
//                if (brand.getScore() <= 0) rating?.visibility = View.GONE
//                else rating?.progress = (brand.getScore() * 10).toInt()
            }
        }

    }

    interface OnBrandClickListener {
        fun onBrandClicked(brand: InterestBrand?)
    }

}


class InterestBrandDiffCallback: DiffUtil.ItemCallback<InterestBrand>() {

    override fun areItemsTheSame(oldItem: InterestBrand, newItem: InterestBrand): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: InterestBrand, newItem: InterestBrand): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                        oldItem.name == newItem.name &&
                        oldItem.wallImageLink == newItem.wallImageLink &&
                        oldItem.id == newItem.id &&
                        oldItem.score == newItem.score
                )
    }
}