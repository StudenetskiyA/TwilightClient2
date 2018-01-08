package com.shi.dayre.twilightclient2

import android.util.Log
import java.io.UnsupportedEncodingException
import android.content.Context

//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler (val context : Context){
    @Throws(UnsupportedEncodingException::class)
    fun  processCommand(fromServer: String?) {
        if (fromServer != null) {
            Log.i("WebClient","Proceed command "+fromServer)
            user.userText=user.userText+"\n"+fromServer
            if (fromServer.equals("Password correct.")) user.logined=true

            if (fromServer.startsWith("ZONE(")){
                if (!user.zoneText.equals(fromServer.getTextBetween().get(0)) && !fromServer.getTextBetween().get(0).equals("free_zone")){
                    //Change zone
                    Log.i("WebClient","NEW ZONE ALARM!")
                    user.justChangedZone=true
                    user.zoneText=fromServer.getTextBetween().get(0)
                    sendNotification(context,context.getString(R.string.enterToNewZone), user.zoneText)
                }
            }
        }
    }
}
