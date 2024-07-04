package com.orbits.paymentapp.mvvm.settings.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.FragmentSettingsBinding
import com.orbits.paymentapp.helper.AlertDialogInterface
import com.orbits.paymentapp.helper.BaseFragment
import com.orbits.paymentapp.helper.Constants
import com.orbits.paymentapp.helper.Dialogs
import com.orbits.paymentapp.helper.PrefUtils.getUserDataResponse
import com.orbits.paymentapp.helper.PrefUtils.setUserDataResponse
import com.orbits.paymentapp.helper.helper_model.UserDataModel
import com.orbits.paymentapp.helper.helper_model.UserResponseModel
import com.orbits.paymentapp.interfaces.CommonInterfaceClickEvent
import com.orbits.paymentapp.mvvm.main.view.MainActivity
import kotlin.random.Random

class SettingsFragment : BaseFragment() {
    private lateinit var mActivity: MainActivity
    private lateinit var binding: FragmentSettingsBinding

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
            R.layout.fragment_settings,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeToolbar()
        onClickListeners()

    }

    private fun initializeToolbar(){
        setUpToolbar(
            binding.layoutToolbar,
            title = "Settings",
            isBackArrow = true,
            navController = findNavController(),
            toolbarClickListener = object : CommonInterfaceClickEvent {
                override fun onToolBarListener(type: String) {
                    if (type == Constants.TOOLBAR_ICON_ONE){

                    }
                }
            }
        )
    }

    private fun onClickListeners(){
        binding.txtGenerateCode.setOnClickListener {
            Dialogs.showCodeDialog(
                activity = mActivity,
                code = activity?.getUserDataResponse()?.code ?: "",
                alertDialogInterface = object : AlertDialogInterface {
                    override fun onYesClick() {
                        Dialogs.showCustomAlert(
                            activity = mActivity,
                            msg = "Are you sure you want to generate new code?",
                            yesBtn = "Yes",
                            noBtn = "No",
                            alertDialogInterface = object : AlertDialogInterface{
                                override fun onYesClick() {
                                    mActivity.setUserDataResponse(
                                        UserResponseModel(
                                            code = mActivity.getUserDataResponse()?.code,
                                            data = UserDataModel(
                                                isCodeVerified = false
                                            )
                                        )
                                    )

                                    activity?.setUserDataResponse(
                                        UserResponseModel(
                                            code = generateRandomCode()
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    fun generateRandomCode(): String {
        val charPool : List<Char> = ('A'..'Z') + ('0'..'9')

        return (1..6)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

}