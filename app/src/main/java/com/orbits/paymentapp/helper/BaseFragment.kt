package com.orbits.paymentapp.helper

import android.app.Dialog
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.orbits.paymentapp.mvvm.main.view.MainActivity

open class BaseFragment : Fragment() {
    private var progressDialog: Dialog? = null
    var onRequestPermissionsResult: OnRequestPermissionsResult? = null
    var hasInitializedRootView = false
    private lateinit var mActivity: MainActivity

    private var rootView: View? = null

    fun getPersistentView(
        view: View?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            // Inflate the layout for this fragment
            hasInitializedRootView = true
            rootView = view
        } else {
            // Do not inflate the layout again.
            // The returned View of onCreateView will be added into the fragment.
            // However it is not allowed to be added twice even if the parent is same.
            // So we must remove rootView from the existing parent view group
            // (it will be added back).
            if (rootView?.parent != null) {
                (rootView?.parent as? ViewGroup)?.removeView(rootView)
            }
        }
        return rootView
    }


    override fun onStop() {
        super.onStop()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }


}
