package com.orbits.paymentapp.mvvm.settings.view

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.FragmentReconcileBinding
import com.orbits.paymentapp.helper.BaseFragment
import com.orbits.paymentapp.helper.Extensions.asString
import com.orbits.paymentapp.helper.Global.loadImagesUsingCoil
import com.orbits.paymentapp.interfaces.CommonInterfaceClickEvent
import io.nearpay.sdk.Environments
import io.nearpay.sdk.NearPay
import io.nearpay.sdk.data.models.ReconciliationReceipt
import io.nearpay.sdk.utils.enums.AuthenticationData
import io.nearpay.sdk.utils.enums.NetworkConfiguration
import io.nearpay.sdk.utils.enums.ReconcileFailure
import io.nearpay.sdk.utils.enums.UIPosition
import io.nearpay.sdk.utils.listeners.ReconcileListener
import java.util.*

class ReconcileFragment : BaseFragment() {
    private lateinit var binding: FragmentReconcileBinding
    private lateinit var nearpay: NearPay

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_reconcile, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeToolbar()
        initializeNearPay()


    }

    private fun initializeToolbar() {
        setUpToolbar(
            binding.layoutToolbar,
            title = "Settings",
            isBackArrow = true,
            navController = findNavController(),
            toolbarClickListener = object : CommonInterfaceClickEvent {
                override fun onToolBarListener(type: String) {
                    // Handle toolbar icon click if needed
                }
            }
        )
    }

    private fun initializeNearPay(){
        nearpay = NearPay.Builder()
            .context(requireContext())
            .authenticationData(AuthenticationData.Email("development@aflak.com.sa")) // Replace with actual authentication data
            .environment(Environments.SANDBOX) // Adjust environment as needed
            .locale(Locale.getDefault())
            .networkConfiguration(NetworkConfiguration.DEFAULT)
            .uiPosition(UIPosition.CENTER_BOTTOM)
            .build()

        reconcileTransactions()
    }

    private fun reconcileTransactions() {
        val reconcileId = UUID.randomUUID()
        val enableReceiptUi = true
        val finishTimeOut: Long = 10
        val enableUiDismiss = true
        val adminPin = "0000"

        nearpay.reconcile(reconcileId, enableReceiptUi, adminPin, finishTimeOut, enableUiDismiss, object : ReconcileListener {
            override fun onReconcileFinished(receipt: ReconciliationReceipt?) {
                receipt?.let {
                    // Update UI with receipt details
                    displayReconciliationDetails(it)
                }
            }

            override fun onReconcileFailed(reconcileFailure: ReconcileFailure) {
                // Handle reconcile failure scenarios
                // Example: Log error message
                println("Reconcile failed: $reconcileFailure")
            }
        })
    }

    private fun displayReconciliationDetails(receipt: ReconciliationReceipt) {

        val qrCodeBitmap = generateQRCode(receipt.qr_code)
        binding.ivCode.setImageBitmap(qrCodeBitmap)
        binding.txtCompleted.text = "Reconcile Completed"
        binding.txtId.text = receipt.id.substringAfterLast("-")
        binding.txtDateTime.text = "Reconciled On ${receipt.date} ${receipt.time}"

        // Purchase
        binding.txtPurchaseCount.text = receipt.details.purchase.count.asString()
        binding.txtPurchaseTotal.text = "${receipt.details.purchase.total} ${receipt.currency.english}"

        // Refund
        binding.txtRefundCount.text = receipt.details.refund.count.asString()
        binding.txtRefundTotal.text = "${receipt.details.refund.total} ${receipt.currency.english}"

        // Purchase Reversal
        binding.txtPurchaseReversalCount.text = receipt.details.purchase_reversal.count.asString()
        binding.txtPurchaseReversalTotal.text = "${receipt.details.purchase_reversal.total} ${receipt.currency.english}"

        // Refund Reversal
        binding.txtRefundReversalCount.text = receipt.details.refund_reversal.count.asString()
        binding.txtRefundReversalTotal.text = "${receipt.details.refund_reversal.total} ${receipt.currency.english}"

        // Total
        binding.txtTotalTypeCount.text = receipt.details.total.count.asString()
        binding.txtTotalTypeTotal.text ="${receipt.details.total.total} ${receipt.currency.english}"
    }

    fun generateQRCode(url: String, width: Int = 512, height: Int = 512): Bitmap {
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 0 // Adjust margin as needed
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix: BitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hints)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

}
