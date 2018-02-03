package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by StudenetskiyA on 10.01.2018.
 */
class SearchUserResult(val name:String,val latitude:Double,val longitude:Double,val powerSide:PowerSide,val lastConnected:String, val curse:String) {

    constructor(s:SearchUserResult) : this(s.name,s.latitude,s.longitude,s.powerSide,s.lastConnected,s.curse) {

    }
}

class SearchZoneResult(val name:String,val latitude:Double,val longitude:Double,val radius:Double,val priority:Int,val textForHuman:String) {

}