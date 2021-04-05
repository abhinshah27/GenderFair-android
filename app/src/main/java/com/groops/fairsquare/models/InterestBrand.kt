package com.groops.fairsquare.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.groops.fairsquare.base.HLModel
import com.groops.fairsquare.utility.helpers.JsonHelper
import org.json.JSONObject

class InterestBrand(parcel: Parcel) : Comparable<InterestBrand>, JsonHelper.JsonDeSerializer, HLModel(),
        Parcelable/*, Scorable*/ {

    companion object CREATOR: Parcelable.Creator<InterestBrand>  {

        fun get(json: JSONObject?): InterestBrand {
            val brand = JsonHelper.deserialize(json, InterestBrand::class.java) as InterestBrand

            return brand
        }

        override fun createFromParcel(`in`: Parcel): InterestBrand {
            return InterestBrand(`in`)
        }

        override fun newArray(size: Int): Array<InterestBrand?> {
            return arrayOfNulls(size)
        }
    }

    @SerializedName(value = "_id")
    var id: String? = null
    var name: String? = null
    var avatarURL: String? = null
    var wallImageLink: String? = null
    var score: String? = null


    override fun hashCode(): Int {
        return id?.hashCode() ?: super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return id != null &&
                (other as? InterestBrand)?.id != null &&
                id == other.id
    }

    override fun compareTo(other: InterestBrand): Int {
       return if (!name.isNullOrBlank() && !other.name.isNullOrBlank()) name!!.compareTo(other.name!!) else 0
    }

    // INFO: 2019-05-07    Scorable no longer implemented
//    override fun getScore(): String? {
//        return score
//    }


    //region == Parcelable interface ==

    init {
        this.id = parcel.readString()
        this.name = parcel.readString()
        this.avatarURL = parcel.readString()
        this.wallImageLink = parcel.readString()
        this.avatarURL = parcel.readString()
        this.score = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.name)
        dest.writeString(this.avatarURL)
        dest.writeString(this.wallImageLink)
        dest.writeString(this.score)
    }

    //endregion



    //region == Basic model implementation ==

    override fun update(`object`: Any?) {
        if (`object` is InterestBrand) {
            id = `object`.id
            name = `object`.name
            avatarURL = `object`.avatarURL
            wallImageLink = `object`.wallImageLink
            score = `object`.score
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