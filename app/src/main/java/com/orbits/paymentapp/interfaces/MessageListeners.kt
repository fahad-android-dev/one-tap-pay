package com.orbits.paymentapp.interfaces

import com.google.gson.JsonObject
import java.net.Socket

interface MessageListener {
    fun onMessageReceived(message: String)
    fun onMessageJsonReceived(json: JsonObject)
    fun onClientConnected(clientSocket: Socket?,clientList: List<String>)
    fun onClientDisconnected()
}