package com.shi.dayre.twilightclient2

import android.util.Log
import android.content.Context
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.text.SimpleDateFormat
import java.util.*


//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler(val context: Context) : Thread() {
    val JSON_COMMAND_NAME = "command"

    var fromServer: String = ""
    override fun run() {
        if (fromServer != null) {
            Log.i("WebClient.Proceed", "Proceed command " + fromServer)
//            val jsonObject = JSONObject(fromServer)
//            Log.i("WebClient.Proceed", "Proceed JSON command " + jsonObject.getString(JSON_COMMAND_NAME))
//            Log.i("WebClient.Proceed", "Proceed JSON name " + jsonObject.getString("name"))
            if (fromServer.equals("Password correct.")) user.logined = true

            if (fromServer.startsWith("ZONE(")) {
                if (!user.zoneText.equals(fromServer.getTextBetween().get(0)) && !fromServer.getTextBetween().get(0).equals("free_zone")) {
                    //Change zone
                    Log.i("WebClient", "NEW ZONE ALARM!")
                    user.justChangedZone = true
                    user.zoneText = fromServer.getTextBetween().get(0)
                    val formattedDate = SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime())
                    sendNotification(context, formattedDate + " " + context.getString(R.string.enterToNewZone), user.zoneText)
                   // var msg = "GETMAIL(" + user.login + COMMA + user.password + ")"
                   // wsj?.sendMessage(msg)
                }
            } else if (fromServer.startsWith("MAIL(")) {
                user.mail.add(fromServer.getTextBetween().get(0))
            } else if (fromServer.startsWith("SEARCHUSER(")) {
                user.searchUserResult.add(SearchUserResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toPowerside(),
                        fromServer.getTextBetween().get(4), fromServer.getTextBetween().get(5)
                ))
            } else if (fromServer.startsWith("SEARCHZONE(")) {
                user.searchZoneResult.add(SearchZoneResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toDouble(),
                        fromServer.getTextBetween().get(4).toInt(), fromServer.getTextBetween().get(5)
                ))
            } else if (fromServer.startsWith("VAMPIRESEND(")) {
                val n = fromServer.getTextBetween().get(0)
                if (n.equals("0")) user.vampireSend="0"
                else user.vampireSend=n
            } else if (fromServer.startsWith("VAMPIRECALL(")) {
                val n = fromServer.getTextBetween().get(0)
                if (n.equals("0")) user.vampireCall="0"
                else {
                    if (!user.vampireCall.equals(n))   sendNotification(context, context.getString(R.string.newVampireCall), "")
                    user.vampireCall = n
                }
            } else if (fromServer.startsWith("SUPERUSER(")) {
                val n = fromServer.getTextBetween().get(0).toInt()
                user.interfaceStyle=1
                Log.i("WebClient", "SuperUser granted = "+n)
                user.superusered = n
            } else if (fromServer.startsWith("MESSAGE(")) {
                Toaster.toast(messageFromServerCode(context,fromServer.getTextBetween().get(0)))
            }
                    else {
                    user.userText = fromServer
                }
                }
            }


        }
