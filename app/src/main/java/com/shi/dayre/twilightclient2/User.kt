package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by StudenetskiyA on 05.01.2018.
 */

data class User(
        var userText: String = "",
        var gpsLocation: String = "",
        var netLocation: String = "",
        var location: Location? = null,
        var locationNet: Location? = null,
        var login: String? = null,
        var password: String? = null,
        var server:String = DEFAULT_SERVER,
        var logined:Boolean = false,
        var justLogined:Boolean=true,
        var superusered:Boolean=false,
        var zoneText:String = "",
        var justChangedZone:Boolean = false
) {
    fun getBestLocation(): Location? {
        if (location != null) return location else return locationNet
    }
}