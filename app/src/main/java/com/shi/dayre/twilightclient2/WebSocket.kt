package com.shi.dayre.twilightclient2

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException

/**
 * Created by StudenetskiyA on 05.01.2018.
 */

class WebSocket (val url:String, val commandHandler:CommandFromServerHandler,val mView:MainActivity){
    private var mWebSocketClient: WebSocketClient? = null

    internal fun connectWebSocket() {
        val uri: URI
        try {
            uri = URI(url)

        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }


        mWebSocketClient = object : WebSocketClient(uri, Draft_17()) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i("Websocket", "Opened")
            }

            override fun onMessage(s: String) {
                Log.i("WebSocket get message", s)
                commandHandler.processCommand(s)
                mView.refresh()
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i("Websocket", "Closed " + s)
            }

            override fun onError(e: Exception) {
                Log.e("Websocket", "Error " + e.message)
            }
        }
        mWebSocketClient!!.connect()
    }

    fun sendMessage(txt: String) {
        mWebSocketClient!!.send(txt)
    }
}
