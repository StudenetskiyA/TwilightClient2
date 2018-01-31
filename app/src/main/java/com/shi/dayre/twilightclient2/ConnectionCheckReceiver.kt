package com.shi.dayre.twilightclient2

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log


// Created by StudenetskiyA on 28.01.2018.

class ConnectionCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("TLC.connect","any connection status changed" )
        writeToLog("any connection status changed")
    }
}