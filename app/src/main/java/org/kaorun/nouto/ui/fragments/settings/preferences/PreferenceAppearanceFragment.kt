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

        findPreference<SwitchPreferenceCompat>(BlackThemeHelper.KEY)
            ?.setOnPreferenceChangeListener { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        requireActivity().recreate()
                    }
                }, animationTime)
                true
            }

        findPreference<SwitchPreferenceCompat>(BlackThemeHelper.KEY)?.isVisible = isDarkMode

        findPreference<SwitchPreferenceCompat>(ColorThemeHelper.KEY_DYNAMIC)
            ?.setOnPreferenceChangeListener { _, _ ->
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        if (isAdded) {
                            requireActivity().recreate()
                        }
                    },
                    animationTime
                )
                true
            }

        findPreference<SwitchPreferenceCompat>("view_only_mode")
            ?.setOnPreferenceChangeListener {_, newValue ->
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putBoolean("is_view_only_mode", newValue as Boolean)
                }
                true
            }

        findPreference<SwitchPreferenceCompat>("show_keyboard")
            ?.setOnPreferenceChangeListener { _, newValue ->
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putBoolean("is_show_keyboard", newValue as Boolean)
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