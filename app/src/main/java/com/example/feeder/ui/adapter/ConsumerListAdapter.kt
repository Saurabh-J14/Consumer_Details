package com.example.feeder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.feeder.databinding.ItemDtnameBinding

class ConsumerListAdapter(
    private val list: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ConsumerListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDtnameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(consumerNo: String) {

            binding.etconsumerNumber.text = "Consumer No : $consumerNo"

            binding.root.setOnClickListener {
                onItemClick(consumerNo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDtnameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}