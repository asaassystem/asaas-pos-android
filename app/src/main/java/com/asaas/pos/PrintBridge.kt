package com.asaas.pos

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

/**
 * JavaScript bridge for Bluetooth printing
 * Called from WebView JS as: AndroidPrint.printReceipt(jsonData)
 */
class PrintBridge(private val context: Context) {

    companion object {
        private const val TAG = "AsaasPOS_Print"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        fun successResult(): String = JSONObject().apply {
            put("success", true)
        }.toString()

        fun errorResult(msg: String?): String = JSONObject().apply {
            put("success", false)
            put("error", msg ?: "Unknown error")
        }.toString()
    }

    @JavascriptInterface
    fun printReceipt(jsonData: String): String {
        return try {
            val result = runBlocking {
                withContext(Dispatchers.IO) {
                    printViaBluetooth(jsonData)
                }
            }
            result
        } catch (e: Exception) {
            errorResult(e.message)
        }
    }

    @JavascriptInterface
    fun isBluetoothPrinterAvailable(): Boolean {
        return try {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun getPairedPrinters(): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return "[]"
            if (!adapter.isEnabled) return "[]"
            val sb = StringBuilder("[")
            adapter.bondedDevices.forEachIndexed { i, device ->
                if (i > 0) sb.append(",")
                val obj = JSONObject()
                obj.put("name", device.name)
                obj.put("address", device.address)
                sb.append(obj.toString())
            }
            sb.append("]")
            sb.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun printToDevice(deviceAddress: String, base64Data: String): String {
        return try {
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            runBlocking {
                withContext(Dispatchers.IO) {
                    sendBytes(deviceAddress, bytes)
                }
            }
        } catch (e: Exception) {
            errorResult(e.message)
        }
    }

    private fun printViaBluetooth(jsonData: String): String {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return errorResult("No Bluetooth adapter")
        if (!adapter.isEnabled) return errorResult("Bluetooth is disabled")

        val printer = adapter.bondedDevices.find { device ->
            device.name?.lowercase()?.let { name ->
                name.contains("printer") || name.contains("pos") ||
                name.contains("thermal") || name.contains("epson")
            } ?: false
        } ?: adapter.bondedDevices.firstOrNull()
            ?: return errorResult("No printer paired")

        return try {
            val json = JSONObject(jsonData)
            val escPosB64 = json.optString("escpos", "")
            val bytes = if (escPosB64.isNotEmpty()) {
                android.util.Base64.decode(escPosB64, android.util.Base64.DEFAULT)
            } else {
                (json.optString("text", "") + "\n\n\n").toByteArray()
            }
            sendBytes(printer.address, bytes)
        } catch (e: Exception) {
            errorResult(e.message)
        }
    }

    private fun sendBytes(addr: String, bytes: ByteArray): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(addr)
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket.outputStream.apply {
                write(bytes)
                flush()
            }
            Thread.sleep(500)
            socket.close()
            successResult()
        } catch (e: Exception) {
            Log.e(TAG, "BT send error: " + e.message)
            errorResult(e.message)
        }
    }
}
