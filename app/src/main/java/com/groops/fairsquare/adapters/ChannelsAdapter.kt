package com.groops.fairsquare.adapters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.groops.fairsquare.R
import com.groops.fairsquare.models.FeedChannel
import com.groops.fairsquare.models.GFChannels
import com.groops.fairsquare.utility.GlideApp
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.MediaHelper
import kotlinx.android.synthetic.main.custom_layout_profile_picture_shadow_tl.view.*
import kotlinx.android.synthetic.main.item_tl_channel.view.*
import kotlinx.android.synthetic.main.share_selection_overlay.view.*


class ChannelsAdapter(private val channels: List<FeedChannel>?, private val listener: OnChannelSelectedListener): RecyclerView.Adapter<ChannelsAdapter.ChannelVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelVH {
        return ChannelVH(LayoutInflater.from(parent.context).inflate(R.layout.item_tl_channel, parent, false))
    }

    override fun getItemCount(): Int {
        return channels?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return channels?.get(position)?.hashCode()?.toLong() ?: super.getItemId(position)
    }


    override fun onBindViewHolder(holder: ChannelVH, position: Int) {
        holder.setChannel(channels?.get(position))
    }

    override fun onViewRecycled(holder: ChannelVH) {
        holder.itemView.tag = null
        GlideApp.with(holder.itemView).clear(holder.itemView.pic)

        super.onViewRecycled(holder)
    }


    inner class ChannelVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                itemView.isSelected = !itemView.isSelected

                handleSelectionOverlay(itemView.selection_overlay, itemView.isSelected)

                val id = itemView.tag as? String
                if (itemView.isSelected)
                    listener.onChannelSelected(id)
                else
                    listener.onChannelDeselected(id)
            }

            itemView.selection_overlay.setBackgroundResource(R.drawable.shape_circle_orange_alpha)
        }

        fun setChannel(channel: FeedChannel?) {

            itemView.tag = channel?.userID

            if (channel != null && channels != null) {
                if (channels.indexOf(channel) == 0)
                    (itemView.layoutParams as? RecyclerView.LayoutParams)?.marginStart = Utils.dpToPx(8f, itemView.resources)
                if (channels.indexOf(channel) == (channels.size - 1))
                    (itemView.layoutParams as? RecyclerView.LayoutParams)?.marginEnd = Utils.dpToPx(8f, itemView.resources)
            }

            itemView.isSelected = (channel?.userID?.equals(GFChannels.currentSelectedChannel ?: "") == true)
            handleSelectionOverlay(itemView.selection_overlay, itemView.isSelected)

            itemView.channelName.text = channel?.name

            MediaHelper.loadProfilePictureWithPlaceholder(itemView.context, channel?.avatarURL, itemView.pic, true)

            // INFO: 2019-06-07    It seems that this is not needed anymore. But it stays here
//            GlideApp
//                    .with(itemView)
//                    .asBitmap()
//                    .load(channel?.avatarURL)
//                    .override(100, 100)
//                    .into(
//                            object : CustomViewTarget<ImageView, Bitmap>(itemView.picture as ImageView) {
//                                override fun onLoadFailed(errorDrawable: Drawable?) {
//                                    getView().setImageResource(R.drawable.ic_profile_placeholder)
//                                }
//
//                                override fun onResourceCleared(placeholder: Drawable?) {
//                                    getView().setImageResource(R.drawable.ic_profile_placeholder)
//                                }
//
//                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//
//                                    doAsync {
//
//                                        val width = resource.width
//                                        val height = resource.height
//
//                                        val scaledBitmap =
//                                                when {
//                                                    width > height -> {
//                                                        scaleBitmapAndKeepRatio(resource, width, width)
//                                                    }
//                                                    width < height -> {
//                                                        scaleBitmapAndKeepRatio(resource, height, height)
//                                                    }
//                                                    else -> resource
//                                                }
//
//                                        val drawable = RoundedBitmapDrawableFactory.create(itemView.resources, scaledBitmap)
//                                        drawable.isCircular = true
//
//                                        getView().post { getView().setImageDrawable(drawable) }
//                                    }
//                                }
//                            }
//                    )
        }

        private fun handleSelectionOverlay(view: View?, selected: Boolean) {
            view?.visibility = if (selected) View.VISIBLE else View.GONE
        }
    }

    internal fun scaleBitmapAndKeepRatio(originalImage: Bitmap, width: Int, height: Int): Bitmap {
        val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        val canvas = Canvas(background)

        val scale = width.toFloat() / originalWidth.toFloat()

        val xTranslation = 0.0f
        val yTranslation = (height - originalHeight * scale) / 2.0f

        val transformation = Matrix()
        transformation.postTranslate(xTranslation, yTranslation)
        transformation.preScale(scale, scale)

        val paint = Paint()
        paint.isFilterBitmap = true

        canvas.drawBitmap(originalImage, transformation, paint)

        return background
    }


    interface OnChannelSelectedListener {
        fun onChannelSelected(id: String?)
        fun onChannelDeselected(id: String?)
    }


}