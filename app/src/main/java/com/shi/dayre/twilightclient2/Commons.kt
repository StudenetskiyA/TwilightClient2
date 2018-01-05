package com.shi.dayre.twilightclient2

import android.location.Location

/**
 * Created by StudenetskiyA on 06.01.2018.
 */

fun Location.format(): String {
    return if (this == null) "" else "lat = "+this.latitude.locationToInt()+","+ "lon = "+this.longitude.locationToInt()
}

fun Double.locationToInt():Int{
    return (this*1000000).toInt()
}
