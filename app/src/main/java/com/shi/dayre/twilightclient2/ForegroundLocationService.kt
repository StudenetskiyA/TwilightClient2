package com.shi.dayre.twilightclient2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.os.Build
import android.util.Log
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import xdroid.toaster.Toaster
import java.util.*


/**
 * Created by StudenetskiyA on 23.01.2018.
 */

class ForegroundLocationService : Service() {
    var timer = Timer()
    lateinit var mSettings: SharedPreferences
    lateinit var location: com.shi.dayre.twilightclient2.LocationProvider

    override fun onCreate() {
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startService() {
        location.start()
        timer.schedule(timerTickTask(), 0, CONNECT_EVERY_SECOND * 1000);
    }

    private inner class timerTickTask : TimerTask() {
        override fun run() {
            sendLocationToServer()
           // MainActivity().refresh()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("TLC.service", "onStartCommand")

        mSettings = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        location = com.shi.dayre.twilightclient2.LocationProvider(this, MainActivity())

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
        var login:String? = null
        var password:String? = null
        var latN:Double? = null
        var lat:Double? = null
        var lonN:Double?= null
        var lon:Double?= null

       // val lat: Double? = if (user.getBestLocation()?.latitude != null) user.getBestLocation()?.latitude else 0.0
       // val lon: Double? = if (user.getBestLocation()?.longitude != null) user.getBestLocation()?.longitude else 0.0

        if (mSettings.contains(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LATITUDE_GPS)) {
            lat = mSettings.getFloat(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LATITUDE_GPS, 0f).toDouble()
            lon = mSettings.getFloat(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LONGITUDE_GPS, 0f).toDouble()
        }
        if (mSettings.contains(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LATITUDE_NET)) {
            latN = mSettings.getFloat(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LATITUDE_NET, 0f).toDouble()
            lonN = mSettings.getFloat(com.shi.dayre.twilightclient2.APP_PREFERENCES_LAST_LONGITUDE_NET, 0f).toDouble()
        }

        if (lat==null) lat=latN
        if (lon==null) lon=lonN

        if (mSettings.contains(APP_PREFERENCES_USERNAME))
            login = mSettings.getString(APP_PREFERENCES_USERNAME, "null")
        if (mSettings.contains(APP_PREFERENCES_PASSWORD))
            password = mSettings.getString(APP_PREFERENCES_PASSWORD, "null")
        //Remove this check after app connect only after loging
        if (login != null  && password != null) {
            var msg = "USER(" + login + COMMA + password + COMMA + lat + COMMA + lon + ")"
            wsj?.sendMessage(msg)
        }
    }
}