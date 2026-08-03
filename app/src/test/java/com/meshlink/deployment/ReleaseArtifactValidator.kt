package com.meshlink.deployment

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReleaseArtifactValidator {

    @Test
    fun `verify sha256 checksum calculation utility`() {
        val sampleData = "Mesh-Link Release Candidate 1.0.0".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sampleData).joinToString("") { "%02x".format(it) }

        assertNotNull("SHA-256 hash must not be null", hash)
        org.junit.Assert.assertEquals("SHA-256 length must be 64 hex characters", 64, hash.length)
    }

    /**
     * Inspects build outputs directory if present and calculates artifact hashes.
     */
    fun validateBuildOutputs(projectRootDir: File): Map<String, String> {
        val artifactHashes = mutableMapOf<String, String>()
        val apkDir = File(projectRootDir, "app/build/outputs/apk/release")
        val bundleDir = File(projectRootDir, "app/build/outputs/bundle/release")
        val mappingFile = File(projectRootDir, "app/build/outputs/mapping/release/mapping.txt")

        if (apkDir.exists()) {
            apkDir.listFiles()?.filter { it.extension == "apk" }?.forEach { file ->
                artifactHashes[file.name] = computeSha256(file)
            }
        }
        if (bundleDir.exists()) {
            bundleDir.listFiles()?.filter { it.extension == "aab" }?.forEach { file ->
                artifactHashes[file.name] = computeSha256(file)
            }
        }
        if (mappingFile.exists()) {
            artifactHashes["mapping.txt"] = computeSha256(mappingFile)
        }

        return artifactHashes
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
