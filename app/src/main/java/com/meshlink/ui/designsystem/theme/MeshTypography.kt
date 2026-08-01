package com.meshlink.ui.designsystem.theme

import com.meshlink.ui.designsystem.theme.typography.MeshTypographyScale
import com.meshlink.ui.designsystem.theme.typography.toMaterial3Typography

val MeshTypographyScaleInstance = MeshTypographyScale()
val MeshTypography = MeshTypographyScaleInstance.toMaterial3Typography()
val Caption = MeshTypographyScaleInstance.caption
val MonospaceMetadata = MeshTypographyScaleInstance.metadata
