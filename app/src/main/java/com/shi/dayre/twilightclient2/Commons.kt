package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by StudenetskiyA on 06.01.2018.
 */

fun Location.format(): String {
    return if (this == null) "" else "lat = "+this.latitude.locationToInt()+","+ "lon = "+this.longitude.locationToInt()
}

fun Double?.locationToInt():Int{
    if (this!=null)
    return (this*1000000).toInt()
    else return 0
}

fun String.getTextBetween(): ArrayList<String> {
    var fromText = this
    val rtrn = ArrayList<String>()
    val beforeText = "("
    fromText = fromText.substring(fromText.indexOf(beforeText) + 1, fromText.indexOf(")"))
    val par = fromText.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (i in par.indices) {
        println("Par : " + par[i])
        rtrn.add(par[i])
    }
    return rtrn
}
