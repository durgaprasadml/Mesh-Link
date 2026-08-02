package com.meshlink.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshlink.ui.components.MeshOnboardingTopAppBar
import com.meshlink.ui.profile.ProfileSetupScreen
import com.meshlink.ui.profile.ProfileSetupViewModel

enum class OnboardingStep {
    SPLASH,
    WELCOME,
    WALKTHROUGH,
    PERMISSIONS,
    PROFILE_SETUP,
    IDENTITY_REVIEW,
    COMPLETE
}

/**
 * Presentation Orchestrator for Mesh-Link Onboarding Flow.
 * Coordinates Splash → Welcome → Walkthrough → Permissions → Profile → Identity → Complete.
 * Supports Phone (Single Column), Tablet/Foldable (Dual Pane), and Landscape adaptive viewports.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MeshOnboardingScreen(
    onOnboardingFinished: () -> Unit,
    profileSetupViewModel: ProfileSetupViewModel = hiltViewModel(),
    isTabletOrWide: Boolean = false,
    modifier: Modifier = Modifier
) {
    ProvideReducedMotion {
        val reducedMotion = LocalReducedMotion.current
        var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
        var isSplashActive by remember { mutableStateOf(true) }

        var tempDisplayName by remember { mutableStateOf("") }
        var tempAboutText by remember { mutableStateOf("Available on Mesh-Link") }

        if (isSplashActive) {
            SplashScreen(
                onSplashFinished = { isSplashActive = false }
            )
            return@ProvideReducedMotion
        }

        val stepProgress = when (currentStep) {
            OnboardingStep.SPLASH -> 0.0f
            OnboardingStep.WELCOME -> 0.16f
            OnboardingStep.WALKTHROUGH -> 0.33f
            OnboardingStep.PERMISSIONS -> 0.50f
            OnboardingStep.PROFILE_SETUP -> 0.66f
            OnboardingStep.IDENTITY_REVIEW -> 0.83f
            OnboardingStep.COMPLETE -> 1.00f
        }

        val showTopBar = currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.COMPLETE

        Scaffold(
            topBar = {
                if (showTopBar) {
                    MeshOnboardingTopAppBar(
                        title = "Setup Mesh Identity",
                        stepText = "Step ${currentStep.ordinal} of ${OnboardingStep.values().size - 1}",
                        progress = stepProgress,
                        onBackClick = {
                            when (currentStep) {
                                OnboardingStep.WALKTHROUGH -> currentStep = OnboardingStep.WELCOME
                                OnboardingStep.PERMISSIONS -> currentStep = OnboardingStep.WALKTHROUGH
                                OnboardingStep.PROFILE_SETUP -> currentStep = OnboardingStep.PERMISSIONS
                                OnboardingStep.IDENTITY_REVIEW -> currentStep = OnboardingStep.PROFILE_SETUP
                                else -> {}
                            }
                        },
                        onSkipClick = if (currentStep == OnboardingStep.WALKTHROUGH || currentStep == OnboardingStep.PERMISSIONS) {
                            { currentStep = OnboardingStep.PROFILE_SETUP }
                        } else null
                    )
                }
            },
            modifier = modifier.fillMaxSize()
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SetupProgress(
                    currentStepIndex = currentStep.ordinal,
                    totalSteps = OnboardingStep.values().size,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            OnboardingAnimations.sharedAxisXForwardEnter(reducedMotion) togetherWith
                                    OnboardingAnimations.sharedAxisXForwardExit(reducedMotion)
                        } else {
                            OnboardingAnimations.sharedAxisXBackwardEnter(reducedMotion) togetherWith
                                    OnboardingAnimations.sharedAxisXBackwardExit(reducedMotion)
                        }
                    },
                    label = "OnboardingStepTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetStep ->
                    when (targetStep) {
                        OnboardingStep.SPLASH -> {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        OnboardingStep.WELCOME -> {
                            WelcomeScreen(
                                onGetStartedClick = { currentStep = OnboardingStep.WALKTHROUGH },
                                onAlreadyHaveProfileClick = { currentStep = OnboardingStep.PROFILE_SETUP }
                            )
                        }
                        OnboardingStep.WALKTHROUGH -> {
                            OnboardingPager(
                                onSkipClick = { currentStep = OnboardingStep.PROFILE_SETUP },
                                onContinueClick = { currentStep = OnboardingStep.PERMISSIONS }
                            )
                        }
                        OnboardingStep.PERMISSIONS -> {
                            PermissionScreen(
                                onContinueClick = { currentStep = OnboardingStep.PROFILE_SETUP }
                            )
                        }
                        OnboardingStep.PROFILE_SETUP -> {
                            ProfileSetupScreen(
                                viewModel = profileSetupViewModel,
                                onSetupSuccess = { currentStep = OnboardingStep.IDENTITY_REVIEW }
                            )
                        }
                        OnboardingStep.IDENTITY_REVIEW -> {
                            IdentityPreview(
                                displayName = tempDisplayName.ifBlank { "Mesh User" },
                                aboutText = tempAboutText,
                                avatarUri = profileSetupViewModel.selectedAvatarUri.value,
                                onConfirmClick = { currentStep = OnboardingStep.COMPLETE },
                                onEditClick = { currentStep = OnboardingStep.PROFILE_SETUP }
                            )
                        }
                        OnboardingStep.COMPLETE -> {
                            SetupComplete(
                                onGoToHomeClick = onOnboardingFinished
                            )
                        }
                    }
                }
            }
        }
    }
}
