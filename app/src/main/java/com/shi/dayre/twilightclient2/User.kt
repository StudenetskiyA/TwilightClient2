package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by samsung on 05.01.2018.
 */
data class User(
    var userText:String="",
    var gpsLocation:String="",
    var netLocation:String="",
    var location:Location?=null,
    var locationNet:Location?=null
){
    fun getBestLocation():Location?{
        if (location!=null) return location else return locationNet
    }
}