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
//import com.sun.corba.se.impl.orbutil.concurrent.SyncUtil.acquire
import android.net.wifi.WifiManager
import android.os.PowerManager

class FusedLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    var userState = User()
    private var serviceStarted: Boolean = false
    val syncLock = java.lang.Object()
    var isMainActivityRunnig = false
    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_ID = 666

    var wsj: WebSocket? = null
    var  wifiLock:WifiManager.WifiLock? = null
    var  wakeLock: PowerManager.WakeLock? = null
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

    private fun connect(needToReturn:Boolean) {
        var count = 0
        val msg = "CONNECT(" + userState.login + COMMA + userState.password + COMMA + CLIENT_VERSION+")"
        //val msg = "CONNECT(" + userState.login + COMMA + userState.password +")"

        while (wsj?.connected == false) {
            wsj = WebSocket(userState.url, CommandFromServerHandler(this, this))
            wsj?.connectWebSocket()
            TimeUnit.SECONDS.sleep(1)
            count++
            if (count > 9) {
                if (needToReturn) {
                    userState.superusered = -1
                    sendBroadcasting(BROADCAST_NEED_TO_REFRESH)
                }
                Toaster.toast(R.string.serverNotResponse)
                writeToLog("Fail to connect to server, url = "+userState.url)
                break
            }
        }
        if (wsj?.connected == true)
        wsj?.sendMessage(msg)
    }

    private fun sendMessageToServer(message: String) {
        if (wsj != null) {
            if (!wsj!!.connected) {
                //Reconnect
                Log.i("TLC.connect", "try to reconnect")
                writeToLog("Service try to reconnect")
                connect(false)
            }
        } else {
            //Recreate?
            Log.i("TLC.connect", "socket null")
            writeToLog("Server socket null")
        }
        writeToLog("Service send message - "+message)
        wsj?.sendMessage(message)
    }

    private fun sendLocationToServer() {
        Log.i(TAG, "onSendLocation")
        val msg = "USER(" + userState.login + COMMA + userState.password + COMMA +
                userState.latitude + COMMA + userState.longitude + ")"
        sendMessageToServer(msg)
    }

    fun sendBroadcasting(action: String) {
        val intentReceive = Intent(BROADCAST)
        intentReceive.putExtra("action",action)
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(intentReceive)
    }

    fun runInForeground() {
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartService with command "+intent.action)

        val action: String? = intent.action
        writeToLog("Service started with action = "+action)

        when (action) {
            ACTION_SETUP_CONNECTION -> {
                userState.url = intent.getStringExtra("url")
                userState.login = intent.getStringExtra("login")
                userState.password = intent.getStringExtra("password")
            }
            ACTION_CONNECT -> {
                val wm = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock")
                if (wifiLock?.isHeld != true) {
                    Log.i(TAG, "wifi locked")
                    wifiLock?.acquire()
                }
                val wm2 = application.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = wm2.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock")
                if (wakeLock?.isHeld != true) {
                    Log.i(TAG, "wake locked")
                    wakeLock?.acquire()
                }

                writeToLog("WiFi lock hold on")
                wsj = WebSocket(userState.url, CommandFromServerHandler(this, this))
                connect(true)
            }
            ACTION_DISCONNECT -> {
                destroyThis()
                stopForeground(true)
            }
            ACTION_SEND_MESSAGE -> {
                sendMessageToServer(intent.getStringExtra("message"))
            }
        }
        return START_STICKY
       // return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        writeToLog("Service created")

        serviceStarted = true
    }

    fun destroyThis() {
        userState = User()
        wsj?.disconnectWebSocket()
        // release the WifiLock
        if (wifiLock != null) {
            if (wifiLock?.isHeld()==true) {
                wifiLock?.release()
                Log.i(TAG, "wifi unlocked")
                writeToLog("WiFi lock unhelded")
            }
        }
        if (wakeLock != null) {
            if (wakeLock?.isHeld()==true) {
                wakeLock?.release()
                Log.i(TAG, "wake unlocked")
                writeToLog("Wake lock unhelded")
            }
        }
        writeToLog("Service destroyed")
        notificationManager?.cancel(NOTIFICATION_ID)
        try {
            if (googleApiClient != null) {
                googleApiClient!!.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
        destroyThis()
    }

    fun startGettingLocation() {
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
        writeToLog("Location changed")
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