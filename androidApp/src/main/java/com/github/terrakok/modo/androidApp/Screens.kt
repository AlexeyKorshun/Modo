package com.github.terrakok.modo.androidApp

import android.content.Intent
import android.net.Uri
import com.github.terrakok.modo.AppScreen
import com.github.terrakok.modo.ExternalScreen

object Screens {
    fun Sample(id: Int) = AppScreen(id.toString()) {
        SampleFragment.create(id)
    }

    fun Browser(url: String) = ExternalScreen {
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }
}