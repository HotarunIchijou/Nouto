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
import org.kaorun.nouto.ui.utils.ThemeHelper
import org.kaorun.nouto.data.PreferenceAppearanceKeys

class PreferenceAppearanceFragment : PreferenceBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        val animationTime = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        findPreference<ListPreference>(PreferenceAppearanceKeys.WIDGET_THEME)
            ?.setOnPreferenceChangeListener { _, value ->
                ThemeHelper.apply(value)
                true
            }

        findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_BLACK_THEME)?.apply {
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

        findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_DYNAMIC_COLORS)
            ?.setOnPreferenceChangeListener { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        requireActivity().recreate()
                    }
                }, animationTime)
                true
            }

        findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_VIEW_ONLY_MODE)
            ?.setOnPreferenceChangeListener { _, value ->
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putBoolean(PreferenceAppearanceKeys.IS_VIEW_ONLY_MODE, value as Boolean)
                }
                true
            }

        val focusOnContentPreference =
            findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_FOCUS_ON_CONTENT)
        val showKeyboardPreference =
            findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_SHOW_KEYBOARD)
        val showTitlePreference =
            findPreference<SwitchPreferenceCompat>(PreferenceAppearanceKeys.WIDGET_SHOW_TITLE)

        focusOnContentPreference?.isEnabled =
            (showTitlePreference?.isChecked == true) && (showKeyboardPreference?.isChecked == true)

        showTitlePreference?.setOnPreferenceChangeListener { _, value ->
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putBoolean(PreferenceAppearanceKeys.IS_SHOW_TITLE, value as Boolean)
            }
            focusOnContentPreference?.isEnabled = value as Boolean &&
                    showKeyboardPreference?.isChecked == true
            true
        }

        showKeyboardPreference?.setOnPreferenceChangeListener { _, value ->
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putBoolean(PreferenceAppearanceKeys.IS_SHOW_KEYBOARD, value as Boolean)
            }
            focusOnContentPreference?.isEnabled = value as Boolean &&
                    showTitlePreference?.isChecked == true
            true
        }

        focusOnContentPreference?.setOnPreferenceChangeListener { _, value ->
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putBoolean(PreferenceAppearanceKeys.IS_FOCUS_ON_CONTENT, value as Boolean)
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