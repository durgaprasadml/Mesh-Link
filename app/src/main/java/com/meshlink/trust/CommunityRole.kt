package com.meshlink.trust

/**
 * Community roles defining user permissions within a specific community context.
 * Kept independent from global TrustLevel.
 */
enum class CommunityRole(val priority: Int) {
    MEMBER(1),
    MODERATOR(2),
    ADMIN(3),
    LEADER(4),
    WARDEN(4),
    EMERGENCY_AUTHORITY(5);

    fun hasAdminPrivileges(): Boolean {
        return this == ADMIN || this == LEADER || this == WARDEN || this == EMERGENCY_AUTHORITY
    }

    fun canModerate(): Boolean {
        return this != MEMBER
    }
}
