package com.shi.dayre.twilightclient2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Binder
import android.os.IBinder
import com.google.android.gms.location.LocationRequest
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationServices
import java.util.*
import android.app.NotificationManager



class FusedLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private var notificationManager: NotificationManager? = null
    val NOTIFICATION_ID = 666

    private val TAG = "TLC.fused"
    private var savedLocation: Location? = null
    var timer = Timer()
    private val LOCATION_INTERVAL = 10000
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var mSettings: SharedPreferences
    private var mContext: Context? = null
    private var locationRequest: LocationRequest? = null
    private var googleApiClient: GoogleApiClient? = null
    private var fusedLocationProviderApi: FusedLocationProviderApi? = null

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(applicationContext,
                NOTIFICATION_ID, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = Notification.Builder(this)
                .setSmallIcon(R.drawable.yinyan)
                .setContentTitle(getString(R.string.twilightWatchYou))
                .setContentIntent(contentIntent)
        val notification: Notification= builder.build()

        startForeground(777, notification)

        mContext = this
        mSettings = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        editor = mSettings.edit()
        timer.schedule(TimerTickTask(), 0, CONNECT_EVERY_SECOND * 1000)
        getLocation()

        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private inner class TimerTickTask : TimerTask() {
        override fun run() {
            Log.i(TAG, "onTick")
            sendLocationToServer()
        }
    }

    fun sendLocationToServer() {
        Log.i(TAG, "onSendLocation")
        var login: String? = null
        var password: String? = null

        if (mSettings.contains(APP_PREFERENCES_USERNAME))
            login = mSettings.getString(APP_PREFERENCES_USERNAME, "null")
        if (mSettings.contains(APP_PREFERENCES_PASSWORD))
            password = mSettings.getString(APP_PREFERENCES_PASSWORD, "null")

        if (login != null && password != null) {
            val msg = "USER(" + login + COMMA + password + COMMA + savedLocation?.latitude + COMMA + savedLocation?.longitude + ")"

            if (wsj != null) {
                if (!wsj!!.connected) {
                    //Reconnect!
                    Log.i("TLC.connect", "try to reconnect")
                    wsj?.connectWebSocket()
                }
            } else {
                Log.i("TLC.connect", "socket null")
            }
            wsj?.sendMessage(msg)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()

        try {
            if (googleApiClient != null) {
                googleApiClient!!.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocation() {
        locationRequest = LocationRequest.create()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = LOCATION_INTERVAL.toLong()
        locationRequest!!.fastestInterval = LOCATION_INTERVAL.toLong()
        fusedLocationProviderApi = LocationServices.FusedLocationApi
        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        if (googleApiClient != null) {
            googleApiClient!!.connect()
        }
    }

    override fun onConnected(arg0: Bundle?) {
        fusedLocationProviderApi!!.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    override fun onConnectionSuspended(arg0: Int) {
    }

    override fun onLocationChanged(location: Location) {
        Log.i("TLC.fused", "Location updated")
        editor.putFloat(APP_PREFERENCES_LAST_LATITUDE_NET, location.latitude.toFloat())
        editor.putFloat(APP_PREFERENCES_LAST_LONGITUDE_NET, location.longitude.toFloat())
        editor.apply()
        savedLocation = location
    }

    override fun onConnectionFailed(arg0: ConnectionResult) {
    }
}