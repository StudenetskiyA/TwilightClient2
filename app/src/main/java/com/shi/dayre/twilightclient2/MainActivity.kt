package com.shi.dayre.twilightclient2

import android.content.Intent
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

var user=User()
var location: com.shi.dayre.twilightclient2.LocationProvider?=null
val commandHandler = CommandFromServerHandler()
val address = "ws://192.168.1.198:8080/BHServer/serverendpoint"
var wsj:WebSocket?=null

class MainActivity : AppCompatActivity() {
    val mTimer = Timer()
    val mMyTimerTask =  onTimerTick(this)

    fun refresh(){
        Thread(Runnable {
            // try to touch View of UI thread
            this.runOnUiThread(java.lang.Runnable {
                Log.i("WebClient","View updated")
                textFromServer.text = user.userText
                if (user.location!=null)
                gpsCoordinate.setText(user.location!!.format())
                else
                    gpsCoordinate.text=this.resources.getText(R.string.gps_disable)
                if (user.locationNet!=null)
                    netCoordinate.setText(user.locationNet!!.format())
                else
                    netCoordinate.text=this.resources.getText(R.string.net_disable)
            })
        }).start()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        location =  com.shi.dayre.twilightclient2.LocationProvider(this,this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        wsj = WebSocket(address, commandHandler, this)

        wsj?.connectWebSocket()

        fab.setOnClickListener {
            try {
                sendLocationToServer()
                fab.hide()
            } catch (x: Exception) {
                println("Cloud not connect to server.")
            }
//            view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
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
        mTimer.schedule(mMyTimerTask, 1000, 5000);
    }

    fun sendLocationToServer(){
        //user.location?.latitude?.locationToInt() not equal null.locationToInt()
        // And I don't know why.
        val lat : Int = if (user.getBestLocation()?.latitude!=null) user.getBestLocation()?.latitude.locationToInt() else 0
        val lon : Int = if (user.getBestLocation()?.longitude!=null) user.getBestLocation()?.longitude.locationToInt() else 0
        var msg = "IAMHERE("+"Боб"+","+"12345"+","+lat+","+ lon+")"
        wsj?.sendMessage(msg)
    }

    class onTimerTick(val context:MainActivity):TimerTask(){
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




