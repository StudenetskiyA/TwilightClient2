package com.shi.dayre.twilightclient2

import android.util.Log
import android.content.Context
import kotlinx.coroutines.experimental.launch
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.text.SimpleDateFormat
import java.util.*


//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler(val context: Context, val service: FusedLocationService) : Thread() {
    val JSON_COMMAND_NAME = "command"

    var fromServer: String = ""

    private fun sendNeedToRefresh() {
        //May be create many function to refresh only that need to refresh, not all one time
        if (service.isForeground()) { //Refresh needs only is activity on top of view
            synchronized(service.syncLock) {
                launch { service.sendBroadcast(BROADCAST_NEED_TO_REFRESH) }
                service.syncLock.wait()
            }
        }
        else {
            //May be wake up activity? It needs?
        }
    }

    override fun run() {
        if (fromServer != null) {
            Log.i("WebClient.Proceed", "Proceed command " + fromServer)
//            val jsonObject = JSONObject(fromServer)
//            Log.i("WebClient.Proceed", "Proceed JSON command " + jsonObject.getString(JSON_COMMAND_NAME))
//            Log.i("WebClient.Proceed", "Proceed JSON name " + jsonObject.getString("name"))

            if (fromServer.startsWith("ZONE(")) {
                if (!service.userState.zoneText.equals(fromServer.getTextBetween().get(0)) && !fromServer.getTextBetween().get(0).equals("free_zone")) {
                    //Change zone
                    Log.i("WebClient", "NEW ZONE ALARM!")
                    service.userState.zoneText = fromServer.getTextBetween().get(0)
                    val formattedDate = SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime())
                    sendNotification(context, formattedDate + " " + context.getString(R.string.enterToNewZone), service.userState.zoneText)
                    sendNeedToRefresh()
                }
            } else if (fromServer.startsWith("MAIL(")) {
                service.userState.mail.add(fromServer.getTextBetween().get(0))
                sendNeedToRefresh()
            } else if (fromServer.startsWith("SEARCHUSER(")) {
                service.userState.searchUserResult.add(SearchUserResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toPowerside(),
                        fromServer.getTextBetween().get(4), fromServer.getTextBetween().get(5)
                ))
                sendNeedToRefresh()
            } else if (fromServer.startsWith("SEARCHZONE(")) {
                service.userState.searchZoneResult.add(SearchZoneResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toDouble(),
                        fromServer.getTextBetween().get(4).toInt(), fromServer.getTextBetween().get(5)
                ))
                sendNeedToRefresh()
            } else if (fromServer.startsWith("VAMPIRESEND(")) {
                val n = fromServer.getTextBetween().get(0)
                if (n.equals("0")) service.userState.vampireSend = "0"
                else service.userState.vampireSend = n
                sendNeedToRefresh()
            } else if (fromServer.startsWith("VAMPIRECALL(")) {
                val n = fromServer.getTextBetween().get(0)
                if (n.equals("0")) service.userState.vampireCall = "0"
                else {
                    if (!service.userState.vampireCall.equals(n)) sendNotification(context, context.getString(R.string.newVampireCall), "")
                    service.userState.vampireCall = n
                }
                sendNeedToRefresh()
            } else if (fromServer.startsWith("SUPERUSER(")) {
                val n = fromServer.getTextBetween().get(0).toInt()
                // service.userState.interfaceStyle = 1
                Log.i("WebClient", "SuperUser granted = " + n)
                service.userState.superusered = n
                sendNeedToRefresh()
            } else if (fromServer.startsWith("MESSAGE(")) {
                Toaster.toast(messageFromServerCode(context, fromServer.getTextBetween().get(0)))
            } else {
                // service.userState.userText = fromServer
            }
        }
    }


}
