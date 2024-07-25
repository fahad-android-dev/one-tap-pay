package com.orbits.paymentapp.mvvm.main.view

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.ActivityMainBinding
import com.orbits.paymentapp.helper.AppController
import com.orbits.paymentapp.helper.BaseActivity
import com.orbits.paymentapp.helper.Global
import com.orbits.paymentapp.helper.PrefUtils.setAppConfig
import com.orbits.paymentapp.helper.PrefUtils.setMasterKey
import com.orbits.paymentapp.helper.helper_model.AppConfigModel
import com.orbits.paymentapp.helper.helper_model.AppMasterKeyModel
import com.orbits.paymentapp.interfaces.MessageListener

class MainActivity : BaseActivity(){

    lateinit var binding: ActivityMainBinding
    private var isBackPressed: Long = 0
    private var currentMenuItemId: Int? = 0

    private val navController by lazy {
        Navigation.findNavController(this, R.id.nav_host_fragment)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initBottomTabs()
        initializeFields()
        initializeToolbar()
        onClickListeners()
    }



    private fun initBottomTabs() {
        onBottomNavigationItemClickListener()
    }

    override fun onNavigateUp(): Boolean {
        return navController.navigateUp(
            AppBarConfiguration(
                topLevelDestinationIds = setOf(
                    R.id.nav_graph_home,
                ), fallbackOnNavigateUpListener = ::onSupportNavigateUp
            )
        )
    }

    private fun onClickListeners() {

    }

    private fun initializeToolbar() {
        toolbarInit(getString(R.string.app_name))
    }

    private fun initializeFields() {
        onBackPressedDispatcher.addCallback(this@MainActivity, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                onBackPressedCallback()
            }
        })
    }




    fun onBackPressedCallback() {
        if (navController.currentDestination?.id == R.id.navigation_home) {
            if (isBackPressed + 2000 > System.currentTimeMillis()) {
                finish()
            } else {
                isBackPressed = System.currentTimeMillis()
            }
        } else {
            navController.popBackStack()
        }
    }

    private fun toolbarInit(title: String){

    }

    private fun onBottomNavigationItemClickListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentMenuItemId = destination.id
            when (destination.id) {
                R.id.navigation_home -> {
                    //    toolbarInit(getString(R.string.app_name))
                }

                else -> {
                    //  setUpToolbar(binding.layoutToolbar, title = getString(R.string.app_name), isBackArrow = false)
                }
            }
        }
    }
}