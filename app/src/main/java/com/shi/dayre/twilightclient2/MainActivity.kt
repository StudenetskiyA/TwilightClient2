package com.shi.dayre.twilightclient2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.shi.dayre.twilightclient2.PowerSide.*
import xdroid.toaster.Toaster
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


val CONNECT_EVERY_SECOND: Long = 10

val APP_PREFERENCES = "mysettings"
val APP_PREFERENCES_USERNAME = "username"
val APP_PREFERENCES_PASSWORD = "password"
val APP_PREFERENCES_SERVER = "server"
val DEFAULT_SERVER = "ws://192.168.1.198:8080/BHServer/serverendpoint"
var user = User()
var location: com.shi.dayre.twilightclient2.LocationProvider? = null

var wsj: WebSocket? = null
val syncLock = java.lang.Object()
val listOfLayout = ArrayList<LinearLayout>()

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var mSettings: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    val commandHandler = CommandFromServerHandler(this)
    var mTimer = Timer()
    var mMyTimerTask = onTimerTick(this)
    var gMap: GoogleMap? = null

    override fun onMapReady(map: GoogleMap) {
        Log.i("WebClient", "onMapReady")
        gMap = map
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID)
        map.setIndoorEnabled(true)
        map.setBuildingsEnabled(true)
        if (user.getBestLocation()?.longitude != null && user.getBestLocation()?.latitude != null)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.longitude,
                    user.getBestLocation()!!.longitude), 10f))
        map.getUiSettings().setZoomControlsEnabled(true)
        map.getUiSettings().setMapToolbarEnabled(false)
    }

    fun refresh() {
        Thread(Runnable {
            // try to touch View of UI thread
            this.runOnUiThread(java.lang.Runnable {
                Log.i("Socket", "View updated")
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
                        user.justLogined = false
                    }
                }
                if (user.superusered) {
                    superuserbar.visibility = View.VISIBLE
                }
                //MapBar refresh if needs
                if (!user.searchUserResult.isEmpty() && mapbar.visibility == View.VISIBLE) {
                    val founded = user.searchUserResult.iterator()
                    var find: SearchUserResult
                    while (founded.hasNext()) {
                        find = founded.next()
                        val draw = if (find.powerSide == Light) R.drawable.light
                        else if (find.powerSide == Dark) R.drawable.dark
                        else R.drawable.human

                        searchLastconnect.text = getString(R.string.searchUserLastConnected) + find.lastConnected
                        addMarkerToMap(gMap, find.name, find.latitude.toDouble(), find.longitude.toDouble(), "", resources, draw)
                    }
                    //And zones
                    val foundedZ = user.searchZoneResult.iterator()
                    var findZ: SearchZoneResult
                    while (foundedZ.hasNext()) {
                        findZ = foundedZ.next()
                        val draw = if (findZ.priority == 0) R.drawable.zone1
                        else if (findZ.priority == 1) R.drawable.zone2
                        else R.drawable.zone3
                        addMarkerToMap(gMap, findZ.name, findZ.latitude, findZ.longitude, findZ.textForHuman, resources, draw)
                        //TODO Draw circle
                        val drawColor = if (findZ.priority == 0) Color.GREEN
                        else if (findZ.priority == 1) Color.YELLOW
                        else Color.RED

                        Log.i("Socket.Radius", findZ.name + "=" + findZ.radius.toDouble().toString())
                        addCircleToMap(gMap, findZ.latitude, findZ.longitude, findZ.radius.toDouble() * 10000, drawColor)
                    }
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
                synchronized(syncLock) {
                    syncLock.notify()
                }
                Log.i("Socket", "View updated ok")
            })
        }).start()
    }

    fun addListener() {
        //TODO Make list of element and show/hide for list

        addNewZoneButton.setOnClickListener {
            if (addnewzonebar.visibility == View.GONE) {
                hideBar()
                addnewzonebar.visibility = View.VISIBLE
                //TODO Change fab image
                fab.show()
            } else {
                hideBar()
                fab.hide()
            }
        }
        addUserButton.setOnClickListener {
            if (adduserbar.visibility == View.GONE) {
                hideBar()
                adduserbar.visibility = View.VISIBLE
                //TODO Change fab image
                fab.show()
            } else {
                hideBar()
                fab.hide()
            }
        }
        deleteZoneButton.setOnClickListener {
            if (deletezonebar.visibility == View.GONE) {
                hideBar()
                deletezonebar.visibility = View.VISIBLE
                //TODO Change fab image
                fab.show()
            } else {
                hideBar()
                fab.hide()
            }
        }
        addCurseButton.setOnClickListener {
            if (cursebar.visibility == View.GONE) {
                hideBar()
                cursebar.visibility = View.VISIBLE
                //TODO Change fab image
                fab.show()
            } else {
                hideBar()
                fab.hide()
            }
        }
        searchAllButton.setOnClickListener {
            if (searchbar.visibility == View.GONE) {
                hideBar()
                searchbar.visibility = View.VISIBLE
                //TODO Change fab image
                fab.show()
                gMap?.clear()
                user.searchUserResult = ArrayList()
                user.searchZoneResult = ArrayList()
                var msg = "SEARCHALL(" + user.login + "," + user.password + ")"
                wsj?.sendMessage(msg)
                if (user.getBestLocation()?.longitude != null && user.getBestLocation()?.latitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                            user.getBestLocation()!!.longitude), 12f))
            } else {
                hideBar()
                gMap?.clear()
                fab.hide()
            }
        }

        fab.setOnClickListener {
            hideSoftKeyboard(this)
            //TODO Change visibility to variable
            if (addnewzonebar.visibility == View.VISIBLE) {
                if (newZoneTextForHuman.length()<2) newZoneTextForHuman.setText(" ")
                if (newZoneTextForLight.length()<2) newZoneTextForLight.setText(" ")
                if (newZoneTextForDark.length()<2) newZoneTextForDark.setText(" ")
                if (newZoneAchievement.length()<2) newZoneAchievement.setText(" ")
                var msg = "ADDNEWZONE(" + user.login + "," + user.password + "," +
                        newZoneName.text + "," + newZoneLatitude.text + "," + newZoneLongitude.text + "," + newZoneRadius.text + "," +
                        newZoneTextForHuman.text + "," + newZoneTextForLight.text + "," + newZoneTextForDark.text + "," +
                        newZonePriority.text + "," + newZoneAchievement.text + ","+newZoneSystem.text+")"
                wsj?.sendMessage(msg)
                newZoneName.setText("")
                newZoneLatitude.setText("")
                newZoneLongitude.setText("")
                newZoneTextForHuman.setText("")
                newZoneTextForLight.setText("")
                newZoneTextForDark.setText("")
                newZoneAchievement.setText("")
            } else if (cursebar.visibility == View.VISIBLE) {
                if (!curseUserName.text.equals("")) {
                    var msg = "MAKECURSE(" + user.login + "," + user.password + "," +
                            curseUserName.text + ","+ curseCurseName.text+")"
                    wsj?.sendMessage(msg)
                }
            } else if (adduserbar.visibility == View.VISIBLE) {
                if (addUserName.text.length>2) {
                    var msg = "ADDUSER(" + user.login + "," + user.password + "," +
                            addUserName.text + "," + addUserPass.text+"," + addUserPowerside.text+"," + addUserSuperuser.text+")"
                    wsj?.sendMessage(msg)
                }
            } else if (deletezonebar.visibility == View.VISIBLE) {
                if (deleteZoneName.text.length>2) {
                    var msg = "DELETEZONE(" + user.login + "," + user.password + "," +
                            deleteZoneName.text +")"
                    wsj?.sendMessage(msg)
                }
            } else if (searchbar.visibility == View.VISIBLE) {
                if (searchName.text.length>1) {
                    searchLastconnect.text = ""
                    user.searchUserResult = ArrayList()
                    user.searchZoneResult = ArrayList()
                    gMap?.clear()
                    var msg = "SEARCHUSER(" + user.login + "," + user.password + "," +
                            searchName.text + ")"
                    wsj?.sendMessage(msg)
                    searchName.setText("")
                }
            } else {
                try {
                    var count=0
                    //TODO Wait
                    fab.hide()
                    user.login = newLogin.text.toString()
                    user.password = newPassword.text.toString()
                    user.server = newServer.text.toString()
                    editor.putString(APP_PREFERENCES_USERNAME, newLogin.text.toString());
                    editor.putString(APP_PREFERENCES_PASSWORD, newPassword.text.toString());
                    editor.putString(APP_PREFERENCES_SERVER, newServer.text.toString());
                    editor.apply()
                    var msg = "USER(" + user.login + "," + user.password + "," + user.getBestLocation()?.longitude + "," +
                            user.getBestLocation()?.longitude + ")"
                    wsj = WebSocket(user.server, commandHandler, this)
                    while (wsj?.connected==false) {
                        wsj?.connectWebSocket()
                        TimeUnit.SECONDS.sleep(1)
                        count++
                        Log.i("Socket","connection count = "+count)
                        if (count > 10) {
                            Toaster.toast(R.string.serverNotResponse);
                            fab.show()
                            break
                        }
                        wsj?.sendMessage(msg)
                    }
                } catch (x: Exception) {
                    println("Cloud not connect to server.")
                }
            }
        }
    }

    fun loadSettings() {
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
    }

    fun loadLayout() {
        listOfLayout.add(addnewzonebar)
        listOfLayout.add(searchbar)
        listOfLayout.add(cursebar)
        listOfLayout.add(adduserbar)
        listOfLayout.add(deletezonebar)
    }

    fun hideBar(){
        for (lay in listOfLayout){
            lay.visibility=View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        location = com.shi.dayre.twilightclient2.LocationProvider(this, this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        loadLayout()
        loadSettings()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.searchmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        addListener()
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
////        return when (item.itemId) {
////           // R.id.action_settings -> true
////           // else -> super.onOptionsItemSelected(item)
////        }
//    }

    override fun onResume() {
        super.onResume()
        location!!.start()
    }

    fun sendLocationToServer() {
        val lat: Double? = if (user.getBestLocation()?.latitude != null) user.getBestLocation()?.latitude else 0.0
        val lon: Double? = if (user.getBestLocation()?.longitude != null) user.getBestLocation()?.longitude else 0.0

        //Remove this check after app connect only after loging
        if (user.login != null && !user.login.equals("null") && user.password != null && !user.password.equals("null")) {
            var msg = "USER(" + user.login + "," + user.password + "," + lat + "," + lon + ")"
            wsj?.sendMessage(msg)
        }
    }

    class onTimerTick(val context: MainActivity) : TimerTask() {
        override fun run() {
            context.sendLocationToServer()
        }
    }
}




