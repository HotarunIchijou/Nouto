package org.kaorun.nouto.ui.fragments.settings.preferences

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.transition.TransitionManager
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment

class PreferenceMainFragment : PreferenceBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)
        val parentView by lazy {
            requireParentFragment().requireView().parent as ViewGroup
        }

        findPreference<Preference>("screen_appearance")?.setOnPreferenceClickListener {
            TransitionManager.endTransitions(parentView)
            requireParentFragment().findNavController()
                .navigate(R.id.action_settingsMainFragment_to_settingsAppearanceFragment)
            true
        }

        findPreference<Preference>("screen_backup_restore")?.setOnPreferenceClickListener {
            requireParentFragment().findNavController()
                .navigate(R.id.action_settingsMainFragment_to_settingsBackupRestoreFragment)
            true
        }

        findPreference<Preference>("screen_language")?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            true
        }

        findPreference<Preference>("screen_about")?.setOnPreferenceClickListener {
            TransitionManager.endTransitions(parentView)
            requireParentFragment().findNavController()
                .navigate(R.id.action_settingsMainFragment_to_settingsAboutFragment)
            true
        }
    }
}