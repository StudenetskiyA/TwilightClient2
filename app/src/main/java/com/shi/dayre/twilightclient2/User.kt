package com.shi.dayre.twilightclient2

// Created by StudenetskiyA on 05.01.2018.

data class User(
       // var userText: String = "",
        var latitude: Double? = null,
        var longitude: Double? = null,
        var login: String? = null,
        var password: String? = null,
        var url:String = "",
        var superusered:Int=-1,
        var zoneText:String = "",
        var searchUserResult:ArrayList<SearchUserResult> = ArrayList(),
        var searchZoneResult:ArrayList<SearchZoneResult> = ArrayList(),
        var vampireSend:String ="0",
        var vampireCall:String= "0",
       // var interfaceStyle:Int = 0,
        var mail:ArrayList<String> = ArrayList()
)