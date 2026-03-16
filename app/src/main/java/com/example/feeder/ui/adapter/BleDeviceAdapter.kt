package com.example.feeder.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.feeder.R

data class BleDeviceItem(
    val name: String,
    val address: String
)

class BleDeviceAdapter(
    private val context: Context,
    private val items: MutableList<BleDeviceItem>,
    private val onConnect: (String) -> Unit,
    private val onDisconnect: (String) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_ble_device_disconnect, parent, false)

        val label = view.findViewById<TextView>(R.id.tvDeviceLabel)
        val disconnect = view.findViewById<ImageView>(R.id.ivDisconnect)
        val item = items[position]

        label.text = "${item.name}\n${item.address}"
        view.setOnClickListener { onConnect(item.address) }
        disconnect.setOnClickListener { onDisconnect(item.address) }

        return view
    }
}
