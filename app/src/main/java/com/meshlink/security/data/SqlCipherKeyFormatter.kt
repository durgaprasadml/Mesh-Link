package com.meshlink.security.data

import java.util.Arrays

object SqlCipherKeyFormatter {

    /**
     * Converts raw key bytes to SQLCipher's hexadecimal representation `x'HEX_KEY'`.
     * Does NOT generate the SQL PRAGMA statement itself.
     * 
     * @param key The raw byte array key.
     * @return The formatted string (e.g. "x'0123456789ABCDEF...'")
     */
    fun formatHexKey(key: SecureDatabaseKey): String {
        val bytes = key.getBytes()
        require(bytes.isNotEmpty()) { "Key cannot be empty" }
        
        // SQLCipher requires deterministic formatting.
        // Create the string from bytes directly.
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        
        try {
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return "x'${String(hexChars)}'"
        } finally {
            // Memory cleanup for temporary CharArray
            Arrays.fill(hexChars, '\u0000')
        }
    }
}
