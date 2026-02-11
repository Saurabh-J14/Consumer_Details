package com.example.feeder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.feeder.data.model.PendingConsumerResponse
import com.example.feeder.databinding.ItemConsumerCountBinding

class PendingConsumerAdapter(
    private val onItemClick: (PendingConsumerResponse.ResData.Data) -> Unit
) : ListAdapter<PendingConsumerResponse.ResData.Data, PendingConsumerAdapter.PendingViewHolder>(
    DiffCallback()
) {

    inner class PendingViewHolder(
        private val binding: ItemConsumerCountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PendingConsumerResponse.ResData.Data) {

            binding.etconsumerno.text = item.consumerNumber
            binding.etMeterNo.text = item.meterNumber
            binding.etphase.text = item.phaseDesignation?.toString() ?: ""

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PendingViewHolder {

        val binding = ItemConsumerCountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback :
        DiffUtil.ItemCallback<PendingConsumerResponse.ResData.Data>() {

        override fun areItemsTheSame(
            oldItem: PendingConsumerResponse.ResData.Data,
            newItem: PendingConsumerResponse.ResData.Data
        ): Boolean {
            return oldItem.consumerNumber == newItem.consumerNumber
        }

        override fun areContentsTheSame(
            oldItem: PendingConsumerResponse.ResData.Data,
            newItem: PendingConsumerResponse.ResData.Data
        ): Boolean {
            return oldItem == newItem
        }
    }
}
