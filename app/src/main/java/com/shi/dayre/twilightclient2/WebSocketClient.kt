//package com.shi.dayre.twilightclient2
//
//import android.app.Activity
//
//import java.io.UnsupportedEncodingException
//import java.net.URI
//import org.java_websocket.client.WebSocketClient;
//import org.java_websocket.handshake.ServerHandshake;
//
////import javax.websocket.*
////
////import javax.websocket.ClientEndpoint
////import javax.websocket.ContainerProvider
////import javax.websocket.OnClose
////import javax.websocket.OnError
////import javax.websocket.OnMessage
////import javax.websocket.OnOpen
////import javax.websocket.Session
//
////@ClientEndpoint
//
//class WebsocketClient private constructor(endpointURI: URI, cycleReadFromServer: CommandFromServerReceiver) {
//
//    var userSession:Session? = null
//
//    internal var commandHandler: CommandFromServerReceiver
//
//    internal var monitor = Any()
//
//    init {
//        try {
//            val container = ContainerProvider.getWebSocketContainer()
//            container.connectToServer(this, endpointURI)
//            this.commandHandler = cycleReadFromServer
//        } catch (e: Exception) {
//            throw RuntimeException(e)
//        }
//
//    }
//
//    @OnOpen
//    fun onOpen(userSession: Session) {
//        println("session started")
//        this.userSession = userSession
//    }
//
//    @OnClose
//    fun onClose(userSession: Session) {
//        println("session closed")
//    }
//
//    @OnMessage
//    @Throws(UnsupportedEncodingException::class)
//    fun onMessage(message: String) {
//
//        synchronized(monitor) {
//           // Main.writerToLog.println(message)
//            commandHandler.processCommand(message)
//        }
//    }
//
//    fun sendMessage(message: String) {
//        println("Send message: " + message)
//        this.userSession?.asyncRemote?.sendText(message)
//    }
//
//    @OnError
//    fun onError(th: Throwable) {
//        println("error: " + th.message)
//        th.printStackTrace()
//    }
//
//    companion object {
//
//        internal var client: WebsocketClient? = null
//
//        fun connect(url: String, cycleReadFromServer: CommandFromServerReceiver) {
//            println("Try to connect url: " + url)
//            client = WebsocketClient(URI.create(url), cycleReadFromServer)
//        }
//    }
//}
