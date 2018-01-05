package com.shi.dayre.twilightclient2;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by samsung on 05.01.2018.
 */

public class WebSocketJava {
    private WebSocketClient mWebSocketClient;

    void connectWebSocket() {
        URI uri;
        try {
            uri = new URI(                    "ws://192.168.1.198:8080/BHServer/serverendpoint");

        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }


        mWebSocketClient = new WebSocketClient(uri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " "
                        + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("WebSocket get message", message);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    public void sendMessage(String txt) {
        mWebSocketClient.send(txt);
    }
}
