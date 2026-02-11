package com.example.feeder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.feeder.data.model.TotalConsumerResponse
import com.example.feeder.databinding.ItemConsumerCountBinding

class TotalConsumerAdapter(
    private val list: List<TotalConsumerResponse.ResData.Data>,
    private val onItemClick: (TotalConsumerResponse.ResData.Data) -> Unit
) : RecyclerView.Adapter<TotalConsumerAdapter.ConsumerVH>() {

    inner class ConsumerVH(
        val binding: ItemConsumerCountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(list[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsumerVH {
        val binding = ItemConsumerCountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConsumerVH(binding)
    }

    override fun onBindViewHolder(holder: ConsumerVH, position: Int) {
        val item = list[position]

        holder.binding.etconsumerno.text = item.consumerNumber
        holder.binding.etMeterNo.text = item.meterNumber
        holder.binding.etphase.text = item.phaseDesignation.toString()
    }

    override fun getItemCount(): Int = list.size
}
