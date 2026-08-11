package org.kaorun.nouto.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.materialswitch.MaterialSwitch
import org.kaorun.nouto.R

class MaterialSwitchPreferenceCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchPreferenceCompat(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val widgetFrame = holder.itemView.findViewById<ViewGroup>(android.R.id.widget_frame) ?: return
        val switch = widgetFrame.getChildAt(0) as? MaterialSwitch ?: run {
            widgetFrame.removeAllViews()
            MaterialSwitch(widgetFrame.context).apply {
                isClickable = false
                isFocusable = false
                widgetFrame.addView(this)
            }
        }
        switch.isChecked = isChecked
        switch.isEnabled = isEnabled
        switch.thumbIconDrawable = ContextCompat.getDrawable(
            context,
            if (isChecked) R.drawable.check_24px else R.drawable.close_24px
        )
    }
}