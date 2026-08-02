package com.meshlink.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.AnimatedErrorDialog
import com.meshlink.ui.settings.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAvatarGridSheet by remember { mutableStateOf(false) }

    val optionsSheetState = rememberModalBottomSheetState()
    val avatarSheetState = rememberModalBottomSheetState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var currentAvatarUriString by remember(profileState.user?.avatarUri) { mutableStateOf(profileState.user?.avatarUri) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            currentAvatarUriString = tempCameraUri.toString()
            viewModel.saveProfile(
                name = profileState.user?.name ?: "",
                aboutMe = profileState.user?.aboutMe,
                avatarUri = currentAvatarUriString
            )
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
                currentAvatarUriString = it.toString()
                viewModel.saveProfile(
                    name = profileState.user?.name ?: "",
                    aboutMe = profileState.user?.aboutMe,
                    avatarUri = currentAvatarUriString
                )
            }
        }
    )

    AnimatedErrorDialog(
        visible = profileState.saveError != null,
        title = "Profile Update Error",
        message = profileState.saveError ?: "",
        onDismiss = { viewModel.dismissError() },
        primaryButtonText = "OK",
        onPrimaryClick = { viewModel.dismissError() }
    )

    MeshProfileScreen(
        profileState = profileState,
        settingsState = settingsState,
        onNavigateBack = onNavigateBack,
        onEditAvatarClick = { showOptionsSheet = true },
        onSaveProfile = { name, aboutMe, avatarUri ->
            viewModel.saveProfile(name, aboutMe, avatarUri ?: currentAvatarUriString)
        },
        onExportLogs = { settingsViewModel.exportDebugLogs() },
        onShowToast = { settingsViewModel.showToast(it) }
    )

    if (showOptionsSheet) {
        ProfileImageOptionsBottomSheet(
            sheetState = optionsSheetState,
            hasCustomImage = !currentAvatarUriString.isNullOrEmpty(),
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
                currentAvatarUriString = null
                viewModel.saveProfile(
                    name = profileState.user?.name ?: "",
                    aboutMe = profileState.user?.aboutMe,
                    avatarUri = null
                )
            }
        )
    }

    if (showAvatarGridSheet) {
        AvatarGridBottomSheet(
            sheetState = avatarSheetState,
            selectedAvatarId = currentAvatarUriString,
            onDismiss = { showAvatarGridSheet = false },
            onAvatarSelected = { avatarUri ->
                currentAvatarUriString = avatarUri
                viewModel.saveProfile(
                    name = profileState.user?.name ?: "",
                    aboutMe = profileState.user?.aboutMe,
                    avatarUri = avatarUri
                )
            }
        )
    }
}
