package com.orbits.paymentapp.mvvm.settings.view

import AppNavigation.navigateToMain
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.FragmentSettingsBinding
import com.orbits.paymentapp.helper.AlertDialogInterface
import com.orbits.paymentapp.helper.BaseFragment
import com.orbits.paymentapp.helper.Dialogs
import com.orbits.paymentapp.helper.Dialogs.showCustomAlert
import com.orbits.paymentapp.helper.LocaleHelper
import com.orbits.paymentapp.helper.PrefUtils.getMasterKey
import com.orbits.paymentapp.helper.PrefUtils.getUserDataResponse
import com.orbits.paymentapp.helper.PrefUtils.setAppPassword
import com.orbits.paymentapp.helper.PrefUtils.setUserDataResponse
import com.orbits.paymentapp.helper.helper_model.PasswordModel
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
            setupClickListeners()
    }

    private fun initializeToolbar() {
        setUpToolbar(
            binding.layoutToolbar,
            title = getString(R.string.settings),
            isBackArrow = true,
            navController = findNavController(),
            toolbarClickListener = object : CommonInterfaceClickEvent {
                override fun onToolBarListener(type: String) {
                    // Handle toolbar icon click if needed
                }
            }
        )
    }

    private fun setupClickListeners() {
        binding.txtGenerateCode.setOnClickListener {
            showGenerateCodeDialog()
        }

        binding.txtReconcile.setOnClickListener {
            navigateToReconcileFragment()
        }

        binding.txtChangePassword.setOnClickListener {
            Dialogs.showChangeAllPasswordDialog(
                activity = mActivity,
                alertDialogInterface = object : AlertDialogInterface{
                    override fun onSubmitPasswordClick(password: String) {
                        println("here is 222 ${mActivity.getMasterKey()?.masterKey}")
                        mActivity.setAppPassword(
                            result = PasswordModel(
                                appPassword = password
                            )
                        )
                    }
                }
            )
        }
        binding.txtChangeLanguage.setOnClickListener {
            showChangeLanguageAlert()
        }
    }

    private fun showChangeLanguageAlert() {
        showCustomAlert(
            activity = mActivity,
            title = getString(R.string.alert_title_lang),
            msg = resources.getString(R.string.alert_language),
            yesBtn = resources.getString(R.string.yes_lang),
            noBtn = resources.getString(R.string.no_lang),
            alertDialogInterface = object : AlertDialogInterface {
                override fun onYesClick() {
                    LocaleHelper.changeLanguage(mActivity)
                    mActivity.navigateToMain{}
                }

                override fun onNoClick() {}
            })
    }

    private fun showGenerateCodeDialog() {
        Dialogs.showCodeDialog(
            activity = mActivity,
            code = activity?.getUserDataResponse()?.code ?: "",
            alertDialogInterface = object : AlertDialogInterface {
                override fun onYesClick() {
                    Dialogs.showCustomAlert(
                        activity = mActivity,
                        msg = getString(R.string.are_you_sure_you_want_to_generate_a_new_code),
                        yesBtn = getString(R.string.yes),
                        noBtn = getString(R.string.label_no),
                        alertDialogInterface = object : AlertDialogInterface {
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

    private fun navigateToReconcileFragment() {
        findNavController().navigate(R.id.action_settingsFragment_to_reconcileFragment)
    }

    private fun generateRandomCode(): String {
        val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

        return (1..6)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
