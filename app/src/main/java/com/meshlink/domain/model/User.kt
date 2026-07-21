package com.meshlink.domain.model

data class User(
    val meshId: String,
    val name: String,
    val phoneNumber: String,
    val avatarUri: String? = null,
    val aboutMe: String? = null
)
