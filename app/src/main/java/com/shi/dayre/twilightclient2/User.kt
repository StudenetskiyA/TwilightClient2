package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by StudenetskiyA on 05.01.2018.
 */

data class User(
        var userText: String = "",
        var gpsLocation: String = "",
        var netLocation: String = "",
        var locationLatitude: Double? = null,
        var locationLongitude: Double? = null,
        var locationNetLatitude: Double? = null,
        var locationNetLongitude: Double? = null,
        var login: String? = null,
        var password: String? = null,
        var server:String = DEFAULT_SERVER,
        var logined:Boolean = false,
        var justLogined:Boolean=true,
        var superusered:Int=-1,
        var zoneText:String = "",
        var justChangedZone:Boolean = false,
        var searchUserResult:ArrayList<SearchUserResult> = ArrayList(),
        var searchZoneResult:ArrayList<SearchZoneResult> = ArrayList(),
        var vampireSend:String ="0",
        var vampireCall:String= "0",
        var infoSearch:Boolean = false,
        var interfaceStyle:Int = 0,
        var mail:ArrayList<String> = ArrayList()
) {
    fun getBestLocationLatitude(): Double? {
        if (locationLatitude != null) return locationLatitude else return locationNetLatitude
    }
    fun getBestLocationLongitude(): Double? {
        if (locationLongitude != null) return locationLongitude else return locationNetLongitude
    }
}