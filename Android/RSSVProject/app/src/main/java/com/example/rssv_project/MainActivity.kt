package com.example.rssv_project

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val uuid = "00001101-0000-1000-8000-00805F9B34FB"
    private var connectThread: ConnectThread? = null
    private var connectText by mutableStateOf("Start connection")
    private var blinkText by mutableStateOf("Blink")
    private val deviceAddress = "20:15:07:01:90:79" // Replace with your HC-06 MAC address - BT-03
    private var rgbOut = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        var red by remember { mutableStateOf("") }
        var green by remember { mutableStateOf("") }
        var blue by remember { mutableStateOf("") }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = red,
                onValueChange = { red = it},
                label = { Text("red:") },
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = green,
                onValueChange = { green = it},
                label = { Text("green:") },
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = blue,
                onValueChange = { blue = it},
                label = { Text("blue:") },
                modifier = Modifier.fillMaxWidth(0.8f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    // Start the ConnectThread
                    if (connectThread == null) {
                        connectThread = ConnectThread(deviceAddress, context, this@MainActivity)
                        connectThread?.start()
                    }
                }
            ) {
                Text(connectText)
            }

            Button(
                onClick = {
                    // Cancel the ConnectThread
                    if (connectThread != null) {
                        connectThread?.cancel()
                        connectThread = null
                    }
                }
            ) {
                Text("Close connection")
            }

            Button(
                onClick = {
                    val redInt = red.toIntOrNull()
                    val greenInt = green.toIntOrNull()
                    val blueInt = blue.toIntOrNull()
                    if ((redInt != null && (redInt < 0 || redInt > 255)) || redInt == null) {
                        Toast.makeText(applicationContext, "Red value must be a number between 0 and 255.", Toast.LENGTH_LONG).show()
                    } else if ((greenInt != null && (greenInt < 0 || greenInt > 255)) || greenInt == null) {
                        Toast.makeText(applicationContext, "Green value must be a number between 0 and 255.", Toast.LENGTH_LONG).show()
                    } else if ((blueInt != null && (blueInt < 0 || blueInt > 255)) || blueInt == null) {
                        Toast.makeText(applicationContext, "Blue value must be a number between 0 and 255.", Toast.LENGTH_LONG).show()
                    } else {
                        rgbOut = "$red,$green,$blue"
                        Log.d("--------My Bluetooth App", "------------BUTTON: $rgbOut")
                        // Send data
                        if (connectThread == null) {
                            connectThread = ConnectThread(deviceAddress, context, this@MainActivity)
                            connectThread?.start()
                        }
                        connectThread?.sendData(rgbOut)
                    }
                }
            ) {
                Text("Send data")
            }
            Button(onClick = {
                if (connectThread == null) {
                    connectThread = ConnectThread(deviceAddress, context, this@MainActivity)
                    connectThread?.start()
                }
                connectThread?.sendData("blink")
                blinkText = if(blinkText == "Blink") {
                    "Stop blinking"
                } else {
                    "Blink"
                }
            }) {
                Text(blinkText)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val deviceAddress: String, private val context: Context, private val activity: MainActivity) : Thread() {
        private val device: BluetoothDevice by lazy {
            adapter.getRemoteDevice(deviceAddress)
        }
        private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
        }

        override fun run() {
            if(adapter.isEnabled) {
                mmSocket?.let { socket ->
                    try {
                        // Connect to the remote device through the socket. This call blocks
                        // until it succeeds or throws an exception.
                        connectText = "Started"

                        activity.runOnUiThread {
                            Toast.makeText(context, "Connecting ...", Toast.LENGTH_SHORT).show()
                        }

                        socket.connect()
                        connectText = "Connected"

                    } catch (e: IOException) {
                        Log.e(TAG, "Error connecting to a device", e)
                        // Handle connection error
                        activity.runOnUiThread {
                            Toast.makeText(context, "Cannot connect to a device.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            } else {
                activity.runOnUiThread {
                    Toast.makeText(context, "Please enable Bluetooth.", Toast.LENGTH_SHORT)
                        .show()
                }
                connectText = "Started"
            }

        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            if(adapter.isEnabled) {
                try {
                    if(mmSocket!!.isConnected) {
                        mmSocket?.close()
                        activity.runOnUiThread {
                            Toast.makeText(context, "Disconnected.", Toast.LENGTH_SHORT)
                                .show()
                        }
                        connectText = "Start connection"
                    } else {
                        connectText = "Start connection"
                        activity.runOnUiThread {
                            Toast.makeText(context, "Not connected to a device.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Could not close the client socket", e)
                }
            } else {
                activity.runOnUiThread {
                    Toast.makeText(context, "Please enable Bluetooth.", Toast.LENGTH_SHORT)
                        .show()
                }
                connectText = "Start connection"
            }
        }

        // Function to send data to the connected device
        fun sendData(data: String) {
            // Check if ConnectThread is running and if there's an active socket
            if(adapter.isEnabled) {
                if (mmSocket != null && mmSocket!!.isConnected) {
                    try {
                        // Get the output stream from the socket
                        val outputStream = mmSocket?.outputStream

                        // Write the data to the output stream of the socket
                        outputStream?.write(data.toByteArray())
                    } catch (e: IOException) {
                        Log.e(TAG, "Error sending data", e)
                        // Handle data sending error
                    }
                } else {
                    activity.runOnUiThread {
                        Toast.makeText(context, "Not connected to a device.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                activity.runOnUiThread {
                    Toast.makeText(context, "Please enable Bluetooth.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
