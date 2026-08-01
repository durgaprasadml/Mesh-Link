package com.meshlink.ui.designsystem.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Unified Bottom Sheet Framework for Mesh-Link 2026.
 * Contains MeshBottomSheet base container, Confirmation Sheet, Error Sheet,
 * Success Sheet, and Information Sheet with shared glass styling and elevation.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MeshTheme.colors.surface,
    scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        scrimColor = scrimColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(MeshTheme.shapes.pill)
                    .background(MeshTheme.colors.textSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            content()
        }
    }
}

/**
 * Confirmation Bottom Sheet for user actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshConfirmationSheet(
    visible: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    if (!visible) return

    MeshBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tintColor = if (isDestructive) MeshTheme.colors.error else MeshTheme.colors.primary

            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MeshTheme.shapes.pill
                ) {
                    Text(text = cancelText, color = MeshTheme.colors.textPrimary)
                }

                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MeshTheme.shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) MeshTheme.colors.error else MeshTheme.colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = confirmText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Error Bottom Sheet for error messages and codes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshErrorSheet(
    visible: Boolean,
    title: String = "An Error Occurred",
    errorMessage: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorCode: String? = null,
    actionText: String = "Dismiss",
    onRetry: (() -> Unit)? = null
) {
    if (!visible) return

    MeshBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MeshTheme.colors.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MeshTheme.colors.error,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary
            )

            if (errorCode != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MeshTheme.shapes.pill,
                    color = MeshTheme.colors.error.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MeshTheme.colors.error.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "ERROR CODE: $errorCode",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onRetry != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MeshTheme.shapes.pill
                    ) {
                        Text(text = "Close", color = MeshTheme.colors.textPrimary)
                    }

                    Button(
                        onClick = {
                            onRetry()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MeshTheme.shapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeshTheme.colors.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Retry", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MeshTheme.shapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeshTheme.colors.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = actionText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Success Bottom Sheet for task completion feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshSuccessSheet(
    visible: Boolean,
    title: String = "Success",
    description: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String = "Continue",
    onAction: (() -> Unit)? = null
) {
    if (!visible) return

    MeshBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MeshTheme.colors.success.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MeshTheme.colors.success,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MeshTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onAction?.invoke()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshTheme.colors.primary,
                    contentColor = Color.Black
                )
            ) {
                Text(text = actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Information Bottom Sheet for contextual help, details, or metadata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshInformationSheet(
    visible: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    details: List<Pair<String, String>> = emptyList()
) {
    if (!visible) return

    MeshBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = MeshTheme.colors.textSecondary
            )

            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MeshTheme.shapes.medium,
                    color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MeshTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        details.forEachIndexed { index, (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = MeshTheme.colors.textSecondary
                                )
                                Text(
                                    text = value,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MeshTheme.colors.textPrimary
                                )
                            }
                            if (index < details.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MeshTheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshTheme.colors.primary,
                    contentColor = Color.Black
                )
            ) {
                Text(text = "Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}
