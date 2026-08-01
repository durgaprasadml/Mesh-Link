package com.meshlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun ActivityTimelineSection(
    nearbyDevices: List<BleDevice>,
    modifier: Modifier = Modifier
) {
    val events = remember(nearbyDevices) {
        val list = mutableListOf<MeshActivityEvent>()

        if (nearbyDevices.isNotEmpty()) {
            val firstPeer = nearbyDevices.first()
            list.add(
                MeshActivityEvent(
                    id = "evt_1",
                    title = "Node Discovered",
                    description = "${firstPeer.name} joined nearby BLE range (-${kotlin.math.abs(firstPeer.rssi)} dBm)",
                    timestamp = "Just now",
                    icon = Icons.Default.PersonAdd,
                    type = MeshActivityEvent.EventType.PEER_JOINED
                )
            )
        }

        list.add(
            MeshActivityEvent(
                id = "evt_2",
                title = "Signal E2E Handshake",
                description = "Diffie-Hellman ratcheted session key initialized",
                timestamp = "2m ago",
                icon = Icons.Default.Key,
                type = MeshActivityEvent.EventType.HANDSHAKE_COMPLETED
            )
        )

        list.add(
            MeshActivityEvent(
                id = "evt_3",
                title = "Multi-Hop Route Mesh",
                description = "BLE 5.3 + Wi-Fi Direct multi-hop routing active",
                timestamp = "5m ago",
                icon = Icons.Default.CellTower,
                type = MeshActivityEvent.EventType.ROUTE_DISCOVERED
            )
        )

        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        // Section title header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF00B0FF))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "LIVE MESH EVENT STREAM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MeshTheme.colors.textPrimary,
                letterSpacing = 1.sp
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(0.5.dp, MeshTheme.colors.border, RoundedCornerShape(20.dp)),
            color = MeshTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                events.forEachIndexed { index, event ->
                    TimelineItemRow(
                        event = event,
                        isLast = index == events.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItemRow(
    event: MeshActivityEvent,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Left Indicator Bar + Node Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (event.type) {
                            MeshActivityEvent.EventType.PEER_JOINED -> Color(0xFF00E676)
                            MeshActivityEvent.EventType.HANDSHAKE_COMPLETED -> MeshTheme.colors.primary
                            MeshActivityEvent.EventType.SOS_ALERT -> Color(0xFFFF5252)
                            else -> Color(0xFF00B0FF)
                        }
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .weight(1f)
                        .background(MeshTheme.colors.border)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary
                )

                Text(
                    text = event.timestamp,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MeshTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = event.description,
                fontSize = 12.sp,
                color = MeshTheme.colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
