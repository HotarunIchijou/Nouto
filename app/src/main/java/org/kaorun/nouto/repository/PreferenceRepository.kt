package org.kaorun.nouto.repository

import android.content.Context
import androidx.core.content.edit
import org.kaorun.nouto.ui.model.LayoutMode

class PreferenceRepository(context: Context) {
    private val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAYOUT_MODE = "key_layout_mode"
    }

    fun saveLayoutMode(mode: LayoutMode) {
        prefs.edit { putString(KEY_LAYOUT_MODE, mode.name) }
    }

    fun getLayoutMode(): LayoutMode = runCatching {
        LayoutMode.valueOf(
            prefs.getString(KEY_LAYOUT_MODE, LayoutMode.LINEAR.name)!!
        )
    }.getOrDefault(LayoutMode.LINEAR)
}