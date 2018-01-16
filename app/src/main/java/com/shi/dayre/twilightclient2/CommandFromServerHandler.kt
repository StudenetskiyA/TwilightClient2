package com.shi.dayre.twilightclient2

import android.app.Activity
import android.util.Log
import java.io.UnsupportedEncodingException
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import xdroid.toaster.Toaster


//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler(val context: Context) : Thread() {
    var fromServer: String = ""
    override fun run() {
        if (fromServer != null) {
            Log.i("WebClient.Proceed", "Proceed command " + fromServer)

            if (fromServer.equals("Password correct.")) user.logined = true

            if (fromServer.startsWith("ZONE(")) {
                if (!user.zoneText.equals(fromServer.getTextBetween().get(0)) && !fromServer.getTextBetween().get(0).equals("free_zone")) {
                    //Change zone
                    Log.i("WebClient", "NEW ZONE ALARM!")
                    user.justChangedZone = true
                    user.zoneText = fromServer.getTextBetween().get(0)
                    sendNotification(context, context.getString(R.string.enterToNewZone), user.zoneText)
                    var msg = "GETMAIL(" + user.login + COMMA + user.password + ")"
                    wsj?.sendMessage(msg)
                }
            } else if (fromServer.startsWith("SEARCHUSER(")) {
                user.searchUserResult.add(SearchUserResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toPowerside(),
                        fromServer.getTextBetween().get(4)
                ))
            } else if (fromServer.startsWith("SEARCHZONE(")) {
                user.searchZoneResult.add(SearchZoneResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toDouble(),
                        fromServer.getTextBetween().get(4).toInt(), fromServer.getTextBetween().get(5)
                ))
            } else if (fromServer.startsWith("SUPERUSER(")) {
                val n = fromServer.getTextBetween().get(0).toInt()
                Log.i("WebClient", "SuperUser granted = "+n)
                user.superusered = n
            } else if (fromServer.startsWith("MESSAGE(")) {
                Toaster.toast(fromServer.getTextBetween().get(0));
            }
                    else {
                    user.userText = fromServer
                }
                }
            }


        }
