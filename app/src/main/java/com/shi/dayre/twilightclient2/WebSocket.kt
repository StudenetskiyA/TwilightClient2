package com.shi.dayre.twilightclient2

import android.util.Log
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException

/**
 * Created by StudenetskiyA on 05.01.2018.
 */

class WebSocket(val url: String, val commandHandler: CommandFromServerHandler, val mView: MainActivity) {
    private var connected: Boolean = false
    private var mWebSocketClient: WebSocketClient? = null
    var commandList = ArrayList<String>()

    internal fun connectWebSocket() {
        val uri: URI
        try {
            uri = URI(url)
        } catch (e: URISyntaxException) {
            Log.i("Websocket", "Server offline.")
            return
        }


        mWebSocketClient = object : WebSocketClient(uri, Draft_17()) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i("Websocket", "Opened")
                connected = true
            }

            override fun onMessage(s: String) {
                Log.i("Socket.onMessage", s)
                commandList.add(s)
                synchronized(syncLock) {
                    if (commandList.size == 1) {
                        val command = commandList.iterator()
                        while (command.hasNext()) {
                            var com = command.next()
                            Log.i("Socket.onHandle", com)
                            commandHandler.fromServer = com
                            commandHandler.run()
                            val job = launch { mView.refresh() }
                            syncLock.wait()
                            command.remove()
                        }
                    }
                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i("Websocket", "Closed " + s)
                connected = false
            }

            override fun onError(e: Exception) {
                Log.e("Websocket", "Error " + e.message)
                connected = false
            }
        }
        mWebSocketClient?.connect()
    }

    fun sendMessage(txt: String) {
        if (connected) mWebSocketClient?.send(txt)
    }
}
