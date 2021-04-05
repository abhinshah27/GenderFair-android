package com.groops.fairsquare.activities_and_fragments.activities_onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.R
import com.groops.fairsquare.models.OnBoardingButtonElement
import com.groops.fairsquare.utility.helpers.MediaHelper

class OnBoardingButtonsAdapter(
        private val items: MutableList<OnBoardingButtonElement>,
        private val listener: OnButtonClickListener,
        private val buttonType: OnBoardingButtonsFragment.ButtonType
): RecyclerView.Adapter<OnBoardingButtonsAdapter.ButtonsVH>()  {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonsVH {
        return if (buttonType == OnBoardingButtonsFragment.ButtonType.CHANNELS)
            ButtonsVH(LayoutInflater.from(parent.context).inflate(R.layout.component_onboarding_button, parent, false))
        else
            ButtonsWithPicVH(LayoutInflater.from(parent.context).inflate(R.layout.component_onboarding_button_picture, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ButtonsVH, position: Int) {
        holder.setButton(items[position])
    }


    open inner class ButtonsVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var currentElement: OnBoardingButtonElement? = null

        private var text = itemView.findViewById<TextView>(R.id.text)

        init {
            itemView.let {
                it.setOnClickListener {

                    itemView.isSelected = !itemView.isSelected
                    currentElement?.selected = itemView.isSelected

                    if (!currentElement?.id.isNullOrBlank()) {
                        if (itemView.isSelected) listener.onButtonSelected(currentElement!!.id!!)
                        else listener.onButtonDeselected(currentElement!!.id!!)
                    }
                }

                (itemView.layoutParams as? RecyclerView.LayoutParams)?.let { lp ->
                    lp.topMargin = itemView.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
                    lp.bottomMargin = itemView.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
                }
            }
        }

        internal open fun setButton(element: OnBoardingButtonElement?) {
            currentElement = element

            text.text = element?.name
            itemView.isSelected = element?.selected ?: false
        }

    }

    inner class ButtonsWithPicVH(itemView: View): ButtonsVH(itemView) {

        private var picture = itemView.findViewById<ImageView>(R.id.profilePicture)

        override fun setButton(element: OnBoardingButtonElement?) {
            super.setButton(element)

            MediaHelper.loadProfilePictureWithPlaceholder(itemView.context, element?.avatarURL, picture, true)
        }

    }


    interface OnButtonClickListener {
        fun onButtonSelected(elementID: String)
        fun onButtonDeselected(elementID: String)
    }

}



