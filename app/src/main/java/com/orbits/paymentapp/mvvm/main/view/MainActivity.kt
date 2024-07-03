package com.orbits.paymentapp.mvvm.main.view

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.ActivityMainBinding
import com.orbits.paymentapp.helper.AlertDialogInterface
import com.orbits.paymentapp.helper.BaseActivity
import com.orbits.paymentapp.helper.Constants
import com.orbits.paymentapp.helper.Dialogs
import com.orbits.paymentapp.helper.TCPServer
import com.orbits.paymentapp.helper.WebSocketClient
import com.orbits.paymentapp.interfaces.CommonInterfaceClickEvent
import com.orbits.paymentapp.interfaces.MessageListener
import com.orbits.paymentapp.mvvm.main.adapter.ClientListAdapter
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

class MainActivity : BaseActivity(), MessageListener {
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
       // binding.rvClients.adapter = adapter

        initializeSocket()
        initializeNearPay()
        initializeToolbar()

    }

    private fun initializeToolbar(){
        setUpToolbar(
            binding.layoutToolbar,
            title = getString(R.string.app_name),
            isBackArrow = false,
            toolbarClickListener = object : CommonInterfaceClickEvent{
                override fun onToolBarListener(type: String) {
                    if (type == Constants.TOOLBAR_ICON_ONE){
                        Dialogs.showPasswordDialog(
                            activity = this@MainActivity,
                            alertDialogInterface = object : AlertDialogInterface{

                            }
                        )
                    }
                }
            }
        )
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
            .authenticationData(AuthenticationData.Email("development@aflak.com.sa"))
            .environment(Environments.SANDBOX)
            .locale(Locale.getDefault())
            .networkConfiguration(NetworkConfiguration.DEFAULT)
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