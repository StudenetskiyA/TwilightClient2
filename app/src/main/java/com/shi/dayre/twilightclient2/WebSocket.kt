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

class WebSocket(val url: String, val commandHandler: CommandFromServerHandler) {
    var connected: Boolean = false
    private var mWebSocketClient: WebSocketClient? = null
    var commandList = ArrayList<String>()

    fun disconnectWebSocket() {
        connected=false
        mWebSocketClient?.close()
    }

    fun connectWebSocket() {
        val uri: URI
        try {
            uri = URI(url)
        } catch (e: URISyntaxException) {
            Log.i("TLC.connect", "Server offline.")
            writeToLog("Server error connect")
            return
        }


        mWebSocketClient = object : WebSocketClient(uri, Draft_17()) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i("TLC.connect", "Opened")
                connected = true
            }

            override fun onMessage(s: String) {
                commandList.add(s)
                    if (commandList.size == 1) { //Handle command one by one
                        val command = commandList.iterator()
                        while (command.hasNext()) {
                            var com = command.next()
                            Log.i("TLC.connect.onHandle", com)
                            writeToLog("Handle message from server - "+com)
                            commandHandler.fromServer = com
                            commandHandler.run()
                            command.remove()
                        }
                    }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i("TLC.connect", "Closed " + s)
                connected = false
            }

            override fun onError(e: Exception) {
                Log.e("TLC.connect", "Error " + e.message)
                connected = false
            }
        }
        mWebSocketClient?.connect()
    }

    fun sendMessage(txt: String) {
        Log.i("TLC.connect", "Message to server " + txt)
        if (connected) mWebSocketClient?.send(txt)
    }
}
