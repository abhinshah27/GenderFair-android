package com.groops.fairsquare.models

import com.google.gson.JsonElement
import com.groops.fairsquare.base.HLModel
import com.groops.fairsquare.utility.helpers.JsonHelper
import org.json.JSONObject

class FeedChannel: Comparable<FeedChannel>, JsonHelper.JsonDeSerializer, HLModel() {

    companion object {
        fun getChannel(json: JSONObject?): FeedChannel? {
            val channel = JsonHelper.deserialize(json, FeedChannel::class.java) as FeedChannel

            return channel
        }
    }

    /**
     * The ObjectId of the Interest in the db.
     */
    var userID: String? = null
    var name: String? = null
    var avatarURL: String? = null
    var idx: Int = 0
    var oldIdx: Int = 0


    override fun hashCode(): Int {
        return userID?.hashCode() ?: super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return userID != null &&
                (other as? FeedChannel)?.userID != null &&
                userID == other.userID
    }

    override fun compareTo(other: FeedChannel): Int {
       return idx.compareTo(other.idx)
    }


    //region == Basic model implementation ==

    override fun update(`object`: Any?) {
        if (`object` is FeedChannel) {
            name = `object`.name
            avatarURL = `object`.avatarURL
            idx = `object`.idx
            userID = `object`.userID
            oldIdx = `object`.oldIdx
        }
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