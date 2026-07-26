package com.example.agent.automation

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.net.Socket
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

@Singleton
class LadbConnection @Inject constructor() {
    enum class State { DISCONNECTED, PAIRING, CONNECTED, AUTHENTICATED, READY }
    private var currentState = State.DISCONNECTED
    private var connectionThread: Thread? = null
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    fun getState() = currentState

    fun connect() {
        if (currentState == State.CONNECTED || currentState == State.READY) return
        connectionThread = thread {
            try {
                socket = Socket("localhost", 5555)
                outputStream = socket?.getOutputStream()
                inputStream = socket?.getInputStream()
                currentState = State.CONNECTED
                startHeartbeat()
                // ADB AUTH logic would go here
                currentState = State.READY 
            } catch (e: Exception) {
                Log.e("LADB", "Connection failed: ${e.message}")
                currentState = State.DISCONNECTED
            }
        }
    }

    private fun startHeartbeat() {
        thread {
            while (currentState == State.READY || currentState == State.CONNECTED) {
                try {
                    Thread.sleep(10000)
                    // send heartbeat packet
                } catch (e: Exception) {
                    currentState = State.DISCONNECTED
                    break
                }
            }
        }
    }

    fun executeShellCommand(command: String): String {
        if (currentState != State.READY) return "Error: LADB not ready"
        try {
            return "Success: simulated execution of $command"
        } catch (e: Exception) {
            currentState = State.DISCONNECTED
            return "Error: ${e.message}"
        }
    }

    fun disconnect() {
        currentState = State.DISCONNECTED
        try {
            socket?.close()
        } catch (e: Exception) {}
    }
}

@Singleton
class LadbPairingManager @Inject constructor() {
    fun startPairing(pairingCode: String, port: Int): Boolean {
        Log.w("LADB", "Pairing not yet fully implemented via SPAKE2+")
        return false
    }
}

@Singleton
class LadbAutomationProvider @Inject constructor(
    private val connection: LadbConnection
) : AutomationProvider {
    override fun tap(x: Int, y: Int): Boolean {
        val res = connection.executeShellCommand("input tap $x $y")
        return res.startsWith("Success")
    }

    override fun type(text: String, nodeId: String?): Boolean {
        val escaped = text.replace(" ", "%s").replace("'", "\\'")
        val res = connection.executeShellCommand("input text '$escaped'")
        return res.startsWith("Success")
    }

    override fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        val res = connection.executeShellCommand("input swipe $startX $startY $endX $endY $duration")
        return res.startsWith("Success")
    }

    override fun scroll(direction: ScrollDirection): Boolean {
        return when (direction) {
            ScrollDirection.UP -> swipe(500, 800, 500, 200, 300)
            ScrollDirection.DOWN -> swipe(500, 200, 500, 800, 300)
            ScrollDirection.LEFT -> swipe(800, 500, 200, 500, 300)
            ScrollDirection.RIGHT -> swipe(200, 500, 800, 500, 300)
        }
    }

    override fun takeScreenshot(): Boolean {
        val res = connection.executeShellCommand("screencap -p /sdcard/screenshot.png")
        return res.startsWith("Success")
    }

    override fun getUiTree(): UiTreeSnapshot {
        val res = connection.executeShellCommand("dumpsys window windows | grep -E 'mCurrentFocus'")
        val pkg = if (res.contains("/")) res.substringAfter(" ").substringBefore("/") else "unknown"
        return UiTreeSnapshot(
            packageName = pkg.trim(),
            rootNode = NodeInfo(null, null, null, null, "", false, emptyList()),
            timestamp = System.currentTimeMillis()
        )
    }
}
