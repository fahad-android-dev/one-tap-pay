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
import io.nearpay.sdk.Environments
import io.nearpay.sdk.NearPay
import io.nearpay.sdk.utils.PaymentText
import io.nearpay.sdk.utils.enums.AuthenticationData
import io.nearpay.sdk.utils.enums.NetworkConfiguration
import io.nearpay.sdk.utils.enums.PurchaseFailure
import io.nearpay.sdk.utils.enums.TransactionData
import io.nearpay.sdk.utils.enums.UIPosition
import io.nearpay.sdk.utils.listeners.PurchaseListener
import java.io.OutputStream
import java.net.Socket
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), MessageListener {
    private lateinit var tcpServer: TCPServer
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var binding: ActivityMainBinding
    private var outStream: OutputStream? = null
    private lateinit var socket : Socket
    private var adapter = ClientListAdapter()
    private  var arrListClients = ArrayList<String>()
    private lateinit var nearpay : NearPay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.rvClients.adapter = adapter

        initializeSocket()
        initializeNearPay()

    }

    private fun initializeSocket(){
        tcpServer = TCPServer(8085,this)
        Thread {
            tcpServer.start()
        }.start()

        webSocketClient = WebSocketClient(8085)
        webSocketClient.start()

        /* binding.btnSend.setOnClickListener {
             val message = binding.edtMessage.text.toString().trim()
             if (message.isNotEmpty()) {
                 binding.edtMessage.text.clear()
             }
         }*/
    }

    private fun initializeNearPay(){
        nearpay = NearPay.Builder()
            .context(this)
            .authenticationData(AuthenticationData.UserEnter)
            .environment(Environments.SANDBOX)
            .locale(Locale.getDefault())
            .networkConfiguration(NetworkConfiguration.SIM_PREFERRED)
            .uiPosition(UIPosition.CENTER_BOTTOM)
            .paymentText(PaymentText("يرجى تمرير الطاقة", "please tap your card"))
            .loadingUi(true)
            .build()
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
            callPurchase(message)
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

    private fun callPurchase(amount:String){
        val customerReferenceNumber = "9ace70b7-977d-4094-b7f4-4ecb17de6753"
        val enableReceiptUi = true
        val enableReversal = true
        val finishTimeOut : Long = 10
        val requestId = UUID.randomUUID()
        val enableUiDismiss = true

        nearpay.purchase(amount.toLong(), customerReferenceNumber, enableReceiptUi, enableReversal, finishTimeOut, requestId, enableUiDismiss, object :
            PurchaseListener {

            override fun onPurchaseApproved(transactionData: TransactionData) {}

            override fun onPurchaseFailed(purchaseFailure: PurchaseFailure) {
                when (purchaseFailure) {
                    is PurchaseFailure.PurchaseDeclined -> {

                    }

                    is PurchaseFailure.PurchaseRejected -> {}

                    is PurchaseFailure.AuthenticationFailed -> {
                        nearpay.updateAuthentication(AuthenticationData.Jwt("JWT HERE"))
                    }

                    is PurchaseFailure.InvalidStatus -> {}

                    is PurchaseFailure.GeneralFailure -> {}

                    is PurchaseFailure.UserCancelled -> {}
                }
            }
        })
    }

}