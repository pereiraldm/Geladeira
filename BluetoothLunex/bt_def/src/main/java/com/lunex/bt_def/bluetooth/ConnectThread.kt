package com.lunex.bt_def.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*


class ConnectThread(device: BluetoothDevice, private val listener: BluetoothController.Listener) :
    Thread() {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var mSocket: BluetoothSocket? = null

    init {
        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()))
        } catch (_: IOException) {

        } catch (_: SecurityException) {

        }
    }

    override fun run() {
        try {
            mSocket?.connect()
            listener.onReceive(BluetoothController.BLUETOOTH_CONNECTED)
            readMessage()
        } catch (e: IOException) {
            listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
        } catch (_: SecurityException) {

        }
    }

    private fun readMessage() {
        val buffer = ByteArray(256)
        while (true) {
            try {
                val length = mSocket?.inputStream?.read(buffer)
                val message = String(buffer, 0, length ?: 0)
                listener.onReceive(message)
            } catch (e: IOException) {
                listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
                break
            }
        }
    }

    fun sendMessage(message: String) {
        try {
            mSocket?.outputStream?.write(message.toByteArray())
        } catch (_: IOException) {
        }
    }

    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (_: IOException) {

        }
    }

    fun mandarInt(sms: Int) {
        try {
            mSocket?.outputStream?.write(sms)
        } catch (_: IOException) {

        }

    }
}
