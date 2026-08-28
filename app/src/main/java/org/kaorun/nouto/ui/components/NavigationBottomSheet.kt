package org.kaorun.nouto.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.kaorun.nouto.databinding.BottomSheetNavigationBinding

class NavigationBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetNavigationBinding? = null
    private val binding get() = _binding!!

    var onTrashClick: (() -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        binding.navTrash.setOnClickListener {
            dismiss()
            onTrashClick?.invoke()
        }

        binding.navSettings.setOnClickListener {
            dismiss()
            onSettingsClick?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
