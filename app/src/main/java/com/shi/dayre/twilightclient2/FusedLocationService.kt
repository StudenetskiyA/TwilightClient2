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
import android.os.Build
import android.os.IBinder

import com.google.android.gms.location.LocationRequest

import android.util.Log

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationServices
import xdroid.toaster.Toaster
import java.util.*

class FusedLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private val TAG = "TLC.fused"
    var savedLocation:Location? = null
    var timer = Timer()
    private val LOCATION_INTERVAL = 1000
    lateinit var editor: SharedPreferences.Editor
    lateinit var mSettings: SharedPreferences
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
        startForeground(777, notification)

        //super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        mContext = this
        mSettings = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        editor = mSettings.edit()
        timer.schedule(timerTickTask(), 0, CONNECT_EVERY_SECOND * 1000);
        getLocation()
    }

    private inner class timerTickTask : TimerTask() {
        override fun run() {
            Log.i(TAG, "onTick")
           // Toaster.toast("timer tick");
            sendLocationToServer()
            // MainActivity().refresh()
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
        //Remove this check after app connect only after loging
        if (login != null && password != null) {
            var msg = "USER(" + login + COMMA + password + COMMA + savedLocation?.latitude + COMMA + savedLocation?.longitude + ")"
            wsj?.sendMessage(msg)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
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
        locationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest!!.setInterval(LOCATION_INTERVAL.toLong())
        locationRequest!!.setFastestInterval(LOCATION_INTERVAL.toLong())
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
        //  Location location = fusedLocationProviderApi.getLastLocation(googleApiClient);
        fusedLocationProviderApi!!.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    override fun onConnectionSuspended(arg0: Int) {

    }

    fun onResponse(reqCode: Int, statusCode: Int, json: String) {

    }

    fun onCancel(canceled: Boolean) {

    }

    fun onProgressChange(progress: Int) {

    }

    override fun onLocationChanged(location: Location) {
        //here
        Log.i("TLC.location", "Location updated")
        editor.putFloat(APP_PREFERENCES_LAST_LATITUDE_NET, location.getLatitude().toFloat())
        editor.putFloat(APP_PREFERENCES_LAST_LONGITUDE_NET, location.getLongitude().toFloat())
        editor.apply()
        savedLocation=location
        //Toast.makeText(mContext, "Driver location :" + location.getLatitude() + " , " + location.getLongitude(), Toast.LENGTH_SHORT).show()
    }

    fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

    }

    fun onProviderEnabled(provider: String) {

    }

    fun onProviderDisabled(provider: String) {

    }

    override fun onConnectionFailed(arg0: ConnectionResult) {

    }
}