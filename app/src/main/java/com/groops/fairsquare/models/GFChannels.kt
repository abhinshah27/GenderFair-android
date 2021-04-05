package com.groops.fairsquare.models

import com.groops.fairsquare.utility.realm.RealmUtils
import io.realm.Realm
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object GFChannels {

    private val channelsMap = ConcurrentHashMap<String, FeedChannel>()

    var currentSelectedChannel: String? = null
        get() {
            var realm: Realm? = null
            var channel: String? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                channel = HLUser().readUser(realm).selectedFeedChannel
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                RealmUtils.closeRealm(realm)
                return field ?: channel
            }
        }
        set(value) {

            field = value

            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                realm?.executeTransaction {
                    HLUser().readUser(it).selectedFeedChannel = value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                RealmUtils.closeRealm(realm)
            }
        }


    fun setChannels(json: JSONArray?, fromGet: Boolean = true) {
        if (json != null) {
            if (fromGet) channelsMap.clear()
            for (i in 0 until json.length()) {
                setSingleChannel(json.optJSONObject(i))
            }
        }
    }

    private fun setSingleChannel(json: JSONObject?) {
        val newChannel = FeedChannel.getChannel(json)
        if (newChannel != null && !newChannel.userID.isNullOrBlank()) {
            val oldChannel = channelsMap[newChannel.userID as String]
            if (oldChannel == null) {
                channelsMap[newChannel.userID as String] = newChannel
            } else {
                oldChannel.update(newChannel)
            }
        }
    }

    fun deleteChannel(id: String?) {
        channelsMap.remove(id)
    }


    fun getUpdatedChannels(): Pair<List<FeedChannel>, Int> {
        val list = channelsMap.values.sorted()
        val position = if (currentSelectedChannel != null) list.indexOf(channelsMap[currentSelectedChannel!!]) else -1

        return list to (if (position > -1) position else 0)
    }


    fun getChannelNameByID(id: String?): String? {
        return if (!id.isNullOrBlank()) {
            val channel = channelsMap[id]
            channel?.name
        } else null
    }


    // INFO: 3/25/19    TEST
    fun createTestList(): JSONArray {

        val array = JSONArray()

        array.put(JSONObject()
                .put("name", "int_Q1422")
                .put("avatarURL", "http://ec2-34-201-111-17.compute-1.amazonaws.com/GetMedia.aspx?name=5aad9e0f115e194fdcd150ea")
                .put("sortOrder", 0)
        )
        array.put(JSONObject()
                .put("name", "int_Q745871")
                .put("avatarURL", "http://ec2-34-201-111-17.compute-1.amazonaws.com/GetMedia.aspx?name=5aad9e17115e194fdcd150ed")
                .put("sortOrder", 1)
        )
        array.put(JSONObject()
                .put("name", "int_Q5379225")
                .put("avatarURL", "https://hl-media-storage.s3.amazonaws.com/5a7c52cf115e191d50de91c6/avatarScaled/ea5e355c-cba6-4c23-8b24-09c9a8da8569.png")
                .put("sortOrder", 2)
        )
        array.put(JSONObject()
                .put("name", "int_Q5268822")
                .put("avatarURL", "http://ec2-34-201-111-17.compute-1.amazonaws.com/GetMedia.aspx?name=5aad9e2c115e194fdcd150f6")
                .put("sortOrder", 3)
        )
        array.put(JSONObject()
                .put("name", "int_Q5041435")
                .put("avatarURL", "http://ec2-34-201-111-17.compute-1.amazonaws.com/GetMedia.aspx?name=5aad9e33115e194fdcd150f9")
                .put("sortOrder", 4)
        )
        array.put(JSONObject()
                .put("name", "int_Q3494889")
                .put("avatarURL", "http://ec2-34-201-111-17.compute-1.amazonaws.com/GetMedia.aspx?name=5aad9e3a115e194fdcd150fc")
                .put("sortOrder", 5)
        )
        array.put(JSONObject()
                .put("name", "int_Q2006428")
                .put("avatarURL", "https://hl-media-storage.s3.amazonaws.com/5a7c52c8115e191d50de91c3/avatarScaled/5986399a-531f-414b-b42e-c4e0937ffaac.jpg")
                .put("sortOrder", 6)
        )

        return array

    }


}