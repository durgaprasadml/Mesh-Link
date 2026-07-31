package com.meshlink.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onSetupSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    val selectedAvatarUri by viewModel.selectedAvatarUri.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOptionsSheet by remember { mutableStateOf(false) }
    var showAvatarGridSheet by remember { mutableStateOf(false) }

    val optionsSheetState = rememberModalBottomSheetState()
    val avatarSheetState = rememberModalBottomSheetState()

    // Temporary camera image URI storage
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.setAvatarUri(tempCameraUri.toString())
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
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to launch camera: ${e.localizedMessage}") }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Camera permission is required to capture a photo") }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.setAvatarUri(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ProfileSetupEvent.SetupSuccess -> {
                    onSetupSuccess()
                }
                is ProfileSetupEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    MeshScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mesh Link") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(MeshTheme.spacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Mesh Link",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
            
            Text(
                text = "Create your profile to join the mesh network.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraGiant))

            // Large Circular Profile Picture Container
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clickable { showOptionsSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                ) {
                    UserAvatar(
                        identity = com.meshlink.domain.model.UserIdentity.create(
                            userId = "preview",
                            displayName = displayName,
                            avatarUri = selectedAvatarUri
                        ),
                        size = 130.dp,
                        contentDescriptionText = "Profile Picture Preview"
                    )
                }

                // Camera Badge Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Profile Picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraGiant))

            // Display Name Input
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Display Name Icon"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = displayName.isNotEmpty() && (displayName.trim().length < 2 || displayName.trim().length > 30),
                supportingText = {
                    val len = displayName.trim().length
                    if (displayName.isNotEmpty() && (len < 2 || len > 30)) {
                        Text("Name must be between 2 and 30 characters ($len/30)")
                    } else {
                        Text("$len/30 characters")
                    }
                }
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

            // Create Profile Button
            Button(
                onClick = { viewModel.createProfile(displayName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MeshTheme.shapes.large,
                enabled = uiState !is ProfileSetupUiState.Loading && displayName.trim().length in 2..30
            ) {
                AnimatedVisibility(
                    visible = uiState is ProfileSetupUiState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = MeshTheme.spacing.small)
                            .size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = "Create Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Photo Options Bottom Sheet
    if (showOptionsSheet) {
        ProfileImageOptionsBottomSheet(
            sheetState = optionsSheetState,
            hasCustomImage = !selectedAvatarUri.isNullOrEmpty(),
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
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("Failed to launch camera: ${e.localizedMessage}") }
                    }
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
                viewModel.setAvatarUri(null)
            }
        )
    }

    // Avatar Selection Grid Bottom Sheet
    if (showAvatarGridSheet) {
        AvatarGridBottomSheet(
            sheetState = avatarSheetState,
            selectedAvatarId = selectedAvatarUri,
            onDismiss = { showAvatarGridSheet = false },
            onAvatarSelected = { avatarUri ->
                viewModel.setAvatarUri(avatarUri)
            }
        )
    }
}
