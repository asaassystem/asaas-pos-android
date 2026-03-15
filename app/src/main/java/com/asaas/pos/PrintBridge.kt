package com.asaas.pos

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.UUID

class PrintBridge(private val context: Context) {

    companion object {
        private const val TAG = "AsaasPOS_Print"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @JavascriptInterface
    fun printReceipt(jsonData: String): String {
        return try {
            val result = runBlocking { withContext(Dispatchers.IO) { printViaBluetooth(jsonData) } }
            result
        } catch (e: Exception) {
            "{"success":false,"error":"${e.message}"}"
        }
    }

    @JavascriptInterface
    fun isBluetoothPrinterAvailable(): Boolean {
        return try {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (e: Exception) { false }
    }

    @JavascriptInterface
    fun getPairedPrinters(): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return "[]"
            if (!adapter.isEnabled) return "[]"
            val sb = StringBuilder("[")
            adapter.bondedDevices.forEachIndexed { i, d ->
                if (i > 0) sb.append(",")
                sb.append("{"name":"${d.name}","address":"${d.address}"}")
            }
            sb.append("]")
            sb.toString()
        } catch (e: Exception) { "[]" }
    }

    @JavascriptInterface
    fun printToDevice(deviceAddress: String, base64Data: String): String {
        return try {
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            runBlocking { withContext(Dispatchers.IO) { sendBytes(deviceAddress, bytes) } }
        } catch (e: Exception) {
            "{"success":false,"error":"${e.message}"}"
        }
    }

    private fun printViaBluetooth(jsonData: String): String {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return "{"success":false,"error":"No Bluetooth"}"
        if (!adapter.isEnabled) return "{"success":false,"error":"Bluetooth off"}"
        
        val printer = adapter.bondedDevices.find { d ->
            d.name?.lowercase()?.let { n ->
                n.contains("printer") || n.contains("pos") || n.contains("thermal") || n.contains("epson")
            } ?: false
        } ?: adapter.bondedDevices.firstOrNull() ?: return "{"success":false,"error":"No printer paired"}"
        
        return try {
            val json = org.json.JSONObject(jsonData)
            val escPosB64 = json.optString("escpos", "")
            val bytes = if (escPosB64.isNotEmpty()) {
                android.util.Base64.decode(escPosB64, android.util.Base64.DEFAULT)
            } else {
                (json.optString("text", "") + "\n\n\n").toByteArray()
            }
            sendBytes(printer.address, bytes)
        } catch (e: Exception) {
            "{"success":false,"error":"${e.message}"}"
        }
    }

    private fun sendBytes(addr: String, bytes: ByteArray): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(addr)
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket.outputStream.apply { write(bytes); flush() }
            Thread.sleep(500)
            socket.close()
            "{"success":true}"
        } catch (e: Exception) {
            Log.e(TAG, "BT error: ${e.message}")
            "{"success":false,"error":"${e.message}"}"
        }
    }
}
