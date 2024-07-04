package com.orbits.paymentapp.helper

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.LayoutGenerateCodeDialogBinding
import com.orbits.paymentapp.databinding.LayoutSettingsPasswordDialogBinding
import com.orbits.paymentapp.helper.Global.getDimension

object Dialogs {

    var customDialog: Dialog? = null
    var codeDialog: Dialog? = null

    fun showPasswordDialog(
        activity: Context,
        isCancellable: Boolean? = true,
        alertDialogInterface: AlertDialogInterface,
    ) {
        try {
            customDialog = Dialog(activity)
            customDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            customDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val binding: LayoutSettingsPasswordDialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(activity),
                R.layout.layout_settings_password_dialog, null, false
            )
            customDialog?.setContentView(binding.root)
            val lp: WindowManager.LayoutParams = WindowManager.LayoutParams()
            lp.copyFrom(customDialog?.window?.attributes)
            lp.width = getDimension(activity as Activity, 300.00)
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.CENTER
            customDialog?.window?.attributes = lp
            customDialog?.setCanceledOnTouchOutside(isCancellable ?: true)
            customDialog?.setCancelable(isCancellable ?: true)

            binding.btnAlertPositive.text = "Confirm"

            binding.ivPasswordEye.setOnClickListener {
                if (binding.edtPassword.transformationMethod == null) {
                    binding.edtPassword.transformationMethod = PasswordTransformationMethod()
                    binding.edtPassword.setSelection(binding.edtPassword.text?.length ?: 0)
                    binding.ivPasswordEye.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_eye))
                } else {
                    binding.edtPassword.transformationMethod = null
                    binding.edtPassword.setSelection(binding.edtPassword.text?.length ?: 0)
                    binding.ivPasswordEye.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_eye_closed))
                }
            }

            binding.btnAlertPositive.setOnClickListener {
                if (binding.edtPassword.text.isEmpty()){
                    Toast.makeText(activity,"Please enter pin", Toast.LENGTH_SHORT).show()
                }else if (binding.edtPassword.text.length < 4){
                    Toast.makeText(activity,"Pin length should be at least 4 digits", Toast.LENGTH_SHORT).show()
                }else if (binding.edtPassword.text.toString() != "1234"){
                    Toast.makeText(activity,"Invalid Pin", Toast.LENGTH_SHORT).show()
                }
                else{
                    customDialog?.dismiss()
                    alertDialogInterface.onYesClick()
                }
            }
            customDialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showCodeDialog(
        activity: Context,
        isCancellable: Boolean? = true,
        savedCode:String,
        alertDialogInterface: AlertDialogInterface,
    ) {
        try {
            codeDialog = Dialog(activity)
            codeDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            codeDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val binding: LayoutGenerateCodeDialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(activity),
                R.layout.layout_generate_code_dialog, null, false
            )
            codeDialog?.setContentView(binding.root)
            val lp: WindowManager.LayoutParams = WindowManager.LayoutParams()
            lp.copyFrom(codeDialog?.window?.attributes)
            lp.width = getDimension(activity as Activity, 300.00)
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.CENTER
            codeDialog?.window?.attributes = lp
            codeDialog?.setCanceledOnTouchOutside(isCancellable ?: true)
            codeDialog?.setCancelable(isCancellable ?: true)


            binding.otpView.setOTP(savedCode)

            binding.btnAlertPositive.setOnClickListener {
                binding.otpView.setOTP(generateRandomCode())
            }
            codeDialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateRandomCode(): String {
        val random = java.util.Random()
        val randomCode = random.nextInt(10000)
        return String.format("%04d", randomCode)
    }
}
