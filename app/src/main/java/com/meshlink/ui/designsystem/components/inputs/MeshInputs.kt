package com.meshlink.ui.designsystem.components.inputs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.accessibility.meshMinTouchTarget
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun MeshSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    onClearClick: (() -> Unit)? = null
) {
    val colors = LocalMeshSemanticColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = placeholderText, style = MeshTheme.typography.bodyMedium, color = colors.textSecondary) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = colors.textSecondary) },
        trailingIcon = {
            if (query.isNotEmpty() && onClearClick != null) {
                IconButton(onClick = onClearClick) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = colors.textSecondary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.glassSurface,
            unfocusedContainerColor = colors.glassSurface,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MeshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val colors = LocalMeshSemanticColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            placeholder = placeholder?.let { { Text(text = it) } },
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null, tint = colors.textSecondary) } },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(imageVector = icon, contentDescription = null, tint = colors.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.glassSurface,
                unfocusedContainerColor = colors.glassSurface,
                focusedBorderColor = if (isError) colors.danger else colors.primary,
                unfocusedBorderColor = if (isError) colors.danger else colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MeshTheme.typography.bodySmall,
                color = colors.danger,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun MeshPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    MeshTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
        onTrailingIconClick = { passwordVisible = !passwordVisible }
    )
}

@Composable
fun MeshDropdown(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.glassSurface,
                unfocusedContainerColor = colors.glassSurface,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.cardSurface)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(text = option, color = colors.textPrimary) },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MeshSegmentedControls(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.elevatedSurface)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val bgColor = if (selected) colors.primary else Color.Transparent
            val textColor = if (selected) Color.White else colors.textSecondary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = title, style = MeshTheme.typography.labelMedium, color = textColor)
            }
        }
    }
}

@Composable
fun MeshSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 2f,
        animationSpec = tween(200),
        label = "switchThumb"
    )
    val bgColor = if (checked) colors.primary else colors.outline.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .meshMinTouchTarget()
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun MeshCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    val bgColor = if (checked) colors.primary else Color.Transparent
    val borderColor = if (checked) colors.primary else colors.outline

    Box(
        modifier = modifier
            .size(24.dp)
            .meshMinTouchTarget()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun MeshRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current
    val borderColor = if (selected) colors.primary else colors.outline

    Box(
        modifier = modifier
            .size(24.dp)
            .meshMinTouchTarget()
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
        }
    }
}

@Composable
fun MeshSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val colors = LocalMeshSemanticColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.border
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MeshLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalMeshSemanticColors.current.primary
) {
    val colors = LocalMeshSemanticColors.current
    LinearProgressIndicator(
        progress = { progress },
        color = color,
        trackColor = colors.border.copy(alpha = 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
    )
}

@Composable
fun MeshCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = LocalMeshSemanticColors.current.primary,
    size: Dp = 32.dp
) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = 3.dp,
        modifier = modifier.size(size)
    )
}
