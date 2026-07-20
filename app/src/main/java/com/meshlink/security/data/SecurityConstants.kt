package com.meshlink.security.data



/**
 * Centralized cryptographic constants for Mesh Link.
 * Follows Android production security best practices.
 */
object SecurityConstants {
    // ────────── KeyStore & Algorithms ──────────
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
    const val ECDH_ALGORITHM = "ECDH"
    const val EC_ALGORITHM = "EC"
    const val SHA_256_ALGORITHM = "SHA-256"
    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    
    // ────────── Sizes & Parameters ──────────
    const val AES_KEY_SIZE_BITS = 256
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128
    const val SEED_LENGTH_BYTES = 32
    const val SALT_LENGTH_BYTES = 16
    
    // ────────── PBKDF2 Parameters ──────────
    const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val PBKDF2_ITERATIONS = 100_000
    const val PBKDF2_KEY_LENGTH_BITS = 256
    
    // ────────── Database Master Keys ──────────
    const val DB_MASTER_KEY_ALIAS = "mesh_db_master_key"
    const val DB_PREFS_NAME_LEGACY = "mesh_db_config"
    const val DB_PREFS_NAME_ENC = "mesh_db_config_enc"
    
    const val KEY_LEGACY_PASSPHRASE = "db_passphrase"
    const val KEY_ENCRYPTED_SEED = "db_encrypted_seed"
    const val KEY_SALT = "db_salt"
    const val KEY_MIGRATION_STATE = "db_migration_state"
    const val STATE_NOT_STARTED = "NOT_STARTED"
    const val STATE_IN_PROGRESS = "IN_PROGRESS"
    const val STATE_VERIFIED = "VERIFIED"
    const val STATE_FAILED = "FAILED"
    
    const val DB_NAME = "mesh_db"
    
    // ────────── Mesh Crypto Keys ──────────
    const val MESH_KEYSTORE_ALIAS = "mesh_link_ecdh_key"
    const val SIGNING_KEYSTORE_ALIAS = "mesh_link_signing_key"
    const val PEER_KEYS_PREF = "mesh_peer_keys"
    const val PEER_SIGNING_KEYS_PREF = "mesh_peer_signing_keys"
    const val SELF_PRIVATE_KEY_KEY = "__self_private_key__"
    const val SELF_PUBLIC_KEY_KEY = "__self_public_key__"
    const val SELF_SIGNING_PRIVATE_KEY_KEY = "__self_signing_private_key__"
    const val SELF_SIGNING_PUBLIC_KEY_KEY = "__self_signing_public_key__"
    const val KEY_CREATION_TIME = "__key_creation_time__"
    const val LAST_ROTATION_TIME = "__last_rotation_time__"
}
