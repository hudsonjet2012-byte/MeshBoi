package com.robco.pipbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class ExplorationPolicy(val label: String, val code: String) {
    SMART_GREEDY("VAULT-TEC SMART GREEDY (UNVISITED FIRST)", "POL-01"),
    DFS_EXPLORER("ROBCO DEEP SCAN (DFS STATE GRAPH)", "POL-02"),
    CHAOS_MONKEY("WASTELAND CHAOS MONKEY (RANDOMIZED)", "POL-03")
}

data class UtgNode(
    val stateHash: String,
    val packageName: String,
    val interactiveCount: Int,
    val visitCount: Int
)

data class BotTelemetry(
    val isServiceConnected: Boolean = false,
    val isRunning: Boolean = false,
    val targetPackage: String = "com.android.settings",
    val policy: ExplorationPolicy = ExplorationPolicy.SMART_GREEDY,
    val tickIntervalMs: Long = 1500L,
    val totalActions: Int = 0,
    val uniqueStatesDiscovered: Int = 0,
    val currentStateHash: String = "00000000",
    val loopWarnings: Int = 0,
    val utgNodes: List<UtgNode> = emptyList(),
    val logs: List<String> = listOf(
        "ROBCO INDUSTRIES UNIFIED OPERATING SYSTEM",
        "COPYRIGHT 2075-2077 ROBCO INDUSTRIES",
        "-PIP-BOT 3000 UI TELEMETRY UNIT INITIALIZED-"
    )
)

class PipBotAccessibilityService : AccessibilityService() {

    companion object {
        private val _telemetry = MutableStateFlow(BotTelemetry())
        val telemetry: StateFlow<BotTelemetry> = _telemetry

        var instance: PipBotAccessibilityService? = null
            private set

        fun logEvent(message: String) {
            val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val current = _telemetry.value.logs
            _telemetry.value = _telemetry.value.copy(
                logs = (listOf("[$stamp] $message") + current).take(80)
            )
        }

        fun updateConfig(
            targetPkg: String = _telemetry.value.targetPackage,
            policy: ExplorationPolicy = _telemetry.value.policy,
            intervalMs: Long = _telemetry.value.tickIntervalMs
        ) {
            _telemetry.value = _telemetry.value.copy(
                targetPackage = targetPkg,
                policy = policy,
                tickIntervalMs = intervalMs
            )
            logEvent("DIRECTIVE UPDATED: PKG=$targetPkg | POL=${policy.code}")
        }

        fun toggleBot(run: Boolean) {
            _telemetry.value = _telemetry.value.copy(isRunning = run)
            logEvent(if (run) ">>> ENGAGING AUTOMATED UI EXPLORATION <<<" else ">>> HALTING PIP-BOT ACTUATORS <<<")
            instance?.scheduleNextTick()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val visitedWidgets = mutableSetOf<String>()
    private val stateVisitCounts = mutableMapOf<String, Int>()
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 45)

    private val botLoopRunnable = object : Runnable {
        override fun run() {
            val state = _telemetry.value
            if (!state.isRunning || !state.isServiceConnected) return

            executeExplorationStep()
            handler.postDelayed(this, _telemetry.value.tickIntervalMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _telemetry.value = _telemetry.value.copy(isServiceConnected = true)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        logEvent("ACCESSIBILITY LINK ESTABLISHED. READY FOR DIRECTIVES.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Real-time passive telemetry updates when windows change
    }

    override fun onInterrupt() {
        logEvent("WARNING: ACCESSIBILITY STREAM INTERRUPTED.")
    }

    override fun onDestroy() {
        instance = null
        _telemetry.value = _telemetry.value.copy(isServiceConnected = false, isRunning = false)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun scheduleNextTick() {
        handler.removeCallbacks(botLoopRunnable)
        if (_telemetry.value.isRunning) {
            handler.postDelayed(botLoopRunnable, 600L)
        }
    }

    private fun executeExplorationStep() {
        val root = rootInActiveWindow ?: run {
            logEvent("WARN: NO ACTIVE WINDOW ROOT DETECTED.")
            return
        }

        val activePkg = root.packageName?.toString() ?: "unknown"
        val actionableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectActionableNodes(root, actionableNodes)

        // Compute structural SHA-256 UI State Signature (Upgraded DroidBot UTG Hash)
        val stateHash = computeUiStateHash(activePkg, actionableNodes)
        val visits = (stateVisitCounts[stateHash] ?: 0) + 1
        stateVisitCounts[stateHash] = visits

        updateUtgTelemetry(stateHash, activePkg, actionableNodes.size, visits)

        // Loop Detection & Recovery
        if (visits > 4) {
            toneGen.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 80)
            logEvent("LOOP DETECTED AT STATE [$stateHash]. EXECUTING GLOBAL BACK.")
            performGlobalAction(GLOBAL_ACTION_BACK)
            _telemetry.value = _telemetry.value.copy(
                loopWarnings = _telemetry.value.loopWarnings + 1,
                totalActions = _telemetry.value.totalActions + 1
            )
            return
        }

        if (actionableNodes.isEmpty()) {
            logEvent("STATE [$stateHash] HAS 0 TARGETS. NAVIGATING BACK.")
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        // Select Target Node Based on RobCo Exploration Policy
        val chosenNode = when (_telemetry.value.policy) {
            ExplorationPolicy.SMART_GREEDY -> {
                actionableNodes.firstOrNull { node ->
                    !visitedWidgets.contains(getNodeSignature(stateHash, node))
                } ?: actionableNodes.random()
            }
            ExplorationPolicy.DFS_EXPLORER -> {
                actionableNodes.lastOrNull { node ->
                    !visitedWidgets.contains(getNodeSignature(stateHash, node))
                } ?: actionableNodes.first()
            }
            ExplorationPolicy.CHAOS_MONKEY -> actionableNodes.random()
        }

        val widgetSig = getNodeSignature(stateHash, chosenNode)
        visitedWidgets.add(widgetSig)

        dispatchNodeAction(chosenNode, stateHash)
    }

    private fun dispatchNodeAction(node: AccessibilityNodeInfo, stateHash: String) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val label = (node.text ?: node.contentDescription ?: node.ViewIdResourceName() ?: node.className ?: "NODE")
            .toString().take(18)

        val success = when {
            node.isClickable -> {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked && bounds.width() > 0 && bounds.height() > 0) {
                    dispatchTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                } else clicked
            }
            node.isScrollable -> {
                node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            else -> {
                dispatchTapGesture(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            }
        }

        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 35)
        logEvent("ACT [$stateHash] -> $label @ (${bounds.centerX()},${bounds.centerY()}) [${if (success) "OK" else "GESTURE"}]")

        _telemetry.value = _telemetry.value.copy(
            totalActions = _telemetry.value.totalActions + 1
        )
    }

    private fun dispatchTapGesture(x: Float, y: Float): Boolean {
        if (x < 0f || y < 0f) return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 65))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun collectActionableNodes(
        node: AccessibilityNodeInfo?,
        output: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null || !node.isVisibleToUser) return
        if (node.isClickable || node.isScrollable || node.isLongClickable) {
            output.add(node)
        }
        for (i in 0 until node.childCount) {
            collectActionableNodes(node.getChild(i), output)
        }
    }

    private fun computeUiStateHash(pkg: String, nodes: List<AccessibilityNodeInfo>): String {
        val rawStructure = buildString {
            append(pkg)
            nodes.forEach { n ->
                append("|").append(n.className).append(":").append(n.ViewIdResourceName())
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(rawStructure.toByteArray())
        return digest.take(4).joinToString("") { "%02X".format(it) }
    }

    private fun getNodeSignature(stateHash: String, node: AccessibilityNodeInfo): String {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return "$stateHash:${node.className}:${node.ViewIdResourceName()}:${rect.centerX()}x${rect.centerY()}"
    }

    private fun AccessibilityNodeInfo.ViewIdResourceName(): String =
        this.viewIdResourceName ?: "no_id"

    private fun updateUtgTelemetry(hash: String, pkg: String, count: Int, visits: Int) {
        val existing = _telemetry.value.utgNodes.toMutableList()
        val idx = existing.indexOfFirst { it.stateHash == hash }
        val updatedNode = UtgNode(hash, pkg, count, visits)
        if (idx >= 0) {
            existing[idx] = updatedNode
        } else {
            existing.add(0, updatedNode)
        }
        _telemetry.value = _telemetry.value.copy(
            currentStateHash = hash,
            uniqueStatesDiscovered = existing.size,
            utgNodes = existing.take(25)
        )
    }
}

