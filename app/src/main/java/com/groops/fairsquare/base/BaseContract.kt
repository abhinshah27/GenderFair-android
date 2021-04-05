package com.groops.fairsquare.base

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView

interface BaseContract {

    interface BasePresenter {
        fun init()
        fun callServer()
        fun getRVAdapter(): RecyclerView.Adapter<*>?
        @StringRes fun getNoResultString(): Int
        fun handleServerReceiver(register: Boolean)
        fun onSaveInstanceState(outState: Bundle)
        fun onRestoreInstanceState(savedInstanceState: Bundle?)
    }

    interface BaseView {
        fun init()
        fun handleSRL(show: Boolean)
        fun handleRVVisibility(show: Boolean)
    }

}