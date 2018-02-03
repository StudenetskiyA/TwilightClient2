package com.shi.dayre.twilightclient2

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
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.shi.dayre.twilightclient2.PowerSide.*
import kotlin.collections.ArrayList
import com.google.android.gms.maps.model.LatLng
import android.location.LocationManager
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.content.*
import android.os.IBinder
import android.content.ComponentName
import android.content.ServiceConnection
import android.support.v4.content.LocalBroadcastManager

const val CLIENT_VERSION = "0.727"
const val CONNECT_EVERY_SECOND: Long = 30
const val COMMA = "|"
const val APP_PREFERENCES = "mysettings"
const val APP_PREFERENCES_USERNAME = "username"
const val APP_PREFERENCES_PASSWORD = "password"
const val APP_PREFERENCES_SERVER = "server"
const val APP_PREFERENCES_SUPERUSER = "superuser"
const val APP_PREFERENCES_ALREADY_VIEW_HELLO = "viewhello"

const val BROADCAST = "com.shi.dayre.twilightclient2"
const val BROADCAST_NEED_TO_REFRESH = "com.shi.dayre.twilightclient2.needToRefresh"
const val BROADCAST_NEED_TO_REFRESH_MAP = "com.shi.dayre.twilightclient2.needToRefreshMap"
const val BROADCAST_NEED_TO_REFRESH_USERSTATE = "com.shi.dayre.twilightclient2.needToRefreshUserState"

const val ACTION_CONNECT = "connect"
const val ACTION_DISCONNECT = "disconnect"
const val ACTION_SEND_MESSAGE = "send message"
const val ACTION_SETUP_CONNECTION = "setup"

const val NOTIFICATION_ID = 666
const val DEFAULT_SERVER = "ws://test1.uralgufk.ru:8080/BHServer/serverendpoint"

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {
    var service: FusedLocationService? = null
    var binded: Boolean = false

    private var superUserView:Int=-1
    private lateinit var mSettings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var broadcast: BroadcastReceiver

    private var isAddFrameOpen: Boolean = false
    private val listOfLayout = ArrayList<LinearLayout>()

    private var markerToMap: MarkerOptions? = null
    private var gMap: GoogleMap? = null
    private var infoSearch = false
    private var fabLock = false

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        _service: IBinder) {
            val binder = _service as FusedLocationService.MyLocalBinder
            service = binder.getService()
            binded = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binded = false
        }
    }

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
        map.getUiSettings().setZoomControlsEnabled(true)
        map.getUiSettings().setMapToolbarEnabled(false)
        gMap?.setOnMarkerDragListener(this)
    }

    fun broadcastRefreshMap() = Thread(Runnable {
        // try to touch View of UI thread
        this.runOnUiThread({
            //MapBar refresh if needs
            try {
                if (service != null && !service?.userState?.searchUserResult!!.isEmpty()) {
                    if (mapInfoBar.visibility == View.VISIBLE) {
                        //One search, zoom to target
                        if (service?.userState?.longitude != null)
                            gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(service?.userState?.searchUserResult?.get(0)?.latitude!!,
                                    service?.userState?.searchUserResult?.get(0)?.longitude!!), 12f))

                    } else {
                        //Many search, zoom to user
                        if (service?.userState?.longitude != null)
                            gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(service?.userState?.latitude!!,
                                    service?.userState?.longitude!!), 12f))
                    }

                    val founded = service?.userState?.searchUserResult?.iterator()
                    while (founded!!.hasNext()) {
                        val find = founded.next()
                        val draw = when {
                            find.powerSide == Light -> R.drawable.light
                            find.powerSide == Dark -> R.drawable.dark
                            else -> R.drawable.human
                        }
                        if (find.curse == "0") searchUserCurse.text = getString(R.string.searchUserCurse) + getString(com.shi.dayre.twilightclient2.R.string.No)
                        else searchUserCurse.text = getString(R.string.searchUserCurse) + find.curse

                        searchLastconnect.text = getString(R.string.searchUserLastConnected) + find.lastConnected
                        addMarkerToMap(gMap, find.name, find.latitude, find.longitude, find.lastConnected, resources, draw)
                    }

                }
                //And zones
                if (service != null && !service?.userState?.searchZoneResult!!.isEmpty()) {
                    val foundedZ = service?.userState?.searchZoneResult!!.iterator()
                    //var findZ: SearchZoneResult
                    while (foundedZ.hasNext()) {
                        val findZ = foundedZ.next()
                        val draw = when {
                            findZ.priority == 0 -> R.drawable.zone1
                            findZ.priority == 1 -> R.drawable.zone2
                            else -> R.drawable.zone3
                        }
                        addMarkerToMap(gMap, findZ.name, findZ.latitude, findZ.longitude, findZ.textForHuman, resources, draw)
                        val drawColor = when {
                            findZ.priority == 0 -> Color.GREEN
                            findZ.priority == 1 -> Color.YELLOW
                            else -> Color.RED
                        }

                        addCircleToMap(gMap, findZ.latitude, findZ.longitude, findZ.radius * 111900, drawColor)
                    }
                }

                Log.i("TLC.view", "View map updated ok")
            } catch (x: Exception) {
                writeToLog("refreshMap FAIL.")
                Log.e("TLC.view.refreshMap", x.toString())
            }
        })
    }).start()

    fun broadcastRefreshSuperUser() = Thread(Runnable {

        // try to touch View of UI thread
        this.runOnUiThread({
            try {
            fabLock=false
            fab.setImageResource(R.drawable.ok)
            if (binded) {
                if (service?.userState?.superusered!=null) {
                    editor.putInt(APP_PREFERENCES_SUPERUSER, service?.userState?.superusered!!)
                    editor.apply()
                }

                if (service != null && service?.userState?.superusered != -1) {
                    superUserView= service?.userState?.superusered!!
                } else {
                    fab.show()
                }
                Log.i("TLC.view", "View updated ok")
            }

            if (superUserView!=-1) {
                //TODO Add more superuser types
                fab.hide()
                connectBar.visibility = View.GONE
                userbar.visibility = View.VISIBLE
                netCoordinate.visibility = View.VISIBLE
                lastConnected.visibility = View.VISIBLE
                if (superUserView > 0) {
                    searchUserButton.visibility = View.VISIBLE
                    addCurseButton.visibility = View.VISIBLE
                    scanUserButton.visibility = View.VISIBLE
                    vampireSendButton.visibility = View.VISIBLE
                    mailButton.visibility = View.VISIBLE
                }
                if (superUserView > 8) {
                    superuserbar.visibility = View.VISIBLE
                }
            }
            else { fab.show()
                connectBar.visibility = View.VISIBLE
                userbar.visibility = View.GONE
                netCoordinate.visibility = View.GONE
                lastConnected.visibility = View.GONE
                superuserbar.visibility = View.GONE
                searchUserButton.visibility = View.GONE
                addCurseButton.visibility = View.GONE
                scanUserButton.visibility = View.GONE
                vampireSendButton.visibility = View.GONE
                mailButton.visibility = View.GONE
                currentStatus.visibility = View.GONE
                vampireCallBar.visibility = View.GONE}
        } catch (x: Exception) {
                writeToLog("refreshSuperUser FAIL.")
                Log.e("TLC.view.refreshSU", x.toString())
            }
        })
    }).start()

    fun broadcastRefresh() = Thread(Runnable {
        // try to touch View of UI thread
        this.runOnUiThread({
            try {
            fabLock=false
            fab.setImageResource(R.drawable.ok)
            if (binded) {
                if (service?.userState?.superusered != null) {
                    editor.putInt(APP_PREFERENCES_SUPERUSER, service?.userState?.superusered!!)
                    editor.apply()
                }

                if (service?.userState?.latitude != null)
                    netCoordinate.text = "lat = " + service?.userState?.latitude + ", lon = " + service?.userState?.longitude
                else {
                    netCoordinate.text = this.resources.getText(R.string.net_disable)
                }

                if (service?.userState?.lastConnected != null)
                    lastConnected.text = "Обновлено - " + service?.userState?.lastConnected + "."

                if (service?.userState?.zoneText != "") {
                    currentStatus.visibility = View.VISIBLE
                    currentStatus.text = service?.userState?.zoneText
                }

                if (service != null && service?.userState?.superusered != -1) {
                    superUserView = service?.userState?.superusered!!
                } else {
                    fab.show()
                }

                //Mail bar
                if (service != null && !service?.userState?.mail!!.isEmpty()) {
                    val mails = service?.userState?.mail?.iterator()
                    mailTextView.text = ""
                    while (mails!!.hasNext()) {
                        val find = mails.next()
                        mailTextView.text = mailTextView.text.toString() + find + "\n"
                    }
                }

                if (!service?.userState?.vampireSend.equals("0")) vampireSendCallStatus.text =
                        getString(com.shi.dayre.twilightclient2.R.string.vampireSendAlready) + service?.userState?.vampireSend
                else vampireSendCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireSendNoYet)

                if (!service?.userState?.vampireCall.equals("0")) {
                    vampireCallBar.visibility = View.VISIBLE
                    vampireCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireCallFrom) + service?.userState?.vampireCall
                } else {
                    vampireCallBar.visibility = View.GONE
                }

                Log.i("TLC.view", "View updated ok")
            }} catch (x: Exception) {
                writeToLog("refreshCommon FAIL.")
                Log.e("TLC.view.refreshCommon", x.toString())
            }})
    }).start()

    private fun serviceDisconnect() {
        service?.isMainActivityRunnig = true
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        serviceIntent.action = ACTION_DISCONNECT
        startService(serviceIntent)
    }

    private fun serviceConnect() {
        service?.isMainActivityRunnig = true
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        serviceIntent.action = ACTION_CONNECT
        startService(serviceIntent)
    }

    private fun serviceSetup() {
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        //This action needs, because binding is asynchron and not work as I want
        serviceIntent.action = ACTION_SETUP_CONNECTION
        serviceIntent.putExtra("url", newServer.text.toString())
        serviceIntent.putExtra("login", newLogin.text.toString())
        serviceIntent.putExtra("password", newPassword.text.toString())
        startService(serviceIntent)
    }

    private fun sendToServer(message: String) {
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        serviceIntent.putExtra("message", message)
        serviceIntent.action = ACTION_SEND_MESSAGE
        startService(serviceIntent)
    }

    private fun addListener() {
        newZonePinCoordinateButton.setOnClickListener {
            hideSoftKeyboard(this)
            if (mapbar.visibility == View.GONE) {
                gMap?.clear()
                service?.userState?.searchUserResult = ArrayList()
                service?.userState?.searchZoneResult = ArrayList()
                markerToMap = addDragableMarkerToMap(gMap, "", service?.userState?.latitude!!,
                        service?.userState?.longitude, "", resources, R.drawable.point)
                if (service?.userState?.longitude != null)
                    gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(service?.userState?.latitude!!,
                            service?.userState?.longitude!!), 12f))
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
        mailButton.setOnClickListener {
            addOnClickBarListener(mailBar)
            fab.hide()
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
                service?.userState?.searchUserResult = ArrayList()
                service?.userState?.searchZoneResult = ArrayList()
                gMap?.clear()
                isAddFrameOpen = true
                infoSearch = true
                val msg = "SEARCHUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA + service?.userState?.vampireCall + COMMA + "9)"
                sendToServer(msg)
                mapbar.visibility = View.VISIBLE
                mapInfoBar.visibility = View.VISIBLE
            } else {
                hideBar()
            }
        }
        searchAllButton.setOnClickListener {
            if (mapbar.visibility == View.GONE) {
                hideBar()
                gMap?.clear()
                service?.userState?.searchUserResult = ArrayList()
                service?.userState?.searchZoneResult = ArrayList()
                isAddFrameOpen = true
                val msg = "SCANUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA + "9)"
                sendToServer(msg)
                mapbar.visibility = View.VISIBLE
            } else {
                hideBar()
            }
        }

        fab.setOnClickListener {
            Log.i("TLC.view", "fab clicked")
            service?.isMainActivityRunnig = true
            hideSoftKeyboard(this)
            //TODO Change visibility to variable
            if (addnewzonebar.visibility == View.VISIBLE) {
                if (newZoneName.text.length > 1) {
                    if (newZoneTextForHuman.text.toString().length < 2) newZoneTextForHuman.setText(" ")
                    if (newZoneTextForLight.text.toString().length < 2) newZoneTextForLight.setText(" ")
                    if (newZoneTextForDark.text.toString().length < 2) newZoneTextForDark.setText(" ")
                    if (newZoneRadius.text.toString() == "") newZoneRadius.setText("0")
                    if (newZoneLatitude.text.toString() == "") newZoneLatitude.setText("0")
                    if (newZoneLongitude.text.toString() == "") newZoneLongitude.setText("0")
                    if (newZoneAchievement.text.toString().length < 2) newZoneAchievement.setText(" ")
                    if (newZoneSystem.text.toString() == "") newZoneSystem.setText("0")

                    if (newZonePriority.text.toString().equals("")) newZonePriority.setText("0")
                    val radius = metrToGradusToString(newZoneRadius.text.toString().toInt())
                    val msg = "ADDNEWZONE(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            newZoneName.text.toString() + COMMA + newZoneLatitude.text.toString() + COMMA + newZoneLongitude.text.toString() +
                            COMMA + radius + COMMA + newZoneTextForHuman.text.toString() + COMMA +
                            newZoneTextForLight.text.toString() + COMMA + newZoneTextForDark.text.toString() + COMMA +
                            newZonePriority.text.toString() + COMMA + newZoneAchievement.text.toString() + COMMA +
                            newZoneSystem.text.toString() + ")"
                    sendToServer(msg)
                    newZoneName.setText("")
                    newZoneLatitude.setText("")
                    newZoneLongitude.setText("")
                    newZoneTextForHuman.setText("")
                    newZoneTextForLight.setText("")
                    newZoneTextForDark.setText("")
                    newZoneAchievement.setText("")
                }
            } else if (dieUserBar.visibility == View.VISIBLE) {
                val msg = "DIE(" + service?.userState?.login + COMMA + service?.userState?.password + ")"
                sendToServer(msg)
                logOut()
            } else if (vampireSendBar.visibility == View.VISIBLE) {
                if (!vampireSendName.text.equals("")) {
                    val msg = "VAMPIRESEND(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            vampireSendName.text + COMMA
                    if (service?.userState?.vampireSend.equals("")) {
                        sendToServer(msg + "1" + ")")
                    } else {
                        sendToServer(msg + "0" + ")")
                    }
                }
            } else if (make1cursebar.visibility == View.VISIBLE) {
                if (!curse1UserName.text.equals("")) {
                    val msg = "MAKECURSE(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            curse1UserName.text + COMMA + "Проклятие 1" + ")"
                    sendToServer(msg)
                }
            } else if (searchUserBar.visibility == View.VISIBLE) {
                if (searchUserName.text.length > 1) {
                    searchLastconnect.text = ""
                    service?.userState?.searchUserResult = ArrayList()
                    service?.userState?.searchZoneResult = ArrayList()
                    gMap?.clear()
                    infoSearch = true
                    var position = searchSpinner.selectedItemPosition + 1
                    if (position > 3) position = 9 //For master power=9
                    val msg = "SEARCHUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            searchUserName.text + COMMA + position + ")"
                    sendToServer(msg)
                    mapbar.visibility = View.VISIBLE
                    mapInfoBar.visibility = View.VISIBLE
                }
            } else if (scanUserBar.visibility == View.VISIBLE) {
                service?.userState?.searchUserResult = ArrayList()
                service?.userState?.searchZoneResult = ArrayList()
                gMap?.clear()
                val position = scanSpinner.selectedItemPosition + 1
                val msg = "SCANUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA + position + ")"
                sendToServer(msg)
                mapbar.visibility = View.VISIBLE
            } else if (cursebar.visibility == View.VISIBLE) {
                if (!curseUserName.text.equals("")) {
                    val msg = "MAKECURSE(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            curseUserName.text + COMMA + curseCurseName.text + ")"
                    sendToServer(msg)
                }
            } else if (adduserbar.visibility == View.VISIBLE) {
                if (addUserName.text.length > 2) {
                    val msg = "ADDUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            addUserName.text + COMMA + addUserPass.text + COMMA + addUserPowerside.text + COMMA + addUserSuperuser.text + ")"
                    sendToServer(msg)
                }
            } else if (deletezonebar.visibility == View.VISIBLE) {
                if (deleteZoneName.text.length > 2) {
                    val msg = "DELETEZONE(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            deleteZoneName.text + ")"
                    sendToServer(msg)
                }
           }
            else {
                if (!fabLock) {
                try {
                    Log.i("TLC.view", "fab connect")
                        fabLock=true
                        fab.setImageResource(R.drawable.wait)
                        //save login and password
                        editor.putString(APP_PREFERENCES_USERNAME, newLogin.text.toString())
                        editor.putString(APP_PREFERENCES_PASSWORD, newPassword.text.toString())
                        editor.putString(APP_PREFERENCES_SERVER, newServer.text.toString())
                        editor.apply()
                        //start service
                        serviceSetup()
                        serviceConnect()
                    } catch (x: Exception) {
                        Log.e("TLC.connect", x.toString())
                    }
                }
            }
        }
    }

    private fun addOnClickBarListener(la: LinearLayout) {
        if (la.visibility == View.GONE) {
            hideBar()
            la.visibility = View.VISIBLE
            isAddFrameOpen = true
            fab.show()
        } else {
            hideBar()
        }
    }

    private fun loadSettings() {
        mSettings = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        editor = mSettings.edit()
        if (mSettings.contains(APP_PREFERENCES_USERNAME))
            newLogin.setText(mSettings.getString(APP_PREFERENCES_USERNAME, ""))
        if (mSettings.contains(APP_PREFERENCES_PASSWORD))
            newPassword.setText(mSettings.getString(APP_PREFERENCES_PASSWORD, ""))
        if (mSettings.contains(APP_PREFERENCES_SERVER))
            newServer.setText(mSettings.getString(APP_PREFERENCES_SERVER, DEFAULT_SERVER))
        else newServer.setText(DEFAULT_SERVER)
        if (mSettings.contains(APP_PREFERENCES_SUPERUSER)) {
            if (mSettings.getInt(APP_PREFERENCES_SUPERUSER, -1)>-1) {
                superUserView = mSettings.getInt(APP_PREFERENCES_SUPERUSER, -1)
                Log.i("TLC.view","Setting super user = "+mSettings.getInt(APP_PREFERENCES_SUPERUSER, -1))
                fab.hide()
                serviceSetup()
                serviceConnect()
            }
        }
    }

    private fun loadLayout() {
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

    private fun hideBar() {
        fab.hide()
        isAddFrameOpen = false
        mapInfoBar.visibility = View.GONE
        for (lay in listOfLayout) {
            lay.visibility = View.GONE
        }
    }

    private fun logOut() {
        Log.i("TLC.view", "log out")
        writeToLog("User log out")
        //unbindService(myConnection)
        //binded=false
        //LocalBroadcastManager.getInstance(this)
         //       .unregisterReceiver(broadcast)
        //Send message to server
        hideNotification(this, NOTIFICATION_CODE_ZONE)
        hideNotification(this, NOTIFICATION_CODE_VAMPIRE)
        val msg = "LOGOUT(" + service?.userState?.login + COMMA + service?.userState?.password + ")"
        sendToServer(msg)
        //Stop sending location
        serviceDisconnect()

        editor.putInt(APP_PREFERENCES_SUPERUSER, -1)
        editor.apply()

        superUserView=-1
        broadcastRefresh()
        broadcastRefreshSuperUser()
        //System.exit(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TLC.view", "onCreate")
        writeToLog("Activity started")
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        bindService(serviceIntent, myConnection, 0)

        broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i("TLC.broadcast","action = "+intent.getStringExtra("action"))
                when (intent.getStringExtra("action")) {
                    BROADCAST_NEED_TO_REFRESH -> broadcastRefresh()
                    BROADCAST_NEED_TO_REFRESH_MAP -> broadcastRefreshMap()
                    BROADCAST_NEED_TO_REFRESH_USERSTATE -> broadcastRefreshSuperUser()
                }
            }
        }
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcast, IntentFilter(BROADCAST))

        service?.isMainActivityRunnig = true

        loadLayout()
        addListener()
        loadSettings()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.searchmap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onBackPressed() {
        if (isAddFrameOpen) {
            hideBar()
        } else {
            if (superUserView!=-1) {
                val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            logOut()
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                        }
                    }
                }
                val builder = AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.exitYesNo)).setPositiveButton("Выход", dialogClickListener)
                        .setNegativeButton("Отмена", dialogClickListener).show()
            }
            else { System.exit(0)}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.i("TLC.view", "onResume")
        writeToLog("Activity resume")
        if (!isAnySensorEnable()) {
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        logOut()
                    }
                }
            }
            val builder = AlertDialog.Builder(this)
            builder.setMessage(this.getString(R.string.sensorOpen)).setPositiveButton("Да", dialogClickListener)
                    .setNegativeButton("Отмена", dialogClickListener).show()
        }
        service?.isMainActivityRunnig = true
        broadcastRefresh()
        broadcastRefreshSuperUser()

        fun showHello(){
            val dialogClickListener = DialogInterface.OnClickListener { _, _ -> }
            val builder2 = AlertDialog.Builder(this)
            builder2.setMessage(getString(R.string.newVersion)).setPositiveButton("Понятно", dialogClickListener)
                    .show()
            editor.putString(APP_PREFERENCES_ALREADY_VIEW_HELLO, CLIENT_VERSION)
        }
        if (mSettings.contains(APP_PREFERENCES_ALREADY_VIEW_HELLO)) {
            if (mSettings.getString(APP_PREFERENCES_ALREADY_VIEW_HELLO, "") != CLIENT_VERSION) {
              showHello()
            }
        } else {
            showHello()
        }

        hideSoftKeyboard(this)
    }

    override fun onPause() {
        super.onPause()
        writeToLog("Activity paused")
        if (!isAddFrameOpen) {
            service?.userState?.searchUserResult = ArrayList()
            service?.userState?.searchZoneResult = ArrayList()
            gMap?.clear()
        }
        service?.isMainActivityRunnig = false
    }

    override fun onDestroy() {
        super.onDestroy()
        service?.isMainActivityRunnig = true

        unbindService(myConnection)

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcast)

        Log.i("TLC.view", "onDestroy")
        writeToLog("Activity destroyed")
    }

    private fun isAnySensorEnable(): Boolean {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}




