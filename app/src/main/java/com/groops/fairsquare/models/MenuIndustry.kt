package com.groops.fairsquare.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.groops.fairsquare.base.HLModel
import com.groops.fairsquare.utility.helpers.JsonHelper
import org.json.JSONObject

class MenuIndustry(parcel: Parcel) : Comparable<MenuIndustry>, JsonHelper.JsonDeSerializer, HLModel(),
        Parcelable {

    companion object CREATOR: Parcelable.Creator<MenuIndustry>  {

        fun get(json: JSONObject?): MenuIndustry {
            val item = JsonHelper.deserialize(json, MenuIndustry::class.java) as MenuIndustry

            return item
        }

        override fun createFromParcel(`in`: Parcel): MenuIndustry {
            return MenuIndustry(`in`)
        }

        override fun newArray(size: Int): Array<MenuIndustry?> {
            return arrayOfNulls(size)
        }
    }

    @SerializedName(value = "_id")
    var id: String? = null
    var name: String? = null
    var avatarURL: String? = null


    override fun hashCode(): Int {
        return id?.hashCode() ?: super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return id != null &&
                (other as? MenuIndustry)?.id != null &&
                id == other.id
    }

    override fun compareTo(other: MenuIndustry): Int {
       return if (!name.isNullOrBlank() && !other.name.isNullOrBlank()) name!!.compareTo(other.name!!) else 0
    }

    //region == Parcelable interface ==

    init {
        this.id = parcel.readString()
        this.name = parcel.readString()
        this.avatarURL = parcel.readString()
        this.avatarURL = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.name)
        dest.writeString(this.avatarURL)
    }

    //endregion



    //region == Basic model implementation ==

    override fun update(`object`: Any?) {
        if (`object` is MenuIndustry) {
            id = `object`.id
            name = `object`.name
            avatarURL = `object`.avatarURL
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