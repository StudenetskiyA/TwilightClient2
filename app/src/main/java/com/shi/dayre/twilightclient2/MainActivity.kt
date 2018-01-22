package com.shi.dayre.twilightclient2

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
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
import com.google.android.gms.maps.model.LatLng
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import com.shi.dayre.twilightclient2.R.color.colorMainBackgroundLight


val CONNECT_EVERY_SECOND: Long = 30
val COMMA = "|"
val APP_PREFERENCES = "mysettings"
val APP_PREFERENCES_USERNAME = "username"
val APP_PREFERENCES_PASSWORD = "password"
val APP_PREFERENCES_SERVER = "server"
val DEFAULT_SERVER = "ws://test1.uralgufk.ru:8080/BHServer/serverendpoint";
var user = User()

var wsj: WebSocket? = null
val syncLock = java.lang.Object()
val listOfLayout = ArrayList<LinearLayout>()

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {
    var isAddFrameOpen:Boolean = false;
    lateinit var location: com.shi.dayre.twilightclient2.LocationProvider
    lateinit var mSettings: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    var markerToMap: MarkerOptions? = null
    val commandHandler = CommandFromServerHandler(this)
    var mTimer = Timer()
    var mMyTimerTask = onTimerTick(this)
    var gMap: GoogleMap? = null

    override fun onMarkerDragStart(marker: Marker) {
        val position = marker.position
        Log.i("TLC.map",
                String.format("Dragging to %f:%f", position.latitude,
                        position.longitude))
    }

    override fun onMarkerDrag(marker: Marker) {
        val position = marker.position
        Log.i("TLC.map",
                String.format("Dragging to %f:%f", position.latitude,
                        position.longitude))
    }

    override fun onMarkerDragEnd(marker: Marker) {
        val position = marker.position
        Log.i("TLC.map",
                String.format("Dragging to %f:%f", position.latitude,
                        position.longitude))
        newZoneLatitude.setText(position.latitude.toString())
        newZoneLongitude.setText(position.longitude.toString())
    }

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
        gMap?.setOnMarkerDragListener(this);
    }

    fun refresh() = Thread(Runnable {
        // try to touch View of UI thread
        this.runOnUiThread(java.lang.Runnable {
            textFromServer.text = user.userText
            if (!user.zoneText.equals("")) {
                currentStatus.visibility=View.VISIBLE
                currentStatus.text = user.zoneText
            }

            if (user.logined) {
//                if (user.interfaceStyle!=0) {
//                    //mainWindow = ConstraintLayout(this, null, R.style.Light);
//                   // make1curseButton.setS
//                    mainWindow.setBackgroundColor(ContextCompat.getColor(this, colorMainBackgroundLight))
//                }
                connectBar.visibility = View.GONE
                if (user.justLogined) {
                    fab.hide()
                    if (mTimer != null) mTimer.cancel()
                    mTimer = Timer()
                    mMyTimerTask = onTimerTick(this)
                    mTimer.schedule(mMyTimerTask, 1000, CONNECT_EVERY_SECOND * 1000);
                    user.justLogined = false
                }
            } else {
                fab.show()
            }

            if (user.superusered != -1) {
                userbar.visibility = View.VISIBLE
                if (user.superusered > 0) {
                    searchUserButton.visibility = View.VISIBLE
                    addCurseButton.visibility = View.VISIBLE
                    scanUserButton.visibility = View.VISIBLE
                    vampireSendButton.visibility= View.VISIBLE
                }
                if (user.superusered > 8) {
                    superuserbar.visibility = View.VISIBLE
                }
                user.superusered = 0
            }
            //MapBar refresh if needs
            if (!user.searchUserResult.isEmpty()) {

                if (user.infoSearch) {
                    mapInfoBar.visibility= View.VISIBLE
                }

                mapbar.visibility = View.VISIBLE
                val founded = user.searchUserResult.iterator()
                var find: SearchUserResult
                while (founded.hasNext()) {
                    find = founded.next()
                    val draw = if (find.powerSide == Light) R.drawable.light
                    else if (find.powerSide == Dark) R.drawable.dark
                    else R.drawable.human
                    if (find.curse.equals("0")) searchUserCurse.text = getString(R.string.searchUserCurse) + "нет"
                    else searchUserCurse.text = getString(R.string.searchUserCurse) + find.curse
                    searchLastconnect.text = getString(R.string.searchUserLastConnected) + find.lastConnected
                    addMarkerToMap(gMap, find.name, find.latitude, find.longitude, "", resources, draw)
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
                    addCircleToMap(gMap, findZ.latitude, findZ.longitude, findZ.radius.toDouble() * 111900, drawColor)
                }
            }

            if (!user.vampireSend.equals("0")) vampireSendCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireSendAlready) + user.vampireSend
            else vampireSendCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireSendNoYet)

            if (!user.vampireCall.equals("0")) {
                vampireCallBar.visibility=View.VISIBLE
                vampireCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireCallFrom) + user.vampireCall
            }

            if (user.location != null)
                gpsCoordinate.setText(user.location?.format())
            else {
                gpsCoordinate.text = this.resources.getText(R.string.gps_disable)
            }
            if (user.locationNet != null)
                netCoordinate.setText(user.locationNet?.format())
            else {
                netCoordinate.text = this.resources.getText(R.string.net_disable)
            }
            synchronized(syncLock) {
                syncLock.notify()
            }
            Log.i("TLC.view", "View updated ok")
        })
    }).start()

    fun addListener() {
        newZonePinCoordinateButton.setOnClickListener {
            if (mapbar.visibility == View.GONE) {
                gMap?.clear()
                markerToMap = addDragableMarkerToMap(gMap, "", user.getBestLocation()?.latitude,
                        user.getBestLocation()?.longitude, "", resources, R.drawable.point)
                if (user.getBestLocation()?.longitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                            user.getBestLocation()!!.longitude), 12f))
                newZoneRadius.visibility = View.GONE
                newZoneTextForDark.visibility = View.GONE
                newZoneTextForLight.visibility = View.GONE
                newZoneTextForHuman.visibility = View.GONE
                newZonePriority.visibility = View.GONE
                newZoneAchievement.visibility = View.GONE
                newZoneSystem.visibility = View.GONE
                mapbar.visibility = View.VISIBLE
            } else {
                mapbar.visibility = View.GONE
                newZoneRadius.visibility = View.VISIBLE
                newZoneTextForDark.visibility = View.VISIBLE
                newZoneTextForLight.visibility = View.VISIBLE
                newZoneTextForHuman.visibility = View.VISIBLE
                newZonePriority.visibility = View.VISIBLE
                newZoneAchievement.visibility = View.VISIBLE
                newZoneSystem.visibility = View.VISIBLE
            }
        }
        make1curseButton.setOnClickListener {
            addOnClickBarListener(make1cursebar)
        }
        dieButton.setOnClickListener {
            addOnClickBarListener(dieUserBar)
        }
        searchUserButton.setOnClickListener {
            addOnClickBarListener(searchUserBar)
        }
        scanUserButton.setOnClickListener {
            addOnClickBarListener(scanUserBar)
        }
        vampireSendButton.setOnClickListener {
            addOnClickBarListener(vampireSendBar)
        }
        addNewZoneButton.setOnClickListener {
            addOnClickBarListener(addnewzonebar)
        }
        addUserButton.setOnClickListener {
            addOnClickBarListener(adduserbar)
        }
        deleteZoneButton.setOnClickListener {
            addOnClickBarListener(deletezonebar)
        }
        addCurseButton.setOnClickListener {
            addOnClickBarListener(cursebar)
        }

        vampireCallButton.setOnClickListener {
            if (mapbar.visibility == View.GONE) {
                hideBar()
                user.searchUserResult = ArrayList()
                user.searchZoneResult = ArrayList()
                gMap?.clear()
                mapbar.visibility = View.VISIBLE
                isAddFrameOpen=true
                user.infoSearch = true
                var msg = "SEARCHUSER(" + user.login + COMMA + user.password + COMMA + user.vampireCall + COMMA + "9)"
                wsj?.sendMessage(msg)
                if (user.getBestLocation()?.longitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                            user.getBestLocation()!!.longitude), 12f))
            } else {
                hideBar()
            }
        }
        searchAllButton.setOnClickListener {
            if (mapbar.visibility == View.GONE) {
                hideBar()
                mapbar.visibility = View.VISIBLE
                isAddFrameOpen=true
                var msg = "SCANUSER(" + user.login + COMMA + user.password + COMMA + "9)"
                wsj?.sendMessage(msg)
                if (user.getBestLocation()?.longitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                            user.getBestLocation()!!.longitude), 12f))
            } else {
                hideBar()
                fab.hide()
            }
        }

        fab.setOnClickListener {
            hideSoftKeyboard(this)
            //TODO Change visibility to variable
            if (addnewzonebar.visibility == View.VISIBLE) {
                if (newZoneName.text.length > 1) {
                    if (newZoneTextForHuman.text.toString().length < 2) newZoneTextForHuman.setText(" ")
                    if (newZoneTextForLight.text.toString().length < 2) newZoneTextForLight.setText(" ")
                    if (newZoneTextForDark.text.toString().length < 2) newZoneTextForDark.setText(" ")
                    if (newZoneRadius.text.toString().equals("")) newZoneRadius.setText("0")
                    if (newZoneLatitude.text.toString().equals("")) newZoneLatitude.setText("0")
                    if (newZoneLongitude.text.toString().equals("")) newZoneLongitude.setText("0")
                    if (newZoneAchievement.text.toString().length < 2) newZoneAchievement.setText(" ")
                    if (newZoneSystem.text.toString().equals("")) newZoneSystem.setText("0")

                    if (newZonePriority.text.toString().equals("")) newZonePriority.setText("0")
                    val radius = metrToGradusToString(newZoneRadius.text.toString().toInt())
                    var msg = "ADDNEWZONE(" + user.login + COMMA + user.password + COMMA +
                            newZoneName.text.toString() + COMMA + newZoneLatitude.text.toString() + COMMA + newZoneLongitude.text.toString() +
                            COMMA + radius + COMMA + newZoneTextForHuman.text.toString() + COMMA +
                            newZoneTextForLight.text.toString() + COMMA + newZoneTextForDark.text.toString() + COMMA +
                            newZonePriority.text.toString() + COMMA + newZoneAchievement.text.toString() + COMMA +
                            newZoneSystem.text.toString() + ")"
                    wsj?.sendMessage(msg)
                    newZoneName.setText("")
                    newZoneLatitude.setText("")
                    newZoneLongitude.setText("")
                    newZoneTextForHuman.setText("")
                    newZoneTextForLight.setText("")
                    newZoneTextForDark.setText("")
                    newZoneAchievement.setText("")
                }
            } else if (dieUserBar.visibility == View.VISIBLE) {
                var msg = "DIE(" + user.login + COMMA + user.password + ")"
                wsj?.sendMessage(msg)
                System.exit(0)
            } else if (vampireSendBar.visibility == View.VISIBLE) {
                if (!vampireSendName.text.equals("")) {
                    var msg = "VAMPIRESEND(" + user.login + COMMA + user.password + COMMA +
                            vampireSendName.text + COMMA
                    if (user.vampireSend.equals("")) {
                        wsj?.sendMessage(msg+ "1" + ")")
                    }
                    else {
                        wsj?.sendMessage(msg+ "0" + ")")
                    }
                }
            } else if (make1cursebar.visibility == View.VISIBLE) {
                if (!curse1UserName.text.equals("")) {
                    var msg = "MAKECURSE(" + user.login + COMMA + user.password + COMMA +
                            curse1UserName.text + COMMA + "Проклятие 1" + ")"
                    wsj?.sendMessage(msg)
                }
            } else if (searchUserBar.visibility == View.VISIBLE) {
                if (searchUserName.text.length > 1) {
                    searchLastconnect.text = ""
                    user.searchUserResult = ArrayList()
                    user.searchZoneResult = ArrayList()
                    gMap?.clear()
                    user.infoSearch = true
                    val position = searchSpinner.selectedItemPosition + 1
                    var msg = "SEARCHUSER(" + user.login + COMMA + user.password + COMMA +
                            searchUserName.text + COMMA + position + ")"
                    wsj?.sendMessage(msg)
                    if (user.getBestLocation()?.longitude != null && user.getBestLocation()?.latitude != null)
                        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                                user.getBestLocation()!!.longitude), 12f))
                    searchUserName.setText("")
                }
            } else if (scanUserBar.visibility == View.VISIBLE) {
                user.searchUserResult = ArrayList()
                user.searchZoneResult = ArrayList()
                gMap?.clear()
                val position = scanSpinner.selectedItemPosition + 1
                var msg = "SCANUSER(" + user.login + COMMA + user.password + COMMA + position + ")"
                wsj?.sendMessage(msg)
                if (user.getBestLocation()?.longitude != null && user.getBestLocation()?.latitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.getBestLocation()!!.latitude,
                            user.getBestLocation()!!.longitude), 12f))
            } else if (cursebar.visibility == View.VISIBLE) {
                if (!curseUserName.text.equals("")) {
                    var msg = "MAKECURSE(" + user.login + COMMA + user.password + COMMA +
                            curseUserName.text + COMMA + curseCurseName.text + ")"
                    wsj?.sendMessage(msg)
                }
            } else if (adduserbar.visibility == View.VISIBLE) {
                if (addUserName.text.length > 2) {
                    var msg = "ADDUSER(" + user.login + COMMA + user.password + COMMA +
                            addUserName.text + COMMA + addUserPass.text + COMMA + addUserPowerside.text + COMMA + addUserSuperuser.text + ")"
                    wsj?.sendMessage(msg)
                }
            } else if (deletezonebar.visibility == View.VISIBLE) {
                if (deleteZoneName.text.length > 2) {
                    var msg = "DELETEZONE(" + user.login + COMMA + user.password + COMMA +
                            deleteZoneName.text + ")"
                    wsj?.sendMessage(msg)
                }
            } else if (searchbar.visibility == View.VISIBLE) {
                if (searchName.text.length > 1) {
                    searchLastconnect.text = ""
                    user.searchUserResult = ArrayList()
                    user.searchZoneResult = ArrayList()
                    gMap?.clear()
                    var msg = "SEARCHUSER(" + user.login + COMMA + user.password + COMMA +
                            searchName.text + COMMA + "9)"
                    wsj?.sendMessage(msg)
                    searchName.setText("")
                }
            } else {
                try {
                    var count = 0
                    fab.hide()
                    user.login = newLogin.text.toString()
                    user.password = newPassword.text.toString()
                    user.server = newServer.text.toString()
                    editor.putString(APP_PREFERENCES_USERNAME, newLogin.text.toString());
                    editor.putString(APP_PREFERENCES_PASSWORD, newPassword.text.toString());
                    editor.putString(APP_PREFERENCES_SERVER, newServer.text.toString());
                    editor.apply()
                    var msg = "CONNECT(" + user.login + COMMA + user.password + ")"
                    wsj = WebSocket(user.server, commandHandler, this)
                    while (wsj?.connected == false) {
                        wsj?.connectWebSocket()
                        TimeUnit.SECONDS.sleep(1)
                        count++
                        Log.i("TLC.connect", "connection count = " + count)
                        if (count > 10) {
                            Toaster.toast(R.string.serverNotResponse);
                            fab.show()
                            break
                        }
                        wsj?.sendMessage(msg)
                    }
                } catch (x: Exception) {
                    Log.e("TLC.connect", x.toString())
                }
            }
        }
    }

    fun addOnClickBarListener(la: LinearLayout) {
        if (la.visibility == View.GONE) {
            hideBar()
            la.visibility = View.VISIBLE
            isAddFrameOpen=true
            fab.show()
        } else {
            hideBar()
            fab.hide()
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
        listOfLayout.add(make1cursebar)
        listOfLayout.add(mapbar)
        listOfLayout.add(mapInfoBar)
        listOfLayout.add(searchUserBar)
        listOfLayout.add(scanUserBar)
        listOfLayout.add(dieUserBar)
        listOfLayout.add(vampireSendBar)
        listOfLayout.add(mailBar)
    }

    fun hideBar() {
        fab.hide()
        isAddFrameOpen=false
        user.infoSearch = false
        gMap?.clear()
        user.searchUserResult = ArrayList()
        user.searchZoneResult = ArrayList()
        for (lay in listOfLayout) {
            lay.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        location = com.shi.dayre.twilightclient2.LocationProvider(this, this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        loadLayout()
        loadSettings()
        addListener()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.searchmap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        refresh()
    }

    override fun onBackPressed(){
        if (isAddFrameOpen) {
            hideBar()
        }
        else {
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        System.exit(0)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.exitYesNo)).setPositiveButton("Выход", dialogClickListener)
                    .setNegativeButton("Отмена", dialogClickListener).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh() 
        if (!location.isAnySensorEnable()) {
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.sensorOpen)).setPositiveButton("Да", dialogClickListener)
                    .setNegativeButton("Отмена", dialogClickListener).show()
        }
        location.start()
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

    class onTimerTick(val context: MainActivity) : TimerTask() {
        override fun run() {
            context.sendLocationToServer()
        }
    }
}




