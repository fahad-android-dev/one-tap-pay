package com.orbits.paymentapp.interfaces

import java.net.Socket

interface MessageListener {
    fun onMessageReceived(message: String)
    fun onClientConnected(clientSocket: Socket?)
}