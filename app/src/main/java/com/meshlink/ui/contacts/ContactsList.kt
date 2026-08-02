package com.meshlink.ui.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.User
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.profile.ContactFilterOption
import com.meshlink.ui.profile.ContactUi
import com.meshlink.ui.profile.TrustLevelUi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsList(
    contacts: List<ContactUi> = defaultContactsList(),
    query: String = "",
    selectedFilter: ContactFilterOption = ContactFilterOption.ALL,
    onContactClick: (ContactUi) -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Filter and search logic
    val filteredContacts = contacts.filter { contact ->
        val matchesQuery = query.isBlank() ||
                contact.displayName.contains(query, ignoreCase = true) ||
                contact.meshId.contains(query, ignoreCase = true) ||
                contact.deviceModel.contains(query, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            ContactFilterOption.ALL -> true
            ContactFilterOption.NEARBY -> contact.isMeshConnected
            ContactFilterOption.ONLINE -> contact.isOnline
            ContactFilterOption.OFFLINE -> !contact.isOnline
            ContactFilterOption.TRUSTED -> contact.trustLevel == TrustLevelUi.TRUSTED || contact.trustLevel == TrustLevelUi.VERIFIED
            ContactFilterOption.VERIFIED -> contact.trustLevel == TrustLevelUi.VERIFIED
        }

        matchesQuery && matchesFilter
    }

    if (filteredContacts.isEmpty()) {
        NoContacts(
            onDiscoverClick = onDiscoverClick,
            modifier = modifier
        )
        return
    }

    // Group alphabetically by initial letter
    val grouped = filteredContacts
        .sortedBy { it.displayName.uppercase() }
        .groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        grouped.forEach { (initial, contactGroup) ->
            stickyHeader(key = "header_$initial") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(
                items = contactGroup,
                key = { it.id }
            ) { contact ->
                ContactItemRow(
                    contact = contact,
                    onClick = { onContactClick(contact) }
                )
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    contact: ContactUi,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = "Contact item for ${contact.displayName}" }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(48.dp)
            ) {
                UserAvatar(
                    identity = UserIdentity.create(
                        userId = contact.meshId,
                        displayName = contact.displayName,
                        avatarUri = contact.avatarUri
                    ),
                    size = 48.dp,
                    contentDescriptionText = "Avatar for ${contact.displayName}"
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (contact.isOnline) MeshTheme.colors.online else Color.Gray)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.trustLevel == TrustLevelUi.VERIFIED) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Identity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = contact.statusMessage ?: "ID: ${contact.meshId.take(12).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (contact.isMeshConnected) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Mesh Connected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun defaultContactsList(): List<ContactUi> = listOf(
    ContactUi(
        id = "CNT-001",
        displayName = "Commander Alpha",
        meshId = "NODE-8A9F-B4C2-9102",
        statusMessage = "Leading mesh operations in Sector 4",
        isOnline = true,
        isMeshConnected = true,
        trustLevel = TrustLevelUi.VERIFIED
    ),
    ContactUi(
        id = "CNT-002",
        displayName = "Bravo Leader",
        meshId = "NODE-4321-8765-FEDC",
        statusMessage = "Relay node active • E2EE enabled",
        isOnline = true,
        isMeshConnected = true,
        trustLevel = TrustLevelUi.TRUSTED
    ),
    ContactUi(
        id = "CNT-003",
        displayName = "Charlie Dispatch",
        meshId = "NODE-9900-1122-3344",
        statusMessage = "Standby emergency channel",
        isOnline = false,
        isMeshConnected = false,
        trustLevel = TrustLevelUi.VERIFIED
    ),
    ContactUi(
        id = "CNT-004",
        displayName = "Delta Recon",
        meshId = "NODE-7788-9900-1122",
        statusMessage = "BLE Mesh multi-hop enabled",
        isOnline = true,
        isMeshConnected = true,
        trustLevel = TrustLevelUi.TRUSTED
    ),
    ContactUi(
        id = "CNT-005",
        displayName = "Echo Medical Node",
        meshId = "NODE-5566-7788-9900",
        statusMessage = "Medical mesh responder",
        isOnline = false,
        isMeshConnected = false,
        trustLevel = TrustLevelUi.VERIFIED
    )
)
