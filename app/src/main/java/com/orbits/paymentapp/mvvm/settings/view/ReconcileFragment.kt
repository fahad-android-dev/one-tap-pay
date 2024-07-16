package com.orbits.paymentapp.mvvm.settings.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.FragmentReconcileBinding
import io.nearpay.sdk.Environments
import io.nearpay.sdk.NearPay
import io.nearpay.sdk.data.models.ReconciliationReceipt
import io.nearpay.sdk.utils.enums.AuthenticationData
import io.nearpay.sdk.utils.enums.NetworkConfiguration
import io.nearpay.sdk.utils.enums.ReconcileFailure
import io.nearpay.sdk.utils.enums.UIPosition
import io.nearpay.sdk.utils.listeners.ReconcileListener
import java.util.*

class ReconcileFragment : Fragment() {
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
        binding.textViewReceiptId.text = "Receipt ID: ${receipt.id}"
        binding.textViewTransactionCount.text = "Transaction Count: ${receipt.details.total.count}"
        binding.textViewDate.text = "Date: ${receipt.date}"
        binding.textViewTime.text = "Time: ${receipt.time}"
        binding.textViewMerchantName.text = "Merchant: ${receipt.merchant.name.english}"
        binding.textViewCurrency.text = "Currency: ${receipt.currency.english}"
    }

}
