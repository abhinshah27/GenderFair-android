/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.models.chat

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.groops.fairsquare.models.HLUser
import com.groops.fairsquare.models.RealmModelListener
import com.groops.fairsquare.utility.Utils
import com.groops.fairsquare.utility.helpers.JsonHelper
import com.groops.fairsquare.utility.realm.RealmUtils
import com.groops.fairsquare.websocket_connection.HLServerCallsChat
import io.realm.*
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*

@RealmClass
open class ChatParticipant: RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer,
        Serializable, RealmChangeListener<ChatParticipant>, Comparable<ChatParticipant> {

    companion object {
        fun getParticipant(json: JSONObject?): ChatParticipant {
            val participant = JsonHelper.deserialize(json, ChatParticipant::class.java) as ChatParticipant
            return participant
        }
    }

    /**
     * Enum class representing all the potential statuses of a chat recipient.
     */
    enum class Status(val value: Int) { OFFLINE(0), ONLINE(1) }

    var userID: String? = null
    var name: String? = null
    var avatarURL: String? = null

    @SerializedName("pstatus")
    var chatStatus = Status.OFFLINE.value
    var lastSeenDate: String? = null

    @Index
    var canChat: Boolean = false
    var canVideocall: Boolean = false
    var canAudiocall: Boolean = false


    override fun compareTo(other: ChatParticipant): Int {
        return name?.compareTo(other.name ?: "") ?: 0
    }

    override fun equals(other: Any?): Boolean {
        return (!userID.isNullOrBlank() && !(other as? ChatParticipant)?.userID.isNullOrBlank() && userID == (other as ChatParticipant).userID)
    }


    fun getChatStatusEnum(): Status {
        return if (chatStatus == Status.ONLINE.value) Status.ONLINE
        else Status.OFFLINE
    }


    //region == Realm listener ==

    override fun onChange(t: ChatParticipant) {
        update(t)
    }

    override fun reset() {}

    override fun read(realm: Realm?): Any? {
        return null
    }

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? {
        return null
    }

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {
        RealmUtils.writeToRealm(realm, this)
    }

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {
        if (`object` is ChatParticipant) {
            userID = `object`.userID
            name = `object`.name
            avatarURL = `object`.avatarURL
            chatStatus = `object`.chatStatus
            lastSeenDate = `object`.lastSeenDate
            canChat = `object`.canChat
            canVideocall = `object`.canVideocall
            canAudiocall = `object`.canAudiocall
        }
    }

    override fun update(json: JSONObject?) {
        deserialize(json, ChatMessage::class.java)
    }

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener {
        update(`object`)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString(), ChatMessage::class.java) as RealmModelListener
    }

    //endregion


    //region == Json De-Serialization ==

    override fun serializeWithExpose(): JsonElement {
        return JsonHelper.serializeWithExpose(this)
    }

    override fun serializeToStringWithExpose(): String {
        return JsonHelper.serializeToStringWithExpose(this)
    }

    override fun serialize(): JsonElement {
        return JsonHelper.serialize(this)
    }

    override fun serializeToString(): String {
        return JsonHelper.serializeToString(this)
    }

    override fun deserialize(json: JSONObject?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(json: JsonElement?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(jsonString: String?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(jsonString, myClass)
    }

    override fun getSelfObject(): Any {
        return this
    }

    //endregion
}

/**
 * Class representing the instance of a chat room.
 * @author mbaldrighi on 10/26/2018.
 */
@RealmClass
open class ChatRoom(var ownerID: String? = null,
                    var participantIDs: RealmList<String> = RealmList(),
                    @PrimaryKey var chatRoomID: String? = null):
        RealmModel, RealmModelListener, JsonHelper.JsonDeSerializer, Serializable, RealmChangeListener<ChatRoom> {

    companion object {
        @JvmStatic fun getRoom(json: JSONObject?): ChatRoom {
            val room = JsonHelper.deserialize(json, ChatRoom::class.java) as ChatRoom
            room.recipientStatus = room.participants[0]?.chatStatus ?: ChatParticipant.Status.OFFLINE.value
            for (it in room.participants)
                room.participantIDs.add(it.userID)

            room.dateObj = Utils.getDateFromDB(room.date)
            room.roomName = room.getRoomName()

            return room
        }

        fun getRightTimeStamp(chatRoomId: String, realm: Realm, direction: HLServerCallsChat.FetchDirection): Long? {

            val messages = RealmUtils.readFromRealmWithIdSorted(realm, ChatMessage::class.java, "chatRoomID", chatRoomId, "creationDateObj", Sort.ASCENDING) as RealmResults<ChatMessage>

            return if (direction == HLServerCallsChat.FetchDirection.BEFORE) {
                messages[0]?.unixtimestamp
            }
            else {
                return when {
                    messages.isEmpty() -> 0

                    !messages.isEmpty() -> {
                        var result = 0L
                        // from the newest message down to the oldest store [ChatMessage.unixtimestamp] if message isn't READ
                        for (it in (messages.size - 1) downTo 0) {
                            val message = messages[it]
                            if (message?.isRead() == false && !message.isError) {
                                if (message.unixtimestamp != null) result = message.unixtimestamp!!
                            }
                            else break
                        }

//                        if (result == 0L) {
//                            result = (messages[0] as ChatMessage).unixtimestamp!!; result
//                        } else
                        result
                    }

                    else -> 0
                }
            }
        }

        // FIXME: 4/1/19    change logic for method using single identity
        fun areThereUnreadMessages(identityID: String?, chatRoomId: String? = null, realm: Realm): Boolean {
            return if (!chatRoomId.isNullOrBlank()) {
                val messages = RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", chatRoomId)
                val incoming = messages.filter { (it as ChatMessage).getDirectionType(HLUser().readUser(realm).userId) == ChatMessage.DirectionType.INCOMING }
                val filtered = incoming.filter { (it as ChatMessage).getStatusEnum() == ChatMessage.Status.READ }
                filtered.isNotEmpty()
            }
            else {
                var thereAreMessages = false
                if (!identityID.isNullOrBlank()) {
                    RealmUtils.readFromRealm(realm, ChatRoom::class.java).forEach {
                        if (it is ChatRoom) {
                            if (it.toRead > 0) {
                                thereAreMessages = true; return@forEach
                            }
                        }
                    }
                }

                thereAreMessages
            }
        }

        /**
         * Checks whether there are some rooms that need to be deleted from the local DB.
         * To be used already inside a Realm transaction.
         */
        fun handleDeletedRooms(realm: Realm, jsonArray: JSONArray?) {

            val userId = HLUser().readUser(realm).userId

            when {
                jsonArray == null -> return
                jsonArray.length() == 0 -> {
                    val rooms = RealmUtils.readFromRealmWithId(realm, ChatRoom::class.java, "identityID", userId)
                    if (rooms != null && !rooms.isEmpty()) {
                        rooms.forEach { room ->
                            val id = (room as? ChatRoom)?.chatRoomID
                            if (!id.isNullOrBlank()) {
                                RealmObject.deleteFromRealm(room)
                                RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", id).deleteAllFromRealm()
                            }
                        }
                    }
                    return
                }
                else -> {
                    val rooms = RealmUtils.readFromRealmWithId(realm, ChatRoom::class.java, "identityID", userId)
                    if (rooms != null && !rooms.isEmpty()) {
                        rooms.forEach { room ->
                            val id = (room as? ChatRoom)?.chatRoomID
                            if (!id.isNullOrBlank()) {
                                var delete = true
                                for (i in 0 until jsonArray.length()) {
                                    val jID = jsonArray.optJSONObject(i)?.optString("chatRoomID", "")
                                    val identityID = jsonArray.optJSONObject(i)?.optString("identityID", "")
                                    if (id == jID || userId != identityID) { delete = false; break }
                                }

                                if (delete) {
                                    RealmObject.deleteFromRealm(room)
                                    RealmUtils.readFromRealmWithId(realm, ChatMessage::class.java, "chatRoomID", id).deleteAllFromRealm()
                                }
                            }
                        }
                    }
                }
            }
        }

        @JvmStatic fun checkRoom(chatRoomID: String?, realm: Realm?): Pair<Boolean, ChatRoom?> {
            if (!chatRoomID.isNullOrBlank() && RealmUtils.isValid(realm)) {
                // check if I have the same room
                val room = RealmUtils.readFirstFromRealmWithId(
                        realm,
                        ChatRoom::class.java,
                        "chatRoomID",
                        chatRoomID
                ) as? ChatRoom

                return (room?.isValid() == true) to room
            }

            return false to null
        }

        fun getAmountUnreadMessagesByIdentityID(identityId: String? = null, realm: Realm): Int {
            return if (!identityId.isNullOrBlank()) {
                var count = 0
                RealmUtils.readFromRealmWithId(realm, ChatRoom::class.java, "identityID", identityId)
                        .forEach {
                            if (it is ChatRoom) {
                                count += it.toRead
                            }
                        }
                return count
            }
            else 0
        }

    }

    var date: String = Utils.formatDateForDB(Date())
    var dateObj: Date? = null
    var text: String = ""
    var recipientStatus: Int = 0
    var participants = RealmList<ChatParticipant>()
    var roomName: String? = null
    var toRead: Int = 0

    @Index var identityID: String? = null



    //region == Class custom methods ==

    fun getRecipientStatus(): ChatParticipant.Status {
        return when (recipientStatus) {
            0 -> ChatParticipant.Status.OFFLINE
            1 -> ChatParticipant.Status.ONLINE
            else -> ChatParticipant.Status.OFFLINE
        }
    }

    fun isValid(): Boolean {
        return !chatRoomID.isNullOrBlank()/* && !ownerID.isNullOrBlank() && participantIDs.isNotEmpty()*/
    }

    fun getRoomAvatar(id: String? = null): String? {
        return getParticipant(id)?.avatarURL
    }

    fun getRoomName(id: String? = null): String? {
        return getParticipant(id)?.name
    }

    fun getLastSeenDate(id: String? = null): Date? {
        return Utils.getDateFromDB(getParticipant(id)?.lastSeenDate)
    }

    fun getParticipantId(only: Boolean = true): String? {
        return if (!participantIDs.isNullOrEmpty() && only) participantIDs[0] else null
    }

    fun canVoiceCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canAudiocall ?: false
    }

    fun canVideoCallParticipant(id: String? = null): Boolean {
        return getParticipant(id)?.canVideocall ?: false
    }

    fun getParticipant(id: String? = null): ChatParticipant? {
        return if (!participants.isNullOrEmpty()) {
            if (id == null) participants[0]
            else {
                val filtered = participants.filter { it.userID == id }
                if (!filtered.isNullOrEmpty() && filtered.size == 1)
                    filtered[0]
                else null
            }
        } else null
    }

    //endregion


    //region == Realm listener ==

    override fun onChange(t: ChatRoom) {
        update(t)
    }

    override fun reset() {}

    override fun read(realm: Realm?): Any? {
        return null
    }

    override fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel? {
        return null
    }

    override fun deserializeStringListFromRealm() {}

    override fun serializeStringListForRealm() {}

    override fun write(realm: Realm?) {
        RealmUtils.writeToRealm(realm, this)
    }

    override fun write(`object`: Any?) {}

    override fun write(json: JSONObject?) {
        update(json)
    }

    override fun write(realm: Realm?, model: RealmModel?) {}

    override fun update() {}

    override fun update(`object`: Any?) {
        if (`object` is ChatRoom) {
            chatRoomID = `object`.chatRoomID
            ownerID = `object`.ownerID
            participantIDs = `object`.participantIDs
            participants = `object`.participants
            date = `object`.date
            recipientStatus = `object`.recipientStatus
            toRead = `object`.toRead
        }
    }

    override fun update(json: JSONObject?) {
        deserialize(json, ChatMessage::class.java)
    }

    override fun updateWithReturn(): RealmModelListener? {
        return null
    }

    override fun updateWithReturn(`object`: Any?): RealmModelListener {
        update(`object`)
        return this
    }

    override fun updateWithReturn(json: JSONObject?): RealmModelListener {
        return deserialize(json.toString(), ChatMessage::class.java) as RealmModelListener
    }

    //endregion



    //region == Json De-Serialization ==

    override fun serializeWithExpose(): JsonElement {
        return JsonHelper.serializeWithExpose(this)
    }

    override fun serializeToStringWithExpose(): String {
        return JsonHelper.serializeToStringWithExpose(this)
    }

    override fun serialize(): JsonElement {
        return JsonHelper.serialize(this)
    }

    override fun serializeToString(): String {
        return JsonHelper.serializeToString(this)
    }

    override fun deserialize(json: JSONObject?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(json: JsonElement?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(json, myClass)
    }

    override fun deserialize(jsonString: String?, myClass: Class<*>?): JsonHelper.JsonDeSerializer {
        return JsonHelper.deserialize(jsonString, myClass)
    }

    override fun getSelfObject(): Any {
        return this
    }

    //endregion

}