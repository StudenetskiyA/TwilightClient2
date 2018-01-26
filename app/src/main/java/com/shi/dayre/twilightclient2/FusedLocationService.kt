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
import android.content.Intent.getIntent
import kotlinx.android.synthetic.main.activity_main.*
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat


class FusedLocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private var notificationManager: NotificationManager? = null
    val NOTIFICATION_ID = 666

    var wsj: WebSocket? = null
    lateinit var login: String
    lateinit var password: String
    lateinit var url: String

    private val TAG = "TLC.fused"
    private var savedLocation: Location? = null

    private val LOCATION_INTERVAL = CONNECT_EVERY_SECOND
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var mSettings: SharedPreferences
    private var mContext: Context? = null
    private var locationRequest: LocationRequest? = null
    private var googleApiClient: GoogleApiClient? = null
    private var fusedLocationProviderApi: FusedLocationProviderApi? = null

    private val mBinder = MyLocalBinder()

     var testCount = 0


    inner class MyLocalBinder : Binder() {
        fun getService() : FusedLocationService {
            return this@FusedLocationService
        }
    }

    fun getCurrentTime(): String {
        val dateformat = SimpleDateFormat("HH:mm:ss MM/dd/yyyy",
                Locale.US)
        return dateformat.format(Date())
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    private fun connect() {
        var count = 0
        // fab.hide()
        Log.i("TLC.view", "onRestart")
        val msg = "CONNECT(" + login + COMMA + password + ")"
        wsj = WebSocket(url, CommandFromServerHandler(this), MainActivity())

        while (wsj?.connected == false) {
            wsj?.connectWebSocket()
            TimeUnit.SECONDS.sleep(1)
            count++
            Log.i("TLC.connect", "connection count = " + count)
            if (count > 10) {
                Toaster.toast(R.string.serverNotResponse)
                // fab.show()
                break
            }
            wsj?.sendMessage(msg)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        login = intent.getStringExtra("login")
        password = intent.getStringExtra("password")
        url = intent.getStringExtra("url")
        Log.i(TAG, "Intent read:" + login + "," + password)

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
            "test" -> {
                Log.i("TLC.exp", "Service count=" + testCount)
                testCount++
            }
            "connect" -> connect()
            "send message" -> {
                val message = intent.getStringExtra("message")
                wsj?.sendMessage(message)
                mContext = this
                mSettings = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                editor = mSettings.edit()

                getLocation()
            }
        }



        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        //stopSelf()

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
        locationRequest!!.interval = CONNECT_EVERY_SECOND.toLong()
        locationRequest!!.fastestInterval = CONNECT_EVERY_SECOND.toLong()
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
//        editor.putFloat(APP_PREFERENCES_LAST_LATITUDE_NET, location.latitude.toFloat())
//        editor.putFloat(APP_PREFERENCES_LAST_LONGITUDE_NET, location.longitude.toFloat())
//        editor.apply()
        savedLocation = location
        sendLocationToServer()
    }

    override fun onConnectionFailed(arg0: ConnectionResult) {
    }
}