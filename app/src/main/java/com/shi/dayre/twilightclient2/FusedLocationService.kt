package com.shi.dayre.twilightclient2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import com.google.android.gms.location.LocationRequest
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationServices
import android.app.NotificationManager
import android.support.v4.content.LocalBroadcastManager
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit
import android.content.ComponentName
import android.app.ActivityManager
import android.content.Context


class FusedLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    var userState = User()
    var serviceStarted: Boolean = false
    val syncLock = java.lang.Object()
    var isMainActivityRunnig = false
    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_ID = 666

    var wsj: WebSocket? = null

    private val TAG = "TLC.fused"
    private var locationRequest: LocationRequest? = null
    private var googleApiClient: GoogleApiClient? = null
    private var fusedLocationProviderApi: FusedLocationProviderApi? = null

    private val mBinder = MyLocalBinder()

    inner class MyLocalBinder : Binder() {
        fun getService(): FusedLocationService {
            return this@FusedLocationService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    private fun connect() {
        var count = 0
        val msg = "CONNECT(" + userState.login + COMMA + userState.password + ")"

        while (wsj?.connected == false) {
            wsj?.connectWebSocket()
            TimeUnit.SECONDS.sleep(1)
            count++
            Log.i("TLC.connect", "connection count = " + count)
            if (count > 10) {
                Toaster.toast(R.string.serverNotResponse)
                break
            }
            wsj?.sendMessage(msg)
        }
    }

    private fun sendMessageToServer(message: String) {
        if (wsj != null) {
            if (!wsj!!.connected) {
                //Reconnect
                Log.i("TLC.connect", "try to reconnect")
                wsj?.connectWebSocket()
            }
        } else {
            //Recreate?
            Log.i("TLC.connect", "socket null")
        }
        wsj?.sendMessage(message)
    }

    private fun sendLocationToServer() {
        Log.i(TAG, "onSendLocation")
        val msg = "USER(" + userState.login + COMMA + userState.password + COMMA +
                userState.latitude + COMMA + userState.longitude + ")"
        sendMessageToServer(msg)
    }

    fun sendBroadcast(action: String) {
        val intentReceive = Intent(action)
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(intentReceive)
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
        val notification: Notification = builder.build()

        startForeground(777, notification)

        val action: String = intent.getAction()

        when (action) {
            ACTION_CONNECT -> {
                wsj = WebSocket(userState.url, CommandFromServerHandler(this, this), this)
                connect()
                getLocation()//Here or in MESSAGE?
            }
            ACTION_SEND_MESSAGE -> {
                sendMessageToServer(intent.getStringExtra("message"))
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        serviceStarted = true
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
        notificationManager?.cancel(NOTIFICATION_ID)
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
        locationRequest!!.interval = CONNECT_EVERY_SECOND*1000
        locationRequest!!.fastestInterval = CONNECT_EVERY_SECOND*1000
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

    override fun onConnectionFailed(arg0: ConnectionResult) {
    }

    override fun onLocationChanged(location: Location) {
        Log.i("TLC.fused", "Location updated")
        Log.i("TLC.exp", "isForeground "+isForeground())
        userState.latitude = location.latitude
        userState.longitude = location.longitude
        sendLocationToServer()
    }

    fun isForeground(myPackage: String="com.shi.dayre.twilightclient2"): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTaskInfo = manager.getRunningTasks(1)
        val componentInfo = runningTaskInfo[0].topActivity
        return componentInfo.packageName == myPackage
    }
}