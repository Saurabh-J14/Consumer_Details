package com.example.feeder.ui

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.feeder.R
import java.util.Locale

data class CharItem(
    val serviceUuid: String,
    val charUuid: String,
    val properties: Int
)

data class ServiceGroup(
    val serviceUuid: String,
    val chars: MutableList<CharItem>
)

class BleCharTreeAdapter(
    private val context: Context,
    private val groups: MutableList<ServiceGroup>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = groups.size
    override fun getChildrenCount(groupPosition: Int): Int = groups[groupPosition].chars.size
    override fun getGroup(groupPosition: Int): Any = groups[groupPosition]
    override fun getChild(groupPosition: Int, childPosition: Int): Any =
        groups[groupPosition].chars[childPosition]

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    override fun getChildId(groupPosition: Int, childPosition: Int): Long =
        (groupPosition * 10_000 + childPosition).toLong()

    override fun hasStableIds(): Boolean = false
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_ble_service_group, parent, false)
        val title = view.findViewById<TextView>(R.id.tvServiceTitle)
        val toggle = view.findViewById<ImageView>(R.id.ivServiceToggle)
        val service = groups[groupPosition].serviceUuid
        title.text = "Service: 0x${shortUuid(service)}"
        toggle.setImageResource(
            if (isExpanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_ble_char_child, parent, false)
        val title = view.findViewById<TextView>(R.id.tvCharTitle)
        val props = view.findViewById<TextView>(R.id.tvCharProps)
        val item = groups[groupPosition].chars[childPosition]

        title.text = "Char: 0x${shortUuid(item.charUuid)}"
        props.text = propertiesToList(item.properties).joinToString("  ")
        return view
    }

    private fun shortUuid(uuid: String): String {
        val hex = uuid.replace("-", "")
        val tail = if (hex.length >= 4) hex.takeLast(4) else hex
        return tail.uppercase(Locale.US)
    }

    private fun propertiesToList(props: Int): List<String> {
        val list = mutableListOf<String>()
        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0) list.add("READ")
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) list.add("WRITE")
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) list.add("WRITE_NR")
        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) list.add("NOTIFY")
        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) list.add("INDICATE")
        if (props and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) list.add("SIGNED")
        if (props and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) list.add("BROADCAST")
        if (list.isEmpty()) list.add("UNKNOWN")
        return list
    }
}
