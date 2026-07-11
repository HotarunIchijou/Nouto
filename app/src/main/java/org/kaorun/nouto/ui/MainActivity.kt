package org.kaorun.nouto.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.kaorun.nouto.BuildConfig
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.components.FreeDroidWarnDialog
import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper


class MainActivity : AppCompatActivity() {
    lateinit var roomBackup: RoomBackup
    private var warnDialog: android.app.Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorThemeHelper.apply(this)
        BlackThemeHelper.apply(this)
        roomBackup = RoomBackup(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        warnDialog = FreeDroidWarnDialog.show(this, BuildConfig.VERSION_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        warnDialog?.takeIf { it.isShowing }?.dismiss()
        warnDialog = null
    }
}