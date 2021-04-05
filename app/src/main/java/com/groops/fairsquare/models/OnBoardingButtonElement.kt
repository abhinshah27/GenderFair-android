package com.groops.fairsquare.models

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.groops.fairsquare.utility.helpers.JsonHelper
import org.json.JSONObject

class OnBoardingButtonElement: JsonHelper.JsonDeSerializer {

    companion object {
        fun get(json: JSONObject): OnBoardingButtonElement {
            return JsonHelper.deserialize(json, OnBoardingButtonElement::class.java) as OnBoardingButtonElement
        }
    }

    @SerializedName(value = "_id")
    var id: String? = null
    var name: String? = null
    var avatarURL: String? = null

    var selected = false


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
}