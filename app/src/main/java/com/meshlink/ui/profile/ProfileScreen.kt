package com.meshlink.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.components.LoadingOverlay
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(uiState.user?.name) { mutableStateOf(uiState.user?.name ?: "") }
    var aboutMe by remember(uiState.user?.aboutMe) { mutableStateOf(uiState.user?.aboutMe ?: "") }
    var avatarUriString by remember(uiState.user?.avatarUri) { mutableStateOf(uiState.user?.avatarUri) }

    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAvatarGridSheet by remember { mutableStateOf(false) }

    val optionsSheetState = rememberModalBottomSheetState()
    val avatarSheetState = rememberModalBottomSheetState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            avatarUriString = tempCameraUri.toString()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File.createTempFile("profile_camera_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (_: Exception) {}
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                avatarUriString = it.toString()
            }
        }
    )

    AnimatedErrorDialog(
        visible = uiState.saveError != null,
        title = "Profile Update Error",
        message = uiState.saveError ?: "",
        onDismiss = { viewModel.dismissError() },
        primaryButtonText = "OK",
        onPrimaryClick = { viewModel.dismissError() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveProfile(name, aboutMe, avatarUriString)
                        },
                        enabled = name.isNotBlank() && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(MeshTheme.spacing.mediumLarge),
                                strokeWidth = MeshTheme.spacing.extraSmall
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(MeshTheme.spacing.mediumLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Avatar Container with Camera Badge
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { showOptionsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        UserAvatar(
                            identity = com.meshlink.domain.model.UserIdentity.create(
                                userId = uiState.user?.meshId ?: "",
                                displayName = name,
                                avatarUri = avatarUriString
                            ),
                            size = 120.dp,
                            contentDescriptionText = "Profile Picture"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Edit Profile Picture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.extraLarge))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

                // About Me Input
                OutlinedTextField(
                    value = aboutMe,
                    onValueChange = { aboutMe = it },
                    label = { Text("About Me") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

                // Mesh ID Display (Read only)
                uiState.user?.meshId?.let { meshId ->
                    Text(
                        text = "Mesh ID: $meshId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LoadingOverlay(isLoading = uiState.isLoading)
        }
    }

    if (showOptionsSheet) {
        ProfileImageOptionsBottomSheet(
            sheetState = optionsSheetState,
            hasCustomImage = !avatarUriString.isNullOrEmpty(),
            onDismiss = { showOptionsSheet = false },
            onTakePhoto = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    try {
                        val photoFile = File.createTempFile("profile_camera_", ".jpg", context.cacheDir)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        tempCameraUri = uri
                        takePictureLauncher.launch(uri)
                    } catch (_: Exception) {}
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onChooseGallery = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onChooseAvatar = {
                showAvatarGridSheet = true
            },
            onRemovePhoto = {
                avatarUriString = null
            }
        )
    }

    if (showAvatarGridSheet) {
        AvatarGridBottomSheet(
            sheetState = avatarSheetState,
            selectedAvatarId = avatarUriString,
            onDismiss = { showAvatarGridSheet = false },
            onAvatarSelected = { avatarUri ->
                avatarUriString = avatarUri
            }
        )
    }
}
