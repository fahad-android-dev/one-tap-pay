package com.example.onetappay.mvvm.main.view

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.onetappay.R
import com.example.onetappay.databinding.ActivityMainBinding
import com.example.onetappay.helper.TCPServer
import com.example.onetappay.helper.WebSocketClient
import com.example.onetappay.interfaces.MessageListener
import com.example.onetappay.mvvm.main.adapter.ClientListAdapter
import java.io.OutputStream
import java.net.Socket

class MainActivity : AppCompatActivity(), MessageListener {
    private lateinit var tcpServer: TCPServer
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var binding: ActivityMainBinding
    private var outStream: OutputStream? = null
    private lateinit var socket : Socket
    private var adapter = ClientListAdapter()
    private  var arrListClients = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.rvClients.adapter = adapter

        initializeSocket()

    }

    private fun initializeSocket(){
        // Initialize and start TCP server
        tcpServer = TCPServer(8085,this)
        Thread {
            tcpServer.start()
        }.start()




        // Initialize and start WebSocket server
        webSocketClient = WebSocketClient(8085)
        webSocketClient.start()

        /* binding.btnSend.setOnClickListener {
             val message = binding.edtMessage.text.toString().trim()
             if (message.isNotEmpty()) {
                 binding.edtMessage.text.clear()
             }
         }*/
    }

    private fun updateClientList(clients: List<String>) {
        runOnUiThread {
            adapter.updateClients(clients)
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        tcpServer.stop()
        webSocketClient.stop()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            println("Received message in activity: $message")
            //tcpServer.handleMessage(message)
        }
    }

    override fun onClientConnected(clientSocket: Socket?) {
        Thread {
            try {
                outStream = clientSocket?.getOutputStream()
                if (clientSocket != null) {
                    socket = clientSocket
                    runOnUiThread {
                        tcpServer.observeClientList().observe(this) { clients ->
                            println("here is client list ${clients}")
                            arrListClients.addAll(clients)

                            updateClientList(clients)
                        }
                    }
                }
                println("Connected to server")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

}