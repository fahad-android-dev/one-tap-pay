package com.example.onetappay.interfaces

import java.net.Socket

interface MessageListener {
    fun onMessageReceived(message: String)
    fun onClientConnected(clientSocket: Socket?)
}