package com.robco.pipbot

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PhosphorMode(val primary: Color, val dim: Color, val bg: Color, val title: String) {
    PIP_GREEN(
        primary = Color(0xFF18FF62),
        dim = Color(0xFF0B5E28),
        bg = Color(0xFF041106),
        title = "FO3/FO4 PHOSPHOR GREEN"
    ),
    MOJAVE_AMBER(
        primary = Color(0xFFFFB642),
        dim = Color(0xFF704B10),
        bg = Color(0xFF140C02),
        title = "NEW VEGAS AMBER"
    )
}

enum class PipTab(val label: String) {
    STAT("STAT"),
    DIRECTIVES("DIRECTIVES"),
    UTG_RADAR("UTG RADAR"),
    HOLOTAPE("HOLOTAPE")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeshBoiTerminalApp()
        }
    }
}

@Composable
fun MeshBoiTerminalApp() {
    val telemetry by PipBotAccessibilityService.telemetry.collectAsState()
    var phosphor by remember { mutableStateOf(PhosphorMode.PIP_GREEN) }
    var activeTab by remember { mutableStateOf(PipTab.STAT) }
    val context = LocalContext.current
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 50) }

    fun clickSound() {
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 25)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1E1B))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(phosphor.bg)
                .border(3.dp, phosphor.primary.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                RobCoHeader(
                    telemetry = telemetry,
                    phosphor = phosphor,
                    onTogglePhosphor = {
                        clickSound()
                        phosphor = if (phosphor == PhosphorMode.PIP_GREEN) {
                            PhosphorMode.MOJAVE_AMBER
                        } else {
                            PhosphorMode.PIP_GREEN
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        PipTab.STAT -> StatScreen(
                            telemetry = telemetry,
                            phosphor = phosphor,
                            onOpenAccessibility = {
                                clickSound()
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onToggleBot = { run ->
                                clickSound()
                                PipBotAccessibilityService.toggleBot(run)
                            }
                        )
                        PipTab.DIRECTIVES -> DirectivesScreen(
                            telemetry = telemetry,
                            phosphor = phosphor,
                            onClickSound = ::clickSound
                        )
                        PipTab.UTG_RADAR -> UtgRadarScreen(
                            telemetry = telemetry,
                            phosphor = phosphor
                        )
                        PipTab.HOLOTAPE -> HolotapeScreen(
                            telemetry = telemetry,
                            phosphor = phosphor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PipBottomTabs(
                    activeTab = activeTab,
                    phosphor = phosphor,
                    onSelect = {
                        clickSound()
                        activeTab = it
                    }
                )
            }

            CrtScanlineOverlay(phosphor = phosphor)
        }
    }
}

@Composable
fun RobCoHeader(
    telemetry: BotTelemetry,
    phosphor: PhosphorMode,
    onTogglePhosphor: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MESHBOI 3000 // ROBCO AUTOMATOR OS",
                    color = phosphor.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (telemetry.isServiceConnected) {
                        "LINK: ONLINE | STATE [${telemetry.currentStateHash}]"
                    } else {
                        "LINK: OFFLINE (ENABLE ACCESSIBILITY)"
                    },
                    color = phosphor.primary.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .border(1.dp, phosphor.primary, RoundedCornerShape(4.dp))
                    .clickable { onTogglePhosphor() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CRT: ${if (phosphor == PhosphorMode.PIP_GREEN) "GRN" else "AMB"}",
                    color = phosphor.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(thickness = 2.dp, color = phosphor.primary)
    }
}

@Composable
fun StatScreen(
    telemetry: BotTelemetry,
    phosphor: PhosphorMode,
    onOpenAccessibility: () -> Unit,
    onToggleBot: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RetroSectionBox(title = "UNIT CONDITION & TELEMETRY", phosphor = phosphor) {
                RetroStatBar("ACTUATOR LINK", if (telemetry.isServiceConnected) 1.0f else 0.1f, phosphor)
                RetroStatBar("UTG COVERAGE", (telemetry.uniqueStatesDiscovered / 25f).coerceIn(0.05f, 1f), phosphor)
                RetroStatBar("LOOP IMMUNITY", (1f - (telemetry.loopWarnings * 0.1f)).coerceIn(0.2f, 1f), phosphor)
            }

            RetroSectionBox(title = "LIVE SESSION METRICS", phosphor = phosphor) {
                MetricRow("TARGET PACKAGE", telemetry.targetPackage, phosphor)
                MetricRow("ACTIVE POLICY", telemetry.policy.code, phosphor)
                MetricRow("ACTIONS DISPATCHED", telemetry.totalActions.toString(), phosphor)
                MetricRow("UNIQUE UI STATES", telemetry.uniqueStatesDiscovered.toString(), phosphor)
                MetricRow("LOOP RECOVERIES", telemetry.loopWarnings.toString(), phosphor)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!telemetry.isServiceConnected) {
                RetroButton(
                    label = "[1] INITIALIZE ACCESSIBILITY MODULE",
                    phosphor = phosphor,
                    isHighlighted = true,
                    onClick = onOpenAccessibility
                )
            }

            RetroButton(
                label = if (telemetry.isRunning) {
                    ">>> HALT AUTOMATED EXPLORATION <<<"
                } else {
                    ">>> ENGAGE MESHBOI EXPLORATION <<<"
                },
                phosphor = phosphor,
                isHighlighted = telemetry.isRunning,
                onClick = { onToggleBot(!telemetry.isRunning) }
            )
        }
    }
}

@Composable
fun DirectivesScreen(
    telemetry: BotTelemetry,
    phosphor: PhosphorMode,
    onClickSound: () -> Unit
) {
    var packageInput by remember { mutableStateOf(telemetry.targetPackage) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RetroSectionBox(title = "SELECT EXPLORATION DIRECTIVE", phosphor = phosphor) {
            ExplorationPolicy.entries.forEach { policy ->
                val selected = telemetry.policy == policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (selected) phosphor.primary else phosphor.dim,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(if (selected) phosphor.dim.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable {
                            onClickSound()
                            PipBotAccessibilityService.updateConfig(policy = policy)
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selected) "[X] " else "[ ] ",
                        color = phosphor.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = policy.label,
                        color = phosphor.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        RetroSectionBox(title = "ACTUATOR CADENCE", phosphor = phosphor) {
            val speeds = listOf(800L to "TURBO (800ms)", 1500L to "STD (1500ms)", 2800L to "SLOW (2800ms)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                speeds.forEach { (ms, label) ->
                    val active = telemetry.tickIntervalMs == ms
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (active) phosphor.primary else phosphor.dim)
                            .background(if (active) phosphor.dim.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable {
                                onClickSound()
                                PipBotAccessibilityService.updateConfig(intervalMs = ms)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = phosphor.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        RetroSectionBox(title = "TARGET PACKAGE IDENTIFIER", phosphor = phosphor) {
            OutlinedTextField(
                value = packageInput,
                onValueChange = {
                    packageInput = it
                    PipBotAccessibilityService.updateConfig(targetPkg = it)
                },
                textStyle = TextStyle(
                    color = phosphor.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun UtgRadarScreen(
    telemetry: BotTelemetry,
    phosphor: PhosphorMode
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RetroSectionBox(title = "UI TRANSITION GRAPH (UTG) RADAR", phosphor = phosphor) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(phosphor.bg)
                    .border(1.dp, phosphor.dim)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.42f

                drawCircle(color = phosphor.dim, radius = maxRadius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                drawCircle(color = phosphor.dim, radius = maxRadius * 0.6f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                drawLine(color = phosphor.dim, start = Offset(0f, center.y), end = Offset(size.width, center.y))
                drawLine(color = phosphor.dim, start = Offset(center.x, 0f), end = Offset(center.x, size.height))

                val nodes = telemetry.utgNodes.take(12)
                var prevPoint: Offset? = null
                nodes.forEachIndexed { index, _ ->
                    val angle = (index * 0.85f)
                    val dist = ((index + 1).toFloat() / (nodes.size.coerceAtLeast(1) + 1)) * maxRadius
                    val pt = Offset(
                        x = center.x + kotlin.math.cos(angle) * dist,
                        y = center.y + kotlin.math.sin(angle) * dist
                    )
                    prevPoint?.let { prev ->
                        drawLine(
                            color = phosphor.primary.copy(alpha = 0.6f),
                            start = prev,
                            end = pt,
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                        )
                    }
                    drawCircle(
                        color = phosphor.primary,
                        radius = if (index == 0) 7f else 4.5f,
                        center = pt
                    )
                    prevPoint = pt
                }
            }
        }

        RetroSectionBox(title = "DISCOVERED UI STATES", phosphor = phosphor) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(telemetry.utgNodes) { node ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, phosphor.dim)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${node.stateHash}] ${node.packageName.takeLast(16)}",
                            color = phosphor.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "NODES:${node.interactiveCount} | VISITS:${node.visitCount}",
                            color = phosphor.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HolotapeScreen(
    telemetry: BotTelemetry,
    phosphor: PhosphorMode
) {
    RetroSectionBox(title = "HOLOTAPE TELEMETRY STREAM", phosphor = phosphor) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(telemetry.logs) { line ->
                Text(
                    text = line,
                    color = phosphor.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PipBottomTabs(
    activeTab: PipTab,
    phosphor: PhosphorMode,
    onSelect: (PipTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PipTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        BorderStroke(if (selected) 2.dp else 1.dp, if (selected) phosphor.primary else phosphor.dim),
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (selected) phosphor.dim.copy(alpha = 0.45f) else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ ${tab.label} ]",
                    color = phosphor.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun RetroSectionBox(
    title: String,
    phosphor: PhosphorMode,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, phosphor.primary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Text(
            text = ":: $title ::",
            color = phosphor.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun RetroStatBar(label: String, fraction: Float, phosphor: PhosphorMode) {
    val totalBlocks = 16
    val filled = (fraction * totalBlocks).toInt().coerceIn(0, totalBlocks)
    val asciiBar = buildString {
        append("[")
        repeat(filled) { append("█") }
        repeat(totalBlocks - filled) { append("░") }
        append("]")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = phosphor.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(text = asciiBar, color = phosphor.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
fun MetricRow(label: String, value: String, phosphor: PhosphorMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = phosphor.primary.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(text = value, color = phosphor.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun RetroButton(
    label: String,
    phosphor: PhosphorMode,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, phosphor.primary, RoundedCornerShape(4.dp))
            .background(if (isHighlighted) phosphor.primary else phosphor.dim.copy(alpha = 0.25f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isHighlighted) phosphor.bg else phosphor.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CrtScanlineOverlay(phosphor: PhosphorMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "crt_roll")
    val rollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanbar"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 6.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.28f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
            y += lineSpacing
        }

        val beamY = size.height * rollOffset
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    phosphor.primary.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                startY = beamY - 80f,
                endY = beamY + 80f
            ),
            size = size
        )
    }
}
