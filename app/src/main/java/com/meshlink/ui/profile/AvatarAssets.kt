package com.meshlink.ui.profile

import com.meshlink.R

data class AvatarItem(
    val id: String,
    val resId: Int,
    val description: String,
    val category: String
)

object AvatarAssets {

    val AVATARS: List<AvatarItem> = listOf(
        AvatarItem("avatar_01", R.drawable.avatar_01, "Avatar 1: Professional Male with short hair", "Male"),
        AvatarItem("avatar_02", R.drawable.avatar_02, "Avatar 2: Female with long hair", "Female"),
        AvatarItem("avatar_03", R.drawable.avatar_03, "Avatar 3: Male with glasses", "Accessories"),
        AvatarItem("avatar_04", R.drawable.avatar_04, "Avatar 4: Female with Hijab", "Female"),
        AvatarItem("avatar_05", R.drawable.avatar_05, "Avatar 5: Male with Turban", "Male"),
        AvatarItem("avatar_06", R.drawable.avatar_06, "Avatar 6: Young Male with Beanie", "Accessories"),
        AvatarItem("avatar_07", R.drawable.avatar_07, "Avatar 7: Female with Afro and Bowtie", "Female"),
        AvatarItem("avatar_08", R.drawable.avatar_08, "Avatar 8: Male with Beard", "Male"),
        AvatarItem("avatar_09", R.drawable.avatar_09, "Avatar 9: Female with Glasses and Top Bun", "Accessories"),
        AvatarItem("avatar_10", R.drawable.avatar_10, "Avatar 10: Male with Side Part hair", "Male"),
        AvatarItem("avatar_11", R.drawable.avatar_11, "Avatar 11: Elderly Male with Glasses", "Male"),
        AvatarItem("avatar_12", R.drawable.avatar_12, "Avatar 12: Female with Curly Hair", "Female"),
        AvatarItem("avatar_13", R.drawable.avatar_13, "Avatar 13: Male with Glasses and Beard", "Accessories"),
        AvatarItem("avatar_14", R.drawable.avatar_14, "Avatar 14: Female with Hijab and Glasses", "Female"),
        AvatarItem("avatar_15", R.drawable.avatar_15, "Avatar 15: Male with Turban and Bowtie", "Male"),
        AvatarItem("avatar_16", R.drawable.avatar_16, "Avatar 16: Male with Beanie and Glasses", "Accessories"),
        AvatarItem("avatar_17", R.drawable.avatar_17, "Avatar 17: Female with Long Red Hair", "Female"),
        AvatarItem("avatar_18", R.drawable.avatar_18, "Avatar 18: Male with Beard and Side Part", "Male"),
        AvatarItem("avatar_19", R.drawable.avatar_19, "Avatar 19: Female with Glasses", "Accessories"),
        AvatarItem("avatar_20", R.drawable.avatar_20, "Avatar 20: Young Male with Afro", "Male"),
        AvatarItem("avatar_21", R.drawable.avatar_21, "Avatar 21: Male with Glasses and Bowtie", "Accessories"),
        AvatarItem("avatar_22", R.drawable.avatar_22, "Avatar 22: Female with Top Bun", "Female"),
        AvatarItem("avatar_23", R.drawable.avatar_23, "Avatar 23: Male with Short Hair", "Male"),
        AvatarItem("avatar_24", R.drawable.avatar_24, "Avatar 24: Female with Hijab", "Female"),
        AvatarItem("avatar_25", R.drawable.avatar_25, "Avatar 25: Male with Turban and Beard", "Male"),
        AvatarItem("avatar_26", R.drawable.avatar_26, "Avatar 26: Young Male with Beanie", "Accessories"),
        AvatarItem("avatar_27", R.drawable.avatar_27, "Avatar 27: Female with Glasses and Bowtie", "Accessories"),
        AvatarItem("avatar_28", R.drawable.avatar_28, "Avatar 28: Male with Beard", "Male"),
        AvatarItem("avatar_29", R.drawable.avatar_29, "Avatar 29: Female with Long Hair", "Female"),
        AvatarItem("avatar_30", R.drawable.avatar_30, "Avatar 30: Male with Side Part", "Male"),
        AvatarItem("avatar_31", R.drawable.avatar_31, "Avatar 31: Male with Glasses", "Accessories"),
        AvatarItem("avatar_32", R.drawable.avatar_32, "Avatar 32: Female with Curly Hair and Bowtie", "Female"),
        AvatarItem("avatar_33", R.drawable.avatar_33, "Avatar 33: Male with Beard and Bowtie", "Male"),
        AvatarItem("avatar_34", R.drawable.avatar_34, "Avatar 34: Female with Hijab and Glasses", "Female"),
        AvatarItem("avatar_35", R.drawable.avatar_35, "Avatar 35: Male with Turban", "Male"),
        AvatarItem("avatar_36", R.drawable.avatar_36, "Avatar 36: Male with Beanie and Beard", "Accessories"),
        AvatarItem("avatar_37", R.drawable.avatar_37, "Avatar 37: Female with Top Bun and Bowtie", "Female"),
        AvatarItem("avatar_38", R.drawable.avatar_38, "Avatar 38: Male with Short Hair", "Male"),
        AvatarItem("avatar_39", R.drawable.avatar_39, "Avatar 39: Female with Glasses", "Accessories"),
        AvatarItem("avatar_40", R.drawable.avatar_40, "Avatar 40: Male with Afro and Beard", "Male"),
        AvatarItem("avatar_41", R.drawable.avatar_41, "Avatar 41: Male with Glasses and Beard", "Accessories"),
        AvatarItem("avatar_42", R.drawable.avatar_42, "Avatar 42: Female with Long Hair", "Female"),
        AvatarItem("avatar_43", R.drawable.avatar_43, "Avatar 43: Male with Side Part and Bowtie", "Male"),
        AvatarItem("avatar_44", R.drawable.avatar_44, "Avatar 44: Female with Hijab", "Female"),
        AvatarItem("avatar_45", R.drawable.avatar_45, "Avatar 45: Male with Turban and Glasses", "Male"),
        AvatarItem("avatar_46", R.drawable.avatar_46, "Avatar 46: Male with Beanie and Bowtie", "Accessories"),
        AvatarItem("avatar_47", R.drawable.avatar_47, "Avatar 47: Female with Glasses and Afro", "Female"),
        AvatarItem("avatar_48", R.drawable.avatar_48, "Avatar 48: Male with Beard", "Male"),
        AvatarItem("avatar_49", R.drawable.avatar_49, "Avatar 49: Female with Top Bun and Glasses", "Accessories"),
        AvatarItem("avatar_50", R.drawable.avatar_50, "Avatar 50: Male with Short Hair and Bowtie", "Male")
    )

    fun isAvatarUri(uriString: String?): Boolean {
        if (uriString.isNullOrEmpty()) return false
        return uriString.startsWith("avatar://") || 
               uriString.startsWith("avatar_") || 
               AVATARS.any { it.id == uriString }
    }

    fun getAvatarResId(uriString: String?): Int? {
        if (uriString.isNullOrEmpty()) return null
        val cleanId = when {
            uriString.startsWith("avatar://") -> uriString.removePrefix("avatar://")
            uriString.startsWith("android.resource://") -> uriString.substringAfterLast("/")
            else -> uriString
        }
        return AVATARS.firstOrNull { it.id == cleanId }?.resId
    }

    fun buildAvatarUri(avatarId: String): String {
        return "avatar://$avatarId"
    }
}
