package com.shi.dayre.twilightclient2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.support.v4.app.NotificationCompat
import android.media.RingtoneManager

/**
 * Created by StudenetskiyA on 06.01.2018.
 */

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
    val par = fromText.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
