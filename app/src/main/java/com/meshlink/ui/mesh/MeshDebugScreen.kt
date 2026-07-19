package com.meshlink.ui.mesh

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import com.meshlink.ui.designsystem.theme.MeshTheme

data class MeshNode(
    val id: String,
    val name: String,
    val angle: Float,
    val distance: Float,
    val signal: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDebugScreen(
    onBack: () -> Unit,
    viewModel: MeshDebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Build the dynamic nodes based on real scanned data + route table
    val nodes = remember(uiState.scannedDevices, uiState.routeTable) {
        val uniqueIds = (uiState.scannedDevices.keys + uiState.routeTable.keys).distinct()
        uniqueIds.mapIndexed { index, id ->
            val isDirect = uiState.scannedDevices.containsKey(id)
            val baseAngle = (index.toFloat() / uniqueIds.size.coerceAtLeast(1)) * (2 * Math.PI.toFloat())
            val randomOffset = Random(id.hashCode()).nextFloat() * 0.5f
            
            MeshNode(
                id = id,
                name = uiState.scannedDevices[id]?.name?.takeIf { it.isNotBlank() } ?: "Node ${id.takeLast(4)}",
                angle = baseAngle + randomOffset,
                distance = if (isDirect) 0.4f + randomOffset * 0.3f else 0.7f + randomOffset * 0.3f,
                signal = if (isDirect) 1.0f else 0.5f
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                title = { Text("Network Topography", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (nodes.isEmpty()) {
                Text("No active mesh nodes detected.", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
            } else {
                MeshGraph(nodes)
            }
        }
    }
}

@Composable
fun MeshGraph(nodes: List<MeshNode>) {
    val infiniteTransition = rememberInfiniteTransition(label = "GraphAnimation")
    
    // Slow rotation animation for the entire graph
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Pulsing effect for lines
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = minOf(size.width, size.height) / 2.5f

        // Draw connections first (so they are under the nodes)
        nodes.forEach { node ->
            val nodeX = center.x + cos(node.angle + rotation) * (node.distance * maxRadius)
            val nodeY = center.y + sin(node.angle + rotation) * (node.distance * maxRadius)
            
            // Connection to center
            drawLine(
                color = primaryColor.copy(alpha = node.signal * pulse),
                start = center,
                end = Offset(nodeX, nodeY),
                strokeWidth = (node.signal * 8f) * pulse,
                cap = StrokeCap.Round
            )
            
            // Random connections between nodes for a "mesh" look
            nodes.filter { it.id != node.id && Random(it.id.hashCode() + node.id.hashCode()).nextFloat() > 0.6f }.forEach { targetNode ->
                val targetX = center.x + cos(targetNode.angle + rotation) * (targetNode.distance * maxRadius)
                val targetY = center.y + sin(targetNode.angle + rotation) * (targetNode.distance * maxRadius)
                
                drawLine(
                    color = primaryColor.copy(alpha = 0.15f * pulse),
                    start = Offset(nodeX, nodeY),
                    end = Offset(targetX, targetY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw Center Node (You)
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = 40f * pulse,
            center = center
        )
        drawCircle(
            color = primaryColor,
            radius = 20f,
            center = center
        )

        // Draw Other Nodes
        nodes.forEach { node ->
            val nodeX = center.x + cos(node.angle + rotation) * (node.distance * maxRadius)
            val nodeY = center.y + sin(node.angle + rotation) * (node.distance * maxRadius)
            
            // Outer glow
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 16f,
                center = Offset(nodeX, nodeY)
            )
            // Inner core
            drawCircle(
                color = primaryColor,
                radius = 8f,
                center = Offset(nodeX, nodeY)
            )
        }
    }
}
