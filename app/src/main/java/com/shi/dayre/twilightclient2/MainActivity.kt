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


val CONNECT_EVERY_SECOND: Long = 20
val COMMA = "|"
val APP_PREFERENCES = "mysettings"
val APP_PREFERENCES_USERNAME = "username"
val APP_PREFERENCES_PASSWORD = "password"
val APP_PREFERENCES_SERVER = "server"
val BROADCAST_NEED_TO_REFRESH = "com.shi.dayre.twilightclient2.needToRefresh"
val ACTION_CONNECT  = "connect"
val ACTION_SEND_MESSAGE = "send message"
val DEFAULT_SERVER = "ws://test1.uralgufk.ru:8080/BHServer/serverendpoint"


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {
    var service: FusedLocationService? = null
    var isBound = false
    val bindLock = java.lang.Object()

    lateinit var mSettings: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var broadcast: BroadcastReceiver

    private var isAddFrameOpen: Boolean = false
    private val listOfLayout = ArrayList<LinearLayout>()

    private var markerToMap: MarkerOptions? = null
    var gMap: GoogleMap? = null
    var infoSearch = false

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        _service: IBinder) {
            val binder = _service as FusedLocationService.MyLocalBinder
            service = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
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

    fun broadcastRefresh() = Thread(Runnable {
        // try to touch View of UI thread
        this.runOnUiThread({
          // testField.setText("Service report - "+service?.userState?.testCount)
            refresh()
        })
    }).start()

    fun refresh() = Thread(Runnable {
        // try to touch View of UI thread
        this.runOnUiThread({
            if (service?.userState?.latitude != null)
                netCoordinate.text = "lat = " + service?.userState?.latitude + "," + "lon = " + service?.userState?.longitude
            else {
                netCoordinate.text = this.resources.getText(R.string.net_disable)
            }

            if (service?.userState?.zoneText != "") {
                currentStatus.visibility = View.VISIBLE
                currentStatus.text = service?.userState?.zoneText
            }

            if (service?.userState?.superusered != -1) {
                //TODO Add more superuser types
                connectBar.visibility = View.GONE
                userbar.visibility = View.VISIBLE
                netCoordinate.visibility = View.VISIBLE
                if (service?.userState?.superusered!! > 0) {
                    searchUserButton.visibility = View.VISIBLE
                    addCurseButton.visibility = View.VISIBLE
                    scanUserButton.visibility = View.VISIBLE
                    vampireSendButton.visibility = View.VISIBLE
                    mailButton.visibility = View.VISIBLE
                }
                if (service?.userState?.superusered!! > 8) {
                    superuserbar.visibility = View.VISIBLE
                }
            }
            else {
                fab.show()
            }

            //Mail bar
            if (!service?.userState?.mail!!.isEmpty()) {
                mailTextView.text = ""
                for (txt in service?.userState?.mail!!)
                    mailTextView.text = mailTextView.text.toString() + txt + "\n"
            }

            //MapBar refresh if needs
            if (!service?.userState?.searchUserResult!!.isEmpty()) {

                if (infoSearch) {
                    mapInfoBar.visibility = View.VISIBLE
                    //One search, zoom to target
                    if (service?.userState?.longitude != null)
                        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(service?.userState?.searchUserResult?.get(0)?.latitude!!,
                                service?.userState?.searchUserResult?.get(0)?.longitude!!), 12f))

                }
                else {
                    mapInfoBar.visibility = View.GONE
                    //Many search, zoom to user
                    if (service?.userState?.longitude != null)
                        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(service?.userState?.latitude!!,
                                service?.userState?.longitude!!), 12f))
                }

                mapbar.visibility = View.VISIBLE
                val founded = service?.userState?.searchUserResult?.iterator()
                var find: SearchUserResult
                while (founded!!.hasNext()) {
                    find = founded!!.next()
                    val draw = if (find.powerSide == Light) R.drawable.light
                    else if (find.powerSide == Dark) R.drawable.dark
                    else R.drawable.human
                    if (find.curse.equals("0")) searchUserCurse.text = getString(R.string.searchUserCurse) + getString(com.shi.dayre.twilightclient2.R.string.No)
                    else searchUserCurse.text = getString(R.string.searchUserCurse) + find.curse
                    searchLastconnect.text = getString(R.string.searchUserLastConnected) + find.lastConnected
                    addMarkerToMap(gMap, find.name, find.latitude, find.longitude, "", resources, draw)
                }
                //And zones
                val foundedZ = service?.userState?.searchZoneResult!!.iterator()
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

                    Log.i("Socket.Radius", findZ.name + "=" + findZ.radius.toString())
                    addCircleToMap(gMap, findZ.latitude, findZ.longitude, findZ.radius * 111900, drawColor)
                }
            }

            if (!service?.userState?.vampireSend.equals("0")) vampireSendCallStatus.text =
                    getString(com.shi.dayre.twilightclient2.R.string.vampireSendAlready) + service?.userState?.vampireSend
            else vampireSendCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireSendNoYet)

            if (!service?.userState?.vampireCall.equals("0")) {
                vampireCallBar.visibility = View.VISIBLE
                vampireCallStatus.text = getString(com.shi.dayre.twilightclient2.R.string.vampireCallFrom) + service?.userState?.vampireCall
            }

            synchronized(service!!.syncLock) {
                service!!.syncLock.notify()
            }
            Log.i("TLC.view", "View updated ok")
        })
    }).start()

    fun startService() {
        Log.i("TLC.service", "try to start")
        service?.isMainActivityRunnig=true
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        serviceIntent.setAction(ACTION_CONNECT)
        startService(serviceIntent)
    }

    fun sendToServer(message: String) {
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        serviceIntent.putExtra("message", message)
        serviceIntent.setAction(ACTION_SEND_MESSAGE)
        startService(serviceIntent)
    }

    fun addListener() {
        newZonePinCoordinateButton.setOnClickListener {
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
            } else {
                hideBar()
            }
        }

        fab.setOnClickListener {
            Log.i("TLC.view", "fab clicked")
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
                }
            } else if (scanUserBar.visibility == View.VISIBLE) {
                service?.userState?.searchUserResult = ArrayList()
                service?.userState?.searchZoneResult = ArrayList()
                gMap?.clear()
                val position = scanSpinner.selectedItemPosition + 1
                val msg = "SCANUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA + position + ")"
                sendToServer(msg)
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
            } else if (searchbar.visibility == View.VISIBLE) {
                if (searchName.text.length > 1) {
                    searchLastconnect.text = ""
                    service?.userState?.searchUserResult = ArrayList()
                    service?.userState?.searchZoneResult = ArrayList()
                    gMap?.clear()
                    val msg = "SEARCHUSER(" + service?.userState?.login + COMMA + service?.userState?.password + COMMA +
                            searchName.text + COMMA + "9)"
                    sendToServer(msg)
                    searchName.setText("")
                }
            } else {
                try {
                    //Connect
                    fab.hide()
                    service?.userState?.login = newLogin.text.toString()
                    service?.userState?.password = newPassword.text.toString()
                    service?.userState?.url = newServer.text.toString()
                    editor.putString(APP_PREFERENCES_USERNAME, newLogin.text.toString())
                    editor.putString(APP_PREFERENCES_PASSWORD, newPassword.text.toString())
                    editor.putString(APP_PREFERENCES_SERVER, newServer.text.toString())
                    editor.apply()
                    startService()
                } catch (x: Exception) {
                    Log.e("TLC.connect", x.toString())
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
        for (lay in listOfLayout) {
            lay.visibility = View.GONE
        }
    }

    private fun logOut() {
        Log.i("TLC.view", "log out")
        //Send message to server
        val msg = "LOGOUT(" + service?.userState?.login + COMMA + service?.userState?.password + ")"
        sendToServer(msg)
        //Stop sending location
        val serviceIntent = Intent(this, FusedLocationService::class.java)
        stopService(serviceIntent)
        System.exit(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TLC.view", "onCreate")
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        loadLayout()
        addListener()
        loadSettings()

        val serviceIntent = Intent(this, FusedLocationService::class.java)
        bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE)

        broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                broadcastRefresh()
            }
        }
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcast, IntentFilter(BROADCAST_NEED_TO_REFRESH))

        val mapFragment = supportFragmentManager.findFragmentById(R.id.searchmap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onBackPressed() {
        if (isAddFrameOpen) {
            hideBar()
        } else {
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.i("TLC.view", "onResume")


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

        hideSoftKeyboard(this)
    }

    override fun onPause() {
        super.onPause()
        if (!isAddFrameOpen) {
            service?.userState?.searchUserResult = ArrayList()
            service?.userState?.searchZoneResult = ArrayList()
            gMap?.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("TLC.view", "onDestroy")
        if (service!=null) {
            service?.isMainActivityRunnig=false
        }

        unbindService(myConnection)

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcast)
    }

    private fun isAnySensorEnable(): Boolean {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}




