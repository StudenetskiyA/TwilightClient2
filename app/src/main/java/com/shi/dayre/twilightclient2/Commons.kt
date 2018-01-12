package com.shi.dayre.twilightclient2

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.location.Location
import android.support.v4.app.NotificationCompat
import android.media.RingtoneManager
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.app.Activity
import android.view.inputmethod.InputMethodManager
import java.util.regex.Pattern


/**
 * Created by StudenetskiyA on 06.01.2018.
 */

val METERINGRADUS = 10000

fun addCircleToMap(map: GoogleMap?, lat:Double, lon:Double, radius:Double, color:Int){
    map?.addCircle(CircleOptions()
            .center(LatLng(lat, lon))
            .radius(radius)
            .strokeColor(color)
    )
}
fun addMarkerToMap(map: GoogleMap?, name: String, lat: Double, lon: Double, snip: String, resource: Resources, icon: Int) {
    if (map != null) {
        Log.i("Webclient", "Try to add point on map:" + lat + "," + lon)
        var marker: MarkerOptions = MarkerOptions()
                .position(LatLng(lat, lon))
                .title(name)
                .snippet(snip)
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resource, icon)))
        map.addMarker(marker)
    }
}

fun hideSoftKeyboard(activity: Activity) {
    val inputMethodManager = activity.getSystemService(
            Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(
            activity.currentFocus?.windowToken, 0)
}
enum class PowerSide {
    Human, Light, Dark;

    fun toInt(): Int {
        return if (this == Light)
            1
        else if (this == Dark)
            2
        else
            0
    }
}

fun String.toPowerside(): PowerSide {
    return if (this.equals("0"))
        PowerSide.Human
    else if (this.equals("1"))
        PowerSide.Light
    else
        PowerSide.Dark
}

fun Location.format(): String {
    return if (this == null) "" else "lat = "+this.latitude.toDouble()+","+ "lon = "+this.longitude.toDouble()
}

fun String.getTextBetween(): ArrayList<String> {
    var fromText = this
    val rtrn = ArrayList<String>()
    val beforeText = "("
    fromText = fromText.substring(fromText.indexOf(beforeText) + 1, fromText.indexOf(")"))
    val par = fromText.split(Pattern.quote(COMMA).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (i in par.indices) {
        println("Par : " + par[i])
        rtrn.add(par[i])
    }
    return rtrn
}
fun sendNotification(context: Context, title: String, body: String){
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notification = NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.notification_icon_background)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
    //      .setLargeIcon(

    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT)

    notification.setContentIntent(pendingIntent)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(1, notification.build())
}
