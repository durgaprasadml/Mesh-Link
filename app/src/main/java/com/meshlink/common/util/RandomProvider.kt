package com.meshlink.common.util

import java.security.SecureRandom

interface RandomProvider {
    fun nextBytes(bytes: ByteArray)
    fun nextInt(): Int
    fun nextInt(bound: Int): Int
}

class SecureRandomProvider : RandomProvider {
    private val secureRandom = SecureRandom()
    
    override fun nextBytes(bytes: ByteArray) {
        secureRandom.nextBytes(bytes)
    }

    override fun nextInt(): Int = secureRandom.nextInt()
    
    override fun nextInt(bound: Int): Int = secureRandom.nextInt(bound)
}
