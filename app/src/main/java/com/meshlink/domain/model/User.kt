package com.meshlink.domain.model

data class User(
    val meshId: String,
    val name: String,
    val avatarUri: String? = null,
    val aboutMe: String? = null,
    val profilePhotoPath: String? = null,
    val profilePhotoHash: String? = null,
    val profilePhotoVersion: Long = 0L,
    val profileLastUpdated: Long = 0L
)
