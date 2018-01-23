package com.shi.dayre.twilightclient2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.os.Build
import android.util.Log
import android.content.Intent
import android.os.IBinder
import xdroid.toaster.Toaster
import java.util.*


/**
 * Created by StudenetskiyA on 23.01.2018.
 */

class ForegroundLocationService : Service() {
    var timer = Timer()
    //lateinit var location: com.shi.dayre.twilightclient2.LocationProvider

    override fun onCreate() {
       val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(applicationContext,
                777, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = Notification.Builder(this)
                .setSmallIcon(R.drawable.yinyan)
                .setContentTitle(getString(R.string.twilightWatchYou))
                .setContentIntent(contentIntent)
        val notification: Notification

        if (Build.VERSION.SDK_INT < 16)
            notification = builder.getNotification()
        else {
            notification = builder.build()
        }

        Log.i("TLC.service", "onCreateCommand")
        startService()
        startForeground(777, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startService() {
        timer.schedule(mainTask(), 0, CONNECT_EVERY_SECOND * 1000);
    }

    private inner class mainTask : TimerTask() {
        override fun run() {
            sendLocationToServer()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("TLC.service", "onStartCommand")
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("TLC.service", "onDestroy")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.i("TLC.service", "onTaskRemoved")
    }

    fun sendLocationToServer() {
        val lat: Double? = if (user.getBestLocation()?.latitude != null) user.getBestLocation()?.latitude else 0.0
        val lon: Double? = if (user.getBestLocation()?.longitude != null) user.getBestLocation()?.longitude else 0.0

        //Remove this check after app connect only after loging
        if (user.login != null && !user.login.equals("null") && user.password != null && !user.password.equals("null")) {
            var msg = "USER(" + user.login + COMMA + user.password + COMMA + lat + COMMA + lon + ")"
            wsj?.sendMessage(msg)
        }
    }
}