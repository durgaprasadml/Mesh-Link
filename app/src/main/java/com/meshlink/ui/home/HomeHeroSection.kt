package com.meshlink.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.UserAvatar
import java.util.Calendar

private fun getGreeting(displayName: String?): String {
    val name = displayName?.takeIf { it.isNotBlank() } ?: "Durga"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeOfDayGreeting = when (hour) {
        in 4..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return "$timeOfDayGreeting, $name"
}

@Composable
fun HomeHeroSection(
    userIdentity: UserIdentity?,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember(userIdentity?.displayName) {
        getGreeting(userIdentity?.displayName)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Greeting and Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greeting,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesh Connected ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "●",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Right side: Profile Avatar (48dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onNavigateToSettings)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "Open Profile Settings"
                },
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                identity = userIdentity,
                size = 48.dp
            )
        }
    }
}
