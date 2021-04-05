/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.activities_and_fragments.activities_chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.groops.fairsquare.R
import com.groops.fairsquare.activities_and_fragments.activities_home.HomeActivity
import com.groops.fairsquare.adapters.ChatRoomsAdapter
import com.groops.fairsquare.base.HLActivity
import com.groops.fairsquare.base.HLApp
import com.groops.fairsquare.base.HLFragment
import com.groops.fairsquare.base.OnBackPressedListener
import com.groops.fairsquare.models.HLIdentity
import com.groops.fairsquare.models.HLUser
import com.groops.fairsquare.models.chat.ChatMessage
import com.groops.fairsquare.models.chat.ChatRoom
import com.groops.fairsquare.utility.Constants
import com.groops.fairsquare.utility.LogUtils
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.MediaHelper
import com.groops.fairsquare.utility.helpers.RealTimeChatListener
import com.groops.fairsquare.utility.helpers.SearchHelper
import com.groops.fairsquare.utility.realm.RealmUtils
import com.groops.fairsquare.websocket_connection.*
import io.realm.*
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import kotlinx.android.synthetic.main.item_chat_identity.view.*
import kotlinx.android.synthetic.main.layout_search_plus_rv.view.*
import kotlinx.android.synthetic.main.profile_search_box.view.*
import org.json.JSONArray

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatRoomsFragment: HLFragment(), OnBackPressedListener, OnServerMessageReceivedListener,
        OnMissingConnectionListener, RealTimeChatListener, SearchHelper.OnQuerySubmitted,
        ChatRoomsAdapter.OnDeleteRoomListener, HLWebSocketAdapter.ConnectionObserver {

    companion object {

        val LOG_TAG = ChatRoomsFragment::class.qualifiedName

        @JvmStatic fun newInstance(): ChatRoomsFragment {
            val fragment = ChatRoomsFragment()
            fragment.arguments = Bundle().apply {
                //                this.put...
            }

            return fragment
        }
    }

    private var adapter: ChatRoomsAdapter? = null
    private val llm: LinearLayoutManager by lazy {
        LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private var srl: SwipeRefreshLayout? = null

    private val searchHelper by lazy { SearchHelper(this) }

    private var chatForDeletion: String? = null

    private var savedSearchString: String? = null

    private var identitiesVisible: Boolean? = null
    private val animationIdentitiesTriangleOn by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.identitiesTriangle, "alpha", 1f)
    }
    private val animationIdentitiesTriangleOff by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.identitiesTriangle, "alpha", 0f)
    }
    private val animationIdentitiesListOn by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.identitiesSV, "alpha", 1f)
    }
    private val animationIdentitiesListOff by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.identitiesSV, "alpha", 0f)
    }
    private val animationIdentitiesOverlayOn by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.overlay, "alpha", 1f)
    }
    private val animationIdentitiesOverlayOff by lazy {
        ObjectAnimator.ofFloat(this@ChatRoomsFragment.overlay, "alpha", 0f)
    }
    private val animationIdentitiesSetOn by lazy {
        AnimatorSet().also {
            it.duration = 250
            it.playTogether(animationIdentitiesTriangleOn, animationIdentitiesListOn, animationIdentitiesOverlayOn)
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    identitiesVisible = true
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {
                    identitiesTriangle?.visibility = View.VISIBLE
                    identitiesSV?.visibility = View.VISIBLE
                    overlay?.visibility = View.VISIBLE
                }
            })
        }
    }
    private val animationIdentitiesSetOff by lazy {
        AnimatorSet().also {
            it.duration = 250
            it.playTogether(animationIdentitiesTriangleOff, animationIdentitiesListOff, animationIdentitiesOverlayOff)
            it.addListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    identitiesVisible = false

                    identitiesTriangle?.visibility = View.GONE
                    identitiesSV?.visibility = View.GONE
                    overlay?.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
    }

    var identityListener: OnIdentitySwitchedListener? = null
    interface OnIdentitySwitchedListener {
        fun onIdentitySwitched(identityID: String)
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        onRestoreInstanceState(savedInstanceState ?: arguments)

        return inflater.inflate(R.layout.fragment_chat_rooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeAdapter()

        configureLayout(view)

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "VIEW_CREATED - isAdded: $isAdded")
    }

    override fun onStart() {
        super.onStart()

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "START - isAdded: $isAdded")
        configureResponseReceiver()
    }

    override fun onResume() {
        super.onResume()

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "RESUME - isAdded: $isAdded")

        (activity as? HomeActivity)?.backListener = this
        setLayout()

        callServer(CallType.LIST)
    }

    override fun onPause() {
        onSaveInstanceState(Bundle())

        Utils.closeKeyboard(chatRoomIncluded1.search_field)

        super.onPause()
    }

    override fun onBackPressed() {
        if (identitiesVisible == true) handleIdentitiesOverlay()
        else {
            (activity as? HomeActivity)?.backListener = null
            activity?.onBackPressed()
            (activity as? HomeActivity)?.backListener = this
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        identityListener = context as? OnIdentitySwitchedListener

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "ATTACH - isAdded: $isAdded")
    }

    override fun onDetach() {
        super.onDetach()

        identityListener = null

        if ((activity as? HomeActivity)?.backListener is ChatRoomsFragment)
            (activity as HomeActivity).backListener = null

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "DETACH - isAdded: $isAdded")
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedSearchString = savedInstanceState?.getString(Constants.EXTRA_PARAM_1)
        if (identitiesVisible != null)
            identitiesVisible = savedInstanceState?.getBoolean(Constants.EXTRA_PARAM_2, true)
    }

    override fun configureResponseReceiver() {
        if (serverMessageReceiver == null)
            serverMessageReceiver = ServerMessageReceiver()
        serverMessageReceiver.setListener(this)
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        super.handleSuccessResponse(operationId, responseObject)

        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)

        if (responseObject == null) {
            handleErrorResponse(operationId, Constants.SERVER_ERROR_GENERIC); return
        }

        when (operationId) {
            Constants.SERVER_OP_CHAT_UPDATE_LIST -> {

                realm.executeTransactionAsync {

                    ChatRoom.handleDeletedRooms(it, responseObject)

                    for (i in 0 until responseObject.length()) {
                        val room = ChatRoom.getRoom(responseObject.optJSONObject(i))
                        if (room.isValid()) {
                            it.insertOrUpdate(room)
                        }
                    }
                }
            }
            Constants.SERVER_OP_CHAT_DELETE_ROOM -> {
                if (!chatForDeletion.isNullOrBlank()) {
                    val room = RealmUtils.readFirstFromRealmWithId(realm, ChatRoom::class.java, "chatRoomID", chatForDeletion!!)
                    realm.executeTransaction {
                        RealmObject.deleteFromRealm(room)
                        RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", chatForDeletion!!).deleteAllFromRealm()

                        this@ChatRoomsFragment.chatRoomIncluded1?.no_result?.visibility = if (adapter?.itemCount == 0) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        super.handleErrorResponse(operationId, errorCode)

        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)

        when (operationId) { }
    }

    override fun onMissingConnection(operationId: Int) {
        (activity as? HLActivity)?.closeProgress()
        Utils.setRefreshingForSwipeLayout(srl, false)
    }

    private var count = 0
    override fun onConnectionEstablished(isChat: Boolean) {
        HLIdentity.performOpsAfterReconnection(context, isChat)

        if (++count == 2) {
            (context as? HLActivity)?.handleProgress(false)
            count = 0
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(Constants.EXTRA_PARAM_1, savedSearchString)

        if (identitiesVisible != null) {
            identitiesVisible = !(identitiesVisible!!)
            outState.putBoolean(Constants.EXTRA_PARAM_2, identitiesVisible!!)
        }
    }

    override fun configureLayout(view: View) {

        chatRoomIncluded1.generic_rv.layoutManager = llm
        chatRoomIncluded1.generic_rv.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        chatRoomIncluded1.no_result.setText(R.string.no_result_chat_rooms)

        srl = Utils.getGenericSwipeLayout(view) {
            Utils.setRefreshingForSwipeLayout(srl, true)
            callServer(CallType.LIST)
        }

        // TODO: 11/29/2018    searchLayout is DISABLED for now
        chatRoomIncluded1.search_box.visibility = View.GONE

        chatRoomIncluded1.search_field.hint = resources.getString(R.string.chat_rooms_search_hint)
        chatRoomIncluded1.search_field.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 0) adapter?.filter?.filter("")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        if (!savedSearchString.isNullOrBlank())
            chatRoomIncluded1.search_field.setText(savedSearchString)
        searchHelper.configureViews(chatRoomIncluded1.search_box, chatRoomIncluded1.search_field)

        createBtn.setOnClickListener {
            if (Utils.isContextValid(context))
                ChatActivity.openChatCreationFragment(context!!)
        }

        profilePicture.setOnClickListener {
            if (identitiesVisible == null) identitiesVisible = false
            handleIdentitiesOverlay()
        }
        overlay.setOnClickListener {
            // no op: just a click catcher
        }

    }

    override fun setLayout() {
        // INFO: 4/16/19    IDENTITIES can no longer chat (at least temporarily)
        val identities = mUser.identitiesOtherThanCurrentSelf
        if (/*identities.isNotEmpty()*/ false) {
            MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.context, mUser.avatarURL, profilePicture as ImageView)
            profilePicture?.visibility = View.VISIBLE
        }
        else profilePicture?.visibility = View.INVISIBLE

        createBtn?.visibility = if (!mUser.isActingAsInterest) View.VISIBLE else View.GONE

        chatRoomIncluded1.focus_catcher.requestFocus()
        chatRoomIncluded1.setPaddingRelative(0, 0, 0, 0)
        chatRoomIncluded1.generic_rv.adapter = adapter
        chatRoomIncluded1.generic_rv.visibility = if (adapter?.itemCount == 0) View.GONE else View.VISIBLE
        chatRoomIncluded1.no_result.visibility = if (adapter?.itemCount == 0) View.VISIBLE else View.GONE

        handleIdentitiesOverlay()

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "LAYOUT - isAdded: $isAdded")
    }


    //region == Class custom methods ==

    internal enum class CallType { LIST, DELETE /*SET_READ*/ }
    private fun callServer(type: CallType, roomId: String? = null, participantId: String? = null) {
        var result: Array<Any?>? = null

        when (type) {
            CallType.LIST -> {
                result = HLServerCallsChat.updateRoomsList(mUser.userId)
            }
            CallType.DELETE -> {
                if (!roomId.isNullOrBlank())
                    result = HLServerCallsChat.deleteRoom(mUser.userId, roomId)
            }

            // TODO: 11/4/2018    is it gonna be a setAsRead function?
//            CallType.SET_READ -> {
//                if (!roomId.isNullOrBlank() && !participantId.isNullOrBlank()
//                        (setIncomingRT || ChatRoom.areThereUnreadMessages(roomId, realm))
//                ) {
//                    if (!participantId.isNullOrBlank())
//                        result = HLServerCallsChat.setMessageRead(participantId!!, roomId)
//                }
//            }
        }

        HLRequestTracker.getInstance(activity?.application as? HLApp).handleCallResult(this, activity as? HLActivity, result, true, true)
    }

    private fun handleIdentitiesOverlay() {
        when (identitiesVisible) {
            null -> {
                // no op
            }
            true -> animationIdentitiesSetOff.start()
            false -> {
                identitiesContainer.removeAllViews()

                val identities = mUser.identitiesOtherThanCurrentSelf
                if (identities.isNotEmpty()) {
                    for (identity in identities) {

                        val row = LayoutInflater.from(context).inflate(R.layout.item_chat_identity, identitiesContainer, false) as View
                        row.setOnClickListener {

                            if (it.tag is HLIdentity) {
                                HLIdentity.switchIdentity(row.context, it.tag as HLIdentity, this@ChatRoomsFragment)
                                initializeAdapter()
                                setLayout()
                                handleIdentitiesOverlay()

                                identityListener?.onIdentitySwitched((it.tag as HLIdentity).id)
                            }
                        }

                        row.tag = identity

                        MediaHelper.loadProfilePictureWithPlaceholder(row.context, identity.avatarURL, row.profilePicture as ImageView)
                        row.identityName.text = identity.name

                        val count = ChatRoom.getAmountUnreadMessagesByIdentityID(identity.id, realm)
                        row.toReadCount.run {
                            visibility = if (count > 0) View.VISIBLE else View.GONE
                            text = count.toString()
                        }

                        identitiesContainer.addView(row)
                    }
                }

                animationIdentitiesSetOn.start()
            }
        }
    }

    private fun initializeAdapter() {
        adapter = null
        val rooms = if (savedSearchString.isNullOrBlank())
            (RealmUtils.readFromRealmWithIdSorted(realm, ChatRoom::class.java, "identityID", mUser.userId, "dateObj", Sort.DESCENDING) as RealmResults<ChatRoom>)
        else {
            realm.where(ChatRoom::class.java)
                    .equalTo("identityID", mUser.userId)
                    .contains("roomName", (savedSearchString as String).toLowerCase().trim(), Case.INSENSITIVE)
                    .sort("dateObj", Sort.DESCENDING)
                    .findAll()
        }
        adapter = ChatRoomsAdapter(rooms, this, this)
    }

    //endregion


    //region == Real time callbacks ==

    override fun onNewMessage(newMessage: ChatMessage) {
        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "MESSAGE - isAdded: $isAdded")

        val roomId = newMessage.chatRoomID
        if (!roomId.isNullOrBlank()) {
            val room = adapter?.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, roomId)?.second

            if (room?.isValid() == true && Utils.isContextValid(activity)) {
                realm.executeTransaction {
                    room.text = when {
                        newMessage.hasAudio() -> getString(R.string.chat_room_first_line_audio_in)
                        newMessage.hasVideo() -> getString(R.string.chat_room_first_line_video_in)
                        newMessage.hasPicture() -> getString(R.string.chat_room_first_line_picture_in)
                        newMessage.hasLocation() -> getString(R.string.chat_room_first_line_location_in)
                        newMessage.hasDocument() -> getString(R.string.chat_room_first_line_document_in)
                        newMessage.hasWebLink() -> {
                            "" //TODO  change with server UNICODE
                        }
                        else -> newMessage.text
                    }
                    room.dateObj = newMessage.creationDateObj
                }
            }
        }
    }

    override fun onStatusUpdated(userId: String, status: Int, date: String) {

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "STATUS - isAdded: $isAdded")

        val room = adapter?.getRoomByID(ChatRoomsAdapter.FilterBy.PARTICIPANT, userId)?.second

        realm.executeTransaction {
            room?.participants?.get(0)?.lastSeenDate = date
            room?.participants?.get(0)?.chatStatus = status
            room?.recipientStatus = status
        }
    }

    private var activitiesMap = mutableMapOf<String, String>()
    override fun onActivityUpdated(userId: String, chatId: String, activity: String) {

        // TODO: 4/5/19    REMOVE
        LogUtils.d(LOG_TAG, "ACTIVITY - isAdded: $isAdded")

        val room = adapter?.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, chatId)?.second
        val previousActivity = activitiesMap[chatId]

        realm.executeTransaction {
            if (!activity.isBlank()) {
                activitiesMap[chatId] = room?.text ?: ""
                room?.text = activity
            }
            else if (!previousActivity.isNullOrBlank())
                room?.text = previousActivity
        }
    }

    override fun onMessageDelivered(chatId: String, userId: String, date: String) {}

    override fun onMessageRead(chatId: String, userId: String, date: String) {}

    override fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String) {}

    //endregion


    //region == Search listener ==

    override fun onQueryReceived(query: String) {
//        adapter.filter.filter(query)
//        {
//            chatRoomIncluded1.no_result.setText(
//                    if (query.isBlank()) R.string.no_result_chat_rooms
//                    else R.string.no_result_chat_rooms_search
//            )
//            chatRoomIncluded1.no_result.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
//            chatRoomIncluded1.generic_rv.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
//        }

        savedSearchString = query
        adapter?.filter?.filter(query)
//        setAdapter(query)
    }

    //endregion

    //region == Delete room listener ==

    override fun onRoomDeleted(chatID: String) {
        chatForDeletion = chatID
        callServer(CallType.DELETE, chatID)
    }

    //endregion


    //region == Getters and setters ==

    fun getUser(): HLUser {
        return mUser
    }

    fun getRealm(): Realm {
        return realm
    }

    //endregion

}