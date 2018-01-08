package com.shi.dayre.twilightclient2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

val CONNECT_EVERY_SECOND: Long = 10
val APP_PREFERENCES = "mysettings"
val APP_PREFERENCES_USERNAME = "username"
val APP_PREFERENCES_PASSWORD = "password"
val APP_PREFERENCES_SERVER = "server"
val DEFAULT_SERVER = "ws://192.168.1.198:8080/BHServer/serverendpoint"
var user = User()
var location: com.shi.dayre.twilightclient2.LocationProvider? = null

var wsj: WebSocket? = null

class MainActivity : AppCompatActivity() {
    lateinit var mSettings: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    val commandHandler = CommandFromServerHandler(this)
    var mTimer = Timer()
    var mMyTimerTask = onTimerTick(this)

    fun refresh() {
        Thread(Runnable {
            // try to touch View of UI thread
            this.runOnUiThread(java.lang.Runnable {
                Log.i("WebClient", "View updated")
                textFromServer.text = user.userText
                currentStatus.text = user.zoneText
                if (user.logined) {
                    newLogin.visibility = View.GONE
                    newPassword.visibility = View.GONE
                    newServer.visibility = View.GONE
                    if (user.justLogined) {
                        if (mTimer != null) mTimer.cancel()
                        mTimer = Timer()
                        mMyTimerTask = onTimerTick(this)
                        mTimer.schedule(mMyTimerTask, 1000, CONNECT_EVERY_SECOND * 1000);
                        fab.hide()
                        user.justLogined=false
                    }
                }
                if (user.superusered) {
                    superuserbar.visibility=View.VISIBLE
                }

                if (user.location != null)
                    gpsCoordinate.setText(user.location!!.format())
                else {
                    gpsCoordinate.text = this.resources.getText(R.string.gps_disable)
                }
                if (user.locationNet != null)
                    netCoordinate.setText(user.locationNet!!.format())
                else {
                    netCoordinate.text = this.resources.getText(R.string.net_disable)
                }
            })
        }).start()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        location = com.shi.dayre.twilightclient2.LocationProvider(this, this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Load settings
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        editor = mSettings.edit()
        if (mSettings.contains(APP_PREFERENCES_USERNAME))
            user.login = mSettings.getString(APP_PREFERENCES_USERNAME, "null")
        if (mSettings.contains(APP_PREFERENCES_PASSWORD))
            user.password = mSettings.getString(APP_PREFERENCES_PASSWORD, "null")
        if (mSettings.contains(APP_PREFERENCES_SERVER))
            user.server = mSettings.getString(APP_PREFERENCES_SERVER, "null")
        else user.server = DEFAULT_SERVER
        newServer.setText(user.server)
        if (user.login != null)
            newLogin.setText(user.login)
        if (user.password != null)
            newPassword.setText(user.password)


        wsj = WebSocket(user.server, commandHandler, this)

        wsj?.connectWebSocket()

        addNewZone.setOnClickListener {
            if (addnewzonebar.visibility == View.GONE) {
                addnewzonebar.visibility = View.VISIBLE
                //TODO Add current coordinates?
            }
            else addnewzonebar.visibility = View.GONE
            //TODO Change fab image
            fab.show()
        }
        fab.setOnClickListener {
            if (addnewzonebar.visibility == View.VISIBLE){
                var msg = "ADDNEWZONE(" + user.login + "," + user.password + ","+
                        newZoneName.text+","+newZoneLatitude.text+","+newZoneLongitude.text+","+newZoneRadius.text+","+
                        newZoneTextForHuman.text+","+newZoneTextForLight.text+","+newZoneTextForDark.text+","+
                        newZonePriority.text+","+newZoneAchievement.text+")"
                wsj?.sendMessage(msg)
            }
            else {
                try {
                    user.login = newLogin.text.toString()
                    user.password = newPassword.text.toString()
                    editor.putString(APP_PREFERENCES_USERNAME, newLogin.text.toString());
                    editor.putString(APP_PREFERENCES_PASSWORD, newPassword.text.toString());
                    editor.putString(APP_PREFERENCES_SERVER, newServer.text.toString());
                    editor.apply()
                    var msg = "USER(" + user.login + "," + user.password + ",0,0)"
                    wsj?.sendMessage(msg)
                } catch (x: Exception) {
                    println("Cloud not connect to server.")
                }
            }
        }
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        location!!.start()
    }

    fun sendLocationToServer() {
        //user.location?.latitude?.locationToInt() not equal null.locationToInt()
        // And I don't know why.
        val lat: Int = if (user.getBestLocation()?.latitude != null) user.getBestLocation()?.latitude.locationToInt() else 0
        val lon: Int = if (user.getBestLocation()?.longitude != null) user.getBestLocation()?.longitude.locationToInt() else 0

        //Remove this check after app connect only after loging
        if (user.login!=null && !user.login.equals("null")&& user.password!=null && !user.password.equals("null")) {
            var msg = "USER(" + user.login + "," + user.password + "," + lat + "," + lon + ")"
            wsj?.sendMessage(msg)
        }
    }

    class onTimerTick(val context: MainActivity) : TimerTask() {
        override fun run() {
            context.sendLocationToServer()
        }
    }

    fun onClickLocationSettings(view: View) {
        //Enable gps-sensor
        startActivity(Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}




