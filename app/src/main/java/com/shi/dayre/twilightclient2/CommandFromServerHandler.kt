package com.shi.dayre.twilightclient2

import android.util.Log
import java.io.UnsupportedEncodingException

//Created by StudenetskiyA on 27.01.2017.

class CommandFromServerHandler (){
    @Throws(UnsupportedEncodingException::class)
    fun  processCommand(fromServer: String?) {
        if (fromServer != null) {
            Log.i("WebClient","Proceed command "+fromServer)
            user.userText=user.userText+"\n"+fromServer
            if (fromServer.equals("Password correct.")) user.logined=true
            //fromSite?.setText(fromSite?.text.toString()+"/n"+fromServer)
        }
    }
}
