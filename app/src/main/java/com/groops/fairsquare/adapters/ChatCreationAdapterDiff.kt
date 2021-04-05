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
import com.groops.fairsquare.models.chat.ChatParticipant
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.MediaHelper
import com.groops.fairsquare.voiceVideoCalls.tmp.VoiceVideoCallType


class ChatCreationAdapterDiff(diffUtilCallback: ChatParticipantDiffCallback, private val newChatCallback: NewChatCallback):
        ListAdapter<ChatParticipant, ChatCreationAdapterDiff.ChatCreationVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatCreationVH {
        return ChatCreationVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_creation, parent, false))
    }

    override fun onBindViewHolder(holder: ChatCreationVH, position: Int) {
        holder.setItem(getItem(position))
    }

    override fun submitList(list: List<ChatParticipant>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }


    inner class ChatCreationVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var profilePicture: ImageView? = null
        private var userName: TextView? = null
        private var chatBtn: View? = null
        private var voiceBtn: View? = null
        private var videoBtn: View? = null

        private var currentUser: ChatParticipant? = null


        init {
            with (itemView) {
                profilePicture = this.findViewById(R.id.profilePicture)
                userName = this.findViewById(R.id.userName)
                chatBtn = (this.findViewById(R.id.btnTextChat) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onChatClicked(currentUser) }
                }
                voiceBtn = (this.findViewById(R.id.btnVoice) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onCallClicked(currentUser, VoiceVideoCallType.VOICE) }
                }
                videoBtn = (this.findViewById(R.id.btnVideo) as? View)?.apply {
                    this.setOnClickListener { newChatCallback.onCallClicked(currentUser, VoiceVideoCallType.VIDEO) }
                }
            }
        }


        fun setItem(user: ChatParticipant?) {

            with (user) {
                currentUser = user

                if (!this?.avatarURL.isNullOrBlank())
                    MediaHelper.loadProfilePictureWithPlaceholder(profilePicture?.context, this?.avatarURL, profilePicture)
                else
                    profilePicture?.setImageResource(R.drawable.ic_profile_placeholder)

                userName?.text = this?.name

                chatBtn?.visibility = if (this?.canChat == true) View.VISIBLE else View.GONE
                voiceBtn?.visibility = if (this?.canAudiocall == true) View.VISIBLE else View.GONE
                videoBtn?.visibility = if (this?.canVideocall == true && Utils.hasDeviceCamera(videoBtn?.context))
                    View.VISIBLE else View.GONE

            }
        }
    }

    interface NewChatCallback {
        fun onChatClicked(user: ChatParticipant?)
        fun onCallClicked(user: ChatParticipant?, callType: VoiceVideoCallType)
    }

}


class ChatParticipantDiffCallback: DiffUtil.ItemCallback<ChatParticipant>() {

    override fun areItemsTheSame(oldItem: ChatParticipant, newItem: ChatParticipant): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ChatParticipant, newItem: ChatParticipant): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                oldItem.name == newItem.name &&
                oldItem.canChat == newItem.canChat &&
                oldItem.canAudiocall == newItem.canAudiocall &&
                oldItem.canVideocall == newItem.canVideocall
                )
    }
}