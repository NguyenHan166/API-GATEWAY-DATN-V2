package com.asoft.artsal.photo.ui.onboard.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemSelectLanguageBinding
import com.asoft.artsal.photo.data.model.Language
import com.asoft.artsal.photo.extensions.inflater

class SelectLanguageAdapter(
    private val onSelect: ((String) -> Unit)? = null,
) : ListAdapter<Language, SelectLanguageAdapter.LanguageViewHolder>(LanguageDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LanguageViewHolder(
        ItemSelectLanguageBinding.inflate(parent.inflater, parent, false)
    )

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    is LanguagePayload.SelectChanged -> holder.bindCheckState(payload.isSelect)
                }
            }
        }
    }

    inner class LanguageViewHolder(private val itemBinding: ItemSelectLanguageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(language: Language) {
            itemBinding.root.setOnClickListener {
                onSelect?.invoke(language.code)
            }

            itemBinding.run {
                ivLanguage.setImageResource(language.img)
                tvContent.text = language.name
                bindCheckState(language.isChecked)
            }
        }

        internal fun bindCheckState(checked: Boolean) {
            itemBinding.root.isSelected = checked
            itemBinding.tvContent.isSelected = checked
        }

    }

    private object LanguageDiffCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Language, newItem: Language): Any? {
            return when {
                oldItem.isChecked != newItem.isChecked -> LanguagePayload.SelectChanged(newItem.isChecked)
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }

}

sealed class LanguagePayload {
    data class SelectChanged(val isSelect: Boolean) : LanguagePayload()
}