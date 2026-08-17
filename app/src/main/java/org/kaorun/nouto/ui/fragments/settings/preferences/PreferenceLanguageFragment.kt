package org.kaorun.nouto.ui.fragments.settings.preferences

import android.app.LocaleConfig
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.listitem.ListItemCardView
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class PreferenceLanguageFragment : PreferenceBaseFragment() {
    private var selectedTag = ""
    private val languagePrefs = mutableListOf<LanguagePreference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        selectedTag = getCurrentLanguageTag()

        getSupportedLanguages().forEach { (tag, name) ->
            val pref = LanguagePreference(tag).apply {
                key = "app_language_$tag"
                title = name
            }
            preferenceScreen.addPreference(pref)
            languagePrefs.add(pref)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageView = LayoutInflater.from(requireContext())
            .inflate(R.layout.illustration_language, listView, false)
        super.onViewCreated(view, savedInstanceState)
    }

    private fun selectLanguage(tag: String) {
        selectedTag = tag
        val localeList = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
        languagePrefs.forEach { it.refresh() }
    }

    private fun getCurrentLanguageTag(): String {
        return AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag().orEmpty()
    }

    private fun getSupportedLanguages(): List<AppLanguage> {
        val context = requireContext()
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val config = LocaleConfig(context)
            val supported = config.supportedLocales
            (0 until (supported?.size() ?: 0)).mapNotNull { supported?.get(it) }
        } else {
            parseLocaleConfigXml(context)
        }

        val systemDefault = AppLanguage("", getString(R.string.system_default))
        val parsedLanguages = locales.map { locale ->
            val name = locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
            AppLanguage(locale.toLanguageTag(), name)
        }.sortedBy { it.name }

        return listOf(systemDefault) + parsedLanguages
    }

    private fun parseLocaleConfigXml(context: Context): List<Locale> {
        val locales = mutableListOf<Locale>()
        runCatching {
            val parser = context.resources.getXml(R.xml.locale_config)
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { locales.add(Locale.forLanguageTag(it)) }
                }
            }
        }
        return locales
    }

    private inner class LanguagePreference(val tag: String) : Preference(requireContext()) {
        init {
            layoutResource = R.layout.item_preference_language
        }
        fun refresh() = notifyChanged()

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val card = holder.itemView.findViewById<ListItemCardView>(R.id.list_item_card)
            card.isChecked = tag == selectedTag
            card.setOnClickListener { selectLanguage(tag) }
        }
    }

    private data class AppLanguage(val tag: String, val name: String)
}