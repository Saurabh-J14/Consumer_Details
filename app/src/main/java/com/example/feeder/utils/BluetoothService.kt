package com.example.feeder.utils

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private var socket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null

    companion object {
        val MY_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun connect(device: BluetoothDevice, onConnected: () -> Unit, onError: (String) -> Unit) {

        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                socket?.connect()

                connectedThread = ConnectedThread(socket!!)
                connectedThread?.start()

                Handler(Looper.getMainLooper()).post {
                    onConnected()
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onError(e.message ?: "Connection Failed")
                }
            }
        }.start()
    }

    fun sendMessage(message: String) {
        connectedThread?.write(message.toByteArray())
    }

    fun setOnMessageReceived(listener: (String) -> Unit) {
        connectedThread?.onMessageReceived = listener
    }

    fun close() {
        socket?.close()
    }

    private class ConnectedThread(private val socket: BluetoothSocket) : Thread() {

        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream

        var onMessageReceived: ((String) -> Unit)? = null

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val received = String(buffer, 0, bytes)

                    Handler(Looper.getMainLooper()).post {
                        onMessageReceived?.invoke(received)
                    }

                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            outputStream.write(bytes)
        }
    }
}