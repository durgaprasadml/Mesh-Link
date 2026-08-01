package com.meshlink.ui.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

/**
 * Screen Transition System for Mesh-Link 2026.
 * Standardizes motion transitions for top-level navigation, forward detail transitions, and modal overlays.
 */
object MeshTransitionSystem {

    val TopLevelEnter: EnterTransition = MeshNavigationMotion.FadeThroughEnter
    val TopLevelExit: ExitTransition = MeshNavigationMotion.FadeThroughExit

    val ForwardEnter: EnterTransition = MeshNavigationMotion.ForwardEnter
    val ForwardExit: ExitTransition = MeshNavigationMotion.ForwardExit

    val BackEnter: EnterTransition = MeshNavigationMotion.BackEnter
    val BackExit: ExitTransition = MeshNavigationMotion.BackExit

    val SharedAxisXEnter: EnterTransition = MeshNavigationMotion.SharedAxisXEnter
    val SharedAxisXExit: ExitTransition = MeshNavigationMotion.SharedAxisXExit

    val SharedAxisYEnter: EnterTransition = MeshNavigationMotion.SharedAxisYEnter
    val SharedAxisYExit: ExitTransition = MeshNavigationMotion.SharedAxisYExit

    val SharedAxisZEnter: EnterTransition = MeshNavigationMotion.SharedAxisZEnter
    val SharedAxisZExit: ExitTransition = MeshNavigationMotion.SharedAxisZExit

    val FadeThroughEnter: EnterTransition = MeshNavigationMotion.FadeThroughEnter
    val FadeThroughExit: ExitTransition = MeshNavigationMotion.FadeThroughExit

    val ScaleEnter: EnterTransition = MeshNavigationMotion.ScaleEnter
    val ScaleExit: ExitTransition = MeshNavigationMotion.ScaleExit

    val ModalSlideUpEnter: EnterTransition = MeshNavigationMotion.SlideInUp
    val ModalSlideDownExit: ExitTransition = MeshNavigationMotion.SlideOutDown

    val ContainerTransformEnter: EnterTransition = MeshNavigationMotion.ContainerTransformEnter
    val ContainerTransformExit: ExitTransition = MeshNavigationMotion.ContainerTransformExit
}

