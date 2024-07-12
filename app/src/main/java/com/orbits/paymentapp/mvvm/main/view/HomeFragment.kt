package com.orbits.paymentapp.mvvm.main.view

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.FragmentHomeBinding
import com.orbits.paymentapp.helper.AlertDialogInterface
import com.orbits.paymentapp.helper.BaseFragment
import com.orbits.paymentapp.helper.Constants
import com.orbits.paymentapp.helper.Dialogs
import com.orbits.paymentapp.helper.Extensions.asDouble
import com.orbits.paymentapp.helper.Global.showSnackBar
import com.orbits.paymentapp.helper.PrefUtils.getUserDataResponse
import com.orbits.paymentapp.helper.PrefUtils.isCodeVerified
import com.orbits.paymentapp.helper.PrefUtils.setUserDataResponse
import com.orbits.paymentapp.helper.TCPServer
import com.orbits.paymentapp.helper.WebSocketClient
import com.orbits.paymentapp.helper.helper_model.UserDataModel
import com.orbits.paymentapp.helper.helper_model.UserResponseModel
import com.orbits.paymentapp.interfaces.CommonInterfaceClickEvent
import com.orbits.paymentapp.interfaces.MessageListener
import com.orbits.paymentapp.mvvm.main.adapter.ClientListAdapter
import com.orbits.paymentapp.mvvm.main.model.ClientDataModel
import com.orbits.paymentapp.mvvm.main.model.SuccessPurchaseDataModel
import io.nearpay.sdk.Environments
import io.nearpay.sdk.NearPay
import io.nearpay.sdk.utils.PaymentText
import io.nearpay.sdk.utils.enums.AuthenticationData
import io.nearpay.sdk.utils.enums.NetworkConfiguration
import io.nearpay.sdk.utils.enums.PurchaseFailure
import io.nearpay.sdk.utils.enums.TransactionData
import io.nearpay.sdk.utils.enums.UIPosition
import io.nearpay.sdk.utils.listeners.BitmapListener
import io.nearpay.sdk.utils.listeners.PurchaseListener
import java.io.OutputStream
import java.net.Socket
import java.util.Locale
import java.util.UUID


class HomeFragment : BaseFragment(), MessageListener {
    private lateinit var mActivity: MainActivity
    private lateinit var tcpServer: TCPServer
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var binding: FragmentHomeBinding
    private var outStream: OutputStream? = null
    private lateinit var socket : Socket
    private var adapter = ClientListAdapter()
    private  var arrListClients = ArrayList<String>()
    private lateinit var nearpay : NearPay
    private var clientModel = ClientDataModel()
    val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_home,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSocket()
        initializeToolbar()
        initializeNearPay()
    }

    private fun initializeToolbar(){
        setUpToolbar(
            binding.layoutToolbar,
            title = getString(R.string.app_name),
            isBackArrow = false,
            navController = findNavController(),
            toolbarClickListener = object : CommonInterfaceClickEvent {
                override fun onToolBarListener(type: String) {
                    if (type == Constants.TOOLBAR_ICON_ONE){
                        Dialogs.showPasswordDialog(
                            activity = mActivity,
                            alertDialogInterface = object : AlertDialogInterface {
                                override fun onYesClick() {
                                    findNavController().navigate(R.id.action_to_navigation_settings)
                                }
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
            .context(mActivity)
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
        mActivity.runOnUiThread {
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
        /*mActivity.runOnUiThread {
            println("Received message in activity: $message")
            callPurchase(message)
        }*/
    }

    override fun onMessageJsonReceived(json: JsonObject) {
        mActivity.runOnUiThread {
            if (!json.isJsonNull){
                println("Received json in activity: $json")

                clientModel = ClientDataModel(

                    code = json.get("code")?.asString ?: "",
                    amount = json.get("amount")?.asString ?: "",
                    client_id = json.get("client_id")?.asString ?: "",
                    transaction_id = json.get("transaction_id")?.asString ?: "",
                    time = json.get("time")?.asString ?: "",
                    desc = json.get("desc")?.asString ?: "",
                    currency = json.get("currency")?.asString ?: "",
                    transaction_type = json.get("transaction_type")?.asString ?: "",
                )

                val code = clientModel.code
                val amount = clientModel.amount

                if (mActivity.isCodeVerified()){
                    if (code?.isEmpty() == true){
                        callPurchase(amount.asDouble())
                    }
                }else {
                    if (code == mActivity.getUserDataResponse()?.code){
                        binding.root.showSnackBar("Client Connected")
                        mActivity.setUserDataResponse(
                            UserResponseModel(
                                code = mActivity.getUserDataResponse()?.code,
                                data = UserDataModel(
                                    isCodeVerified = true
                                )
                            )
                        )
                    }else{
                        socket.close()
                        binding.root.showSnackBar("Client Disconnected")
                    }
                }
            }else {
                socket.close()
                binding.root.showSnackBar("Client Disconnected")
            }

        }
    }

    override fun onClientDisconnected() {
        mActivity.setUserDataResponse(
            UserResponseModel(
                code = mActivity.getUserDataResponse()?.code,
                data = UserDataModel(
                    isCodeVerified = false
                )
            )
        )
    }

    override fun onClientConnected(clientSocket: Socket?) {
        Thread {
            try {
                outStream = clientSocket?.getOutputStream()
                if (clientSocket != null) {
                    socket = clientSocket
                    mActivity.runOnUiThread {
                        tcpServer.observeClientList().observe(this) { clients ->
                            binding.root.showSnackBar("Client Connected")
                            arrListClients.addAll(clients)

                        }
                    }
                }
                println("Connected to server")
            } catch (e: Exception) {

                e.printStackTrace()
            }
        }.start()
    }

    private fun callPurchase(amount: Double){
        println("here is client start 0000 ${arrListClients.size}")

        val customerReferenceNumber = "9ace70b7-977d-4094-b7f4-4ecb17de6753"
        val enableReceiptUi = true
        val enableReversal = true
        val finishTimeOut : Long = 10
        val requestId = UUID.randomUUID()
        val enableUiDismiss = true
        val nearpayAmount = (amount * 100).toLong()

        nearpay.purchase(nearpayAmount, customerReferenceNumber, enableReceiptUi, enableReversal, finishTimeOut, requestId, enableUiDismiss, object :
            PurchaseListener {

            override fun onPurchaseApproved(transactionData: TransactionData) {
                val jsonObject = JsonObject()
                println("here is transaction data $transactionData")
                jsonObject.add("transactionData", gson.toJsonTree(transactionData))
                arrListClients.forEach {
                    sendMessageToWebSocketClient(it, jsonObject)
                }
            }


            override fun onPurchaseFailed(purchaseFailure: PurchaseFailure) {
                when (purchaseFailure) {
                    is PurchaseFailure.PurchaseDeclined -> {
                        println("here is 1111")
                        println("here is ${purchaseFailure.transactionData}")
                        val jsonObject = JsonObject()
                        jsonObject.add("transactionData", gson.toJsonTree(purchaseFailure.transactionData))
                        arrListClients.forEach {
                            sendMessageToWebSocketClient(it, jsonObject)
                        }

                    }

                    is PurchaseFailure.PurchaseRejected -> {

                        println("here is 222")
                        arrListClients.forEach {
                            println("here is ${purchaseFailure.message}")
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("status_message","failure")
                            jsonObject.addProperty("description", purchaseFailure.message)
                            println("here is purchase rejected")
                            sendMessageToWebSocketClient(it,jsonObject)
                        }
                    }

                    is PurchaseFailure.AuthenticationFailed -> {

                        println("here is client start 2222 ${arrListClients.size}")
                        println("here is 333")
                        println("here is ${purchaseFailure.message}")
                        println("here is client list ${arrListClients.size}")
                        println("here is client list 111 $arrListClients")
                        nearpay.updateAuthentication(AuthenticationData.Jwt("JWT HERE"))
                        arrListClients.forEach {
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("status_message","failure")
                            println("here is purchase rejected ${jsonObject}")
                            sendMessageToWebSocketClient(it,jsonObject)
                        }
                    }

                    is PurchaseFailure.InvalidStatus -> {
                        println("here is 4444")
                        arrListClients.forEach {
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("status_message","failure")
                            sendMessageToWebSocketClient(it,jsonObject)
                        }
                    }

                    is PurchaseFailure.GeneralFailure -> {
                        arrListClients.forEach {
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("status_message","failure")
                            sendMessageToWebSocketClient(it,jsonObject)
                        }
                    }

                    is PurchaseFailure.UserCancelled -> {
                        arrListClients.forEach {
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("status_message","failure")
                            sendMessageToWebSocketClient(it,jsonObject)
                        }
                    }
                }
            }
        })

    }


    private fun sendMessageToWebSocketClient(clientId: String, jsonObject: JsonObject) {
        val clientHandler = TCPServer.WebSocketManager.getClientHandler(clientId)
        if (clientHandler != null && clientHandler.isWebSocket) {
            Thread{
                val jsonMessage = gson.toJson(jsonObject)
                clientHandler.sendMessageToClient(clientId, jsonMessage)
            }.start()
            // Optionally handle success or error
        } else {
            // Handle case where clientHandler is not found or not a WebSocket client
        }
    }

}