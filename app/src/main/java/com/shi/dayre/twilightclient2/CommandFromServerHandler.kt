package com.shi.dayre.twilightclient2

import android.util.Log
import java.io.UnsupportedEncodingException
import android.content.Context
import android.location.Location

//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler(val context : Context) : Thread()  {
    var fromServer:String =""
     override fun run() {
        if (fromServer != null) {
            Log.i("WebClient.Proceed","Proceed command "+fromServer)

            if (fromServer.equals("Password correct.")) user.logined=true

            if (fromServer.startsWith("ZONE(")){
                //It may have confused with case you first start app in not-free-zone :
                //You have two message in same time
                if (!user.zoneText.equals(fromServer.getTextBetween().get(0)) && !fromServer.getTextBetween().get(0).equals("free_zone")){
                    //Change zone
                    Log.i("WebClient","NEW ZONE ALARM!")
                    user.justChangedZone=true
                    user.zoneText=fromServer.getTextBetween().get(0)
                    sendNotification(context,context.getString(R.string.enterToNewZone), user.zoneText)
                }
            }
            else if (fromServer.startsWith("SEARCHUSER(")){
                user.searchUserResult.add(SearchUserResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toPowerside(),
                        fromServer.getTextBetween().get(4)
                ))
            }
            else if (fromServer.startsWith("SEARCHZONE(")){
                user.searchZoneResult.add(SearchZoneResult(
                        fromServer.getTextBetween().get(0), fromServer.getTextBetween().get(1).toDouble(),
                        fromServer.getTextBetween().get(2).toDouble(), fromServer.getTextBetween().get(3).toDouble(),
                        fromServer.getTextBetween().get(4).toInt(),fromServer.getTextBetween().get(5)
                ))
            }
            else if (fromServer.startsWith("SUPERUSER(")){
                Log.i("WebClient","SuperUser granted.")
                user.superusered=true
            }
            else {
                user.userText=fromServer
            }
        }
    }
}
