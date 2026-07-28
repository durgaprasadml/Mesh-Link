package com.meshlink.domain.model

data class User(
    val meshId: String,
    val name: String,
    val avatarUri: String? = null,
    val aboutMe: String? = null
)
