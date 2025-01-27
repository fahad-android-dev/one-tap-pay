package com.orbits.paymentapp.mvvm.main.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
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
import com.orbits.paymentapp.helper.Global
import com.orbits.paymentapp.helper.Global.showSnackBar
import com.orbits.paymentapp.helper.PrefUtils.getAppPassword
import com.orbits.paymentapp.helper.PrefUtils.getMasterKey
import com.orbits.paymentapp.helper.PrefUtils.getUserDataResponse
import com.orbits.paymentapp.helper.PrefUtils.isCodeVerified
import com.orbits.paymentapp.helper.PrefUtils.setAppConfig
import com.orbits.paymentapp.helper.PrefUtils.setAppPassword
import com.orbits.paymentapp.helper.PrefUtils.setMasterKey
import com.orbits.paymentapp.helper.PrefUtils.setUserDataResponse
import com.orbits.paymentapp.helper.ServerService
import com.orbits.paymentapp.helper.TCPServer
import com.orbits.paymentapp.helper.WebSocketClient
import com.orbits.paymentapp.helper.helper_model.AppConfigModel
import com.orbits.paymentapp.helper.helper_model.AppMasterKeyModel
import com.orbits.paymentapp.helper.helper_model.PasswordModel
import com.orbits.paymentapp.helper.helper_model.UserDataModel
import com.orbits.paymentapp.helper.helper_model.UserResponseModel
import com.orbits.paymentapp.interfaces.CommonInterfaceClickEvent
import com.orbits.paymentapp.interfaces.MessageListener
import com.orbits.paymentapp.mvvm.main.adapter.ClientListAdapter
import com.orbits.paymentapp.mvvm.main.model.ClientDataModel
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


class HomeFragment : BaseFragment(), MessageListener{
    private lateinit var mActivity: MainActivity
    private lateinit var tcpServer: TCPServer
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var binding: FragmentHomeBinding
    private var outStream: OutputStream? = null
    private lateinit var socket: Socket
    private var adapter = ClientListAdapter()
    private var arrListClients = ArrayList<String>()
    private lateinit var nearpay: NearPay
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

        println("here is 111 ${mActivity.getMasterKey()?.masterKey}")
        if (mActivity.getMasterKey()?.masterKey == null){
            Global.getRandomDeviceId()
            mActivity.setMasterKey(
                result = AppMasterKeyModel(
                    masterKey = Global.getRandomDeviceId()
                )
            )

        }
        if (mActivity.getAppPassword()?.appPassword == null){
            mActivity.setAppPassword(
                result = PasswordModel(
                    appPassword = "1234"
                )
            )
        }

        startServerService()
        initializeToolbar()
        initializeNearPay()
    }

    private fun initializeToolbar() {
        setUpToolbar(
            binding.layoutToolbar,
            title = getString(R.string.app_name),
            isBackArrow = false,
            navController = findNavController(),
            toolbarClickListener = object : CommonInterfaceClickEvent {
                override fun onToolBarListener(type: String) {
                    if (type == Constants.TOOLBAR_ICON_ONE) {
                        val appSalt = byteArrayOf(17, 43, 99, 82, 55, 28, 40, 90)
                        val masterPassword =  Global.getMasterKey(mActivity,appSalt)

                        println("here is master password $masterPassword")

                        Dialogs.showPasswordDialog(
                            activity = mActivity,
                            alertDialogInterface = object : AlertDialogInterface {
                                override fun onYesClick() {
                                  findNavController().navigate(R.id.action_to_navigation_settings)
                                }

                                override fun onMasterYesClick() {
                                    val code = Global.getRandomDeviceId()
                                    mActivity.setMasterKey(
                                        result = AppMasterKeyModel(
                                            masterKey = code
                                        )
                                    )
                                    Dialogs.showChangePasswordDialog(
                                        activity = mActivity,
                                        alertDialogInterface = object : AlertDialogInterface {
                                            override fun onSubmitPasswordClick(password: String) {
                                                mActivity.setAppPassword(
                                                    result = PasswordModel(
                                                        appPassword = password
                                                    )
                                                )
                                                findNavController().navigate(R.id.action_to_navigation_settings)
                                            }

                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    private fun startServerService(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityCompat.requestPermissions(
                mActivity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        Intent(mActivity.applicationContext, ServerService::class.java).also { intent ->
            intent.action = ServerService.Actions.START.toString()
            mActivity.startService(intent)
            println("here is service started")
            initializeSocket()
        }

    }

    private fun initializeSocket(){
        tcpServer = TCPServer(8085,this)
        Thread {
            tcpServer.start()
        }.start()

        webSocketClient = WebSocketClient(8085)
        webSocketClient.start()

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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: String) {

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

                if (mActivity.isCodeVerified()) {
                    if (code?.isEmpty() == true) {
                        callPurchase(amount.asDouble())
                    }
                } else {
                    if (code == mActivity.getUserDataResponse()?.code) {
                        binding.root.showSnackBar("Client Connected")
                        mActivity.setUserDataResponse(
                            UserResponseModel(
                                code = mActivity.getUserDataResponse()?.code,
                                data = UserDataModel(
                                    isCodeVerified = true
                                )
                            )
                        )
                    } else {
                        socket.close()
                        binding.root.showSnackBar("Client Disconnected")
                    }
                }
            } else {
                socket.close()
                binding.root.showSnackBar("Client Disconnected")
            }

        }
    }

    override fun onClientConnected(clientSocket: Socket?, clientList: List<String>) {
        Thread {
            try {
                outStream = clientSocket?.getOutputStream()
                if (clientSocket != null) {
                    socket = clientSocket
                    mActivity.runOnUiThread {
                        binding.root.showSnackBar("Client Connected")
                        println("here is client list fahad ${clientList}")
                        arrListClients.clear()
                        arrListClients.addAll(clientList)
                        println("here is client list fahad 111 ${arrListClients}")
                    }
                }
                println("Connected to server")
            } catch (e: Exception) {

                e.printStackTrace()
            }
        }.start()
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


    private fun callPurchase(amount: Double) {
        println("here is client start 0000 ${arrListClients.size}")

        val customerReferenceNumber = "9ace70b7-977d-4094-b7f4-4ecb17de6753"
        val enableReceiptUi = true
        val enableReversal = true
        val finishTimeOut: Long = 10
        val requestId = UUID.randomUUID()
        val enableUiDismiss = true
        val nearpayAmount = (amount * 100).toLong()

        nearpay.purchase(
            nearpayAmount,
            customerReferenceNumber,
            enableReceiptUi,
            enableReversal,
            finishTimeOut,
            requestId,
            enableUiDismiss,
            object :
                PurchaseListener {

            override fun onPurchaseApproved(transactionData: TransactionData) {
                val jsonObject = JsonObject()
                println("here is transaction data $transactionData")
                println("here is arrListClients $arrListClients")
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
                            jsonObject.add(
                                "transactionData",
                                gson.toJsonTree(purchaseFailure.transactionData)
                            )
                            arrListClients.forEach {
                                sendMessageToWebSocketClient(it, jsonObject)
                            }

                        }

                        is PurchaseFailure.PurchaseRejected -> {

                            println("here is 222")
                            arrListClients.forEach {
                                println("here is ${purchaseFailure.message}")
                                val jsonObject = JsonObject()
                                jsonObject.addProperty("status_message", "failure")
                                jsonObject.addProperty("description", purchaseFailure.message)
                                println("here is purchase rejected")
                                sendMessageToWebSocketClient(it, jsonObject)
                            }
                        }

                        is PurchaseFailure.AuthenticationFailed -> {
                            nearpay.updateAuthentication(AuthenticationData.Jwt("JWT HERE"))
                            arrListClients.forEach {
                                val jsonObject = JsonObject()
                                jsonObject.addProperty("status_message", "failure")
                                println("here is purchase rejected ${jsonObject}")
                                sendMessageToWebSocketClient(it, jsonObject)
                            }
                        }

                        is PurchaseFailure.InvalidStatus -> {
                            println("here is 4444")
                            arrListClients.forEach {
                                val jsonObject = JsonObject()
                                jsonObject.addProperty("status_message", "failure")
                                sendMessageToWebSocketClient(it, jsonObject)
                            }
                        }

                        is PurchaseFailure.GeneralFailure -> {
                            arrListClients.forEach {
                                val jsonObject = JsonObject()
                                jsonObject.addProperty("status_message", "failure")
                                sendMessageToWebSocketClient(it, jsonObject)
                            }
                        }

                        is PurchaseFailure.UserCancelled -> {
                            arrListClients.forEach {
                                val jsonObject = JsonObject()
                                jsonObject.addProperty("status_message", "failure")
                                sendMessageToWebSocketClient(it, jsonObject)
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