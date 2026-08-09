package org.kaorun.nouto.ui.fragments.settings.preferences

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment
import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper
import org.kaorun.nouto.ui.utils.ThemeHelper

class PreferenceAppearanceFragment : PreferenceBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        val animationTime = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        findPreference<ListPreference>(ThemeHelper.KEY)
            ?.setOnPreferenceChangeListener { _, value ->
                ThemeHelper.apply(value)
                true
            }

        findPreference<SwitchPreferenceCompat>(BlackThemeHelper.KEY)?.apply {
            isVisible = isDarkMode
            setOnPreferenceChangeListener { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        requireActivity().recreate()
                    }
                }, animationTime)
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(ColorThemeHelper.KEY_DYNAMIC)
            ?.setOnPreferenceChangeListener { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        requireActivity().recreate()
                    }
                }, animationTime)
                true
            }

        findPreference<SwitchPreferenceCompat>("view_only_mode")
            ?.setOnPreferenceChangeListener { _, value ->
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putBoolean("is_view_only_mode", value as Boolean)
                }
                true
            }

        val focusOnContentPreference = findPreference<SwitchPreferenceCompat>("focus_on_content")
        val showKeyboardPreference = findPreference<SwitchPreferenceCompat>("show_keyboard")
        val isShowKeyboard = showKeyboardPreference?.isChecked ?: PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getBoolean("is_show_keyboard", true)

        focusOnContentPreference?.isVisible = isShowKeyboard

        showKeyboardPreference?.setOnPreferenceChangeListener { _, value ->
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putBoolean("is_show_keyboard", value as Boolean)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) {
                    focusOnContentPreference?.isVisible = value as Boolean
                }
            }, animationTime)
            true
        }

        focusOnContentPreference?.setOnPreferenceChangeListener { _, value ->
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putBoolean("is_focus_on_content", value as Boolean)
            }
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageView = LayoutInflater.from(requireContext())
            .inflate(R.layout.illustration_themes, listView, false)
        super.onViewCreated(view, savedInstanceState)
    }
}