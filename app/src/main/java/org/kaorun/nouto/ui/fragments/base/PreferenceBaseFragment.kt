package org.kaorun.nouto.ui.fragments.base

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemLayout
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.model.LayoutMode
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.MarginItemDecoration

abstract class PreferenceBaseFragment : PreferenceFragmentCompat() {
    var textView: View? = null
    var imageView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = listView.adapter ?: return
        listView.removeItemDecorationAt(0)
        listView.addItemDecoration(MarginItemDecoration(
            resources.getDimensionPixelSize(R.dimen.recycler_view_outer_margin),
            resources.getDimensionPixelSize(R.dimen.recycler_view_segmented_list_margin),
            LayoutMode.LINEAR.spanCount,
            false
        ))
        val segmentedPreferenceAdapter = SegmentedPreferenceAdapter(adapter)

        val adapters = listOfNotNull(
            textView?.let { SingleViewAdapter(it) },
            imageView?.let { SingleViewAdapter(it) },
            segmentedPreferenceAdapter
        )
        listView.adapter = if (adapters.size == 1) adapters[0] else ConcatAdapter(adapters)

        InsetsHandler.applyViewInsets(listView, false)
    }

    protected fun openUrl(url: Int) {
        val intent = Intent(Intent.ACTION_VIEW, resources.getString(url).toUri())
        startActivity(intent)
    }

    inner class SegmentedPreferenceAdapter(
        private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        init {
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onChanged() = notifyDataSetChanged()
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) =
                    notifyItemRangeChanged(positionStart, itemCount)
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) =
                    notifyItemRangeChanged(positionStart, itemCount, payload)
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) =
                    notifyItemRangeInserted(positionStart, itemCount)
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) =
                    notifyItemRangeRemoved(positionStart, itemCount)
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
                    notifyItemMoved(fromPosition, toPosition)
            })
        }

        override fun getItemCount() = adapter.itemCount
        override fun getItemViewType(position: Int) = adapter.getItemViewType(position)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            adapter.createViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            adapter.bindViewHolder(holder, position)
            val view = holder.itemView as? ListItemLayout ?: return

            val (start, end) = segmentBounds(position)
            view.updateAppearance(position - start, end - start + 1)
        }

        private val viewTypeIsPreferenceCache = mutableMapOf<Int, Boolean>()

        private fun isPreference(position: Int): Boolean {
            val viewType = adapter.getItemViewType(position)
            return viewTypeIsPreferenceCache.getOrPut(viewType) {
                adapter.createViewHolder(listView, viewType).itemView is ListItemLayout
            }
        }

        private fun segmentBounds(position: Int): Pair<Int, Int> {
            var start = position
            while (start > 0 && isPreference(start - 1)) start--

            var end = position
            val lastIndex = adapter.itemCount - 1
            while (end < lastIndex && isPreference(end + 1)) end++

            return start to end
        }
    }

    class SingleViewAdapter(
        private val view: View
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(view) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }
}