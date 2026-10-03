package com.meshlink.transfer

import com.meshlink.wifi.data.WifiSocketTransport
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class AudioTransferPipelineTest {

    @Test
    fun `test audio transfer states are defined and non-terminal during transfer`() {
        val states = listOf(
            TransferState.QUEUED,
            TransferState.WAITING_FOR_WIFI,
            TransferState.SOCKET_CONNECTING,
            TransferState.HANDSHAKING,
            TransferState.READY,
            TransferState.TRANSFERRING,
            TransferState.WAITING_FOR_ACK,
            TransferState.COMPLETING,
            TransferState.RETRYING
        )

        for (state in states) {
            assertTrue("State $state should be active", state.isActive())
            assertFalse("State $state should not be terminal", state.isTerminal())
        }

        assertTrue(TransferState.COMPLETED.isTerminal())
        assertTrue(TransferState.FAILED.isTerminal())
        assertTrue(TransferState.CANCELLED.isTerminal())
    }

    @Test
    fun `test binary protocol magic constants alignment`() {
        assertEquals(-0x5348414B, WifiSocketTransport.MAGIC_STREAM_HANDSHAKE)
        assertEquals(-0x5244595F, WifiSocketTransport.MAGIC_STREAM_READY)
        assertEquals(-0x41434B5F, WifiSocketTransport.MAGIC_STREAM_ACK)
        assertEquals(-0x5253554D, WifiSocketTransport.MAGIC_STREAM_RESUME_REQ)
        assertEquals(-0x4155444F, WifiSocketTransport.MAGIC_AUDIO_CHUNK)
        assertEquals(-0x454E445F, WifiSocketTransport.MAGIC_MEDIA_STREAM_END)
        assertEquals(16 * 1024, WifiSocketTransport.AUDIO_CHUNK_SIZE)
    }

    @Test
    fun `test audio handshake framing protocol serialization and deserialization`() {
        val transferId = "voice_msg_123"
        val fileName = "voice_test.m4a"
        val mimeType = "audio/mp4"
        val totalBytes = 24576L
        val expectedChecksum = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
        val senderId = "NODE-sender-001"

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Sender writes handshake
        dos.writeInt(WifiSocketTransport.MAGIC_STREAM_HANDSHAKE)
        dos.writeUTF(transferId)
        dos.writeUTF(fileName)
        dos.writeUTF(mimeType)
        dos.writeLong(totalBytes)
        dos.writeUTF(expectedChecksum)
        dos.writeUTF(senderId)
        dos.flush()

        // Receiver reads handshake
        val bais = ByteArrayInputStream(baos.toByteArray())
        val dis = DataInputStream(bais)

        val magic = dis.readInt()
        assertEquals(WifiSocketTransport.MAGIC_STREAM_HANDSHAKE, magic)
        assertEquals(transferId, dis.readUTF())
        assertEquals(fileName, dis.readUTF())
        assertEquals(mimeType, dis.readUTF())
        assertEquals(totalBytes, dis.readLong())
        assertEquals(expectedChecksum, dis.readUTF())
        assertEquals(senderId, dis.readUTF())
    }

    @Test
    fun `test audio chunk framing with resume offset`() {
        val transferId = "voice_resume_456"
        val chunkIndex = 2
        val totalChunks = 5
        val offset = 32768L
        val fakeEncryptedPayload = ByteArray(1024) { 0x42 }

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Sender writes chunk
        dos.writeInt(WifiSocketTransport.MAGIC_AUDIO_CHUNK)
        dos.writeUTF(transferId)
        dos.writeInt(chunkIndex)
        dos.writeInt(totalChunks)
        dos.writeLong(offset)
        dos.writeInt(fakeEncryptedPayload.size)
        dos.write(fakeEncryptedPayload)
        dos.flush()

        // Receiver reads chunk
        val bais = ByteArrayInputStream(baos.toByteArray())
        val dis = DataInputStream(bais)

        val magic = dis.readInt()
        assertEquals(WifiSocketTransport.MAGIC_AUDIO_CHUNK, magic)
        assertEquals(transferId, dis.readUTF())
        assertEquals(chunkIndex, dis.readInt())
        assertEquals(totalChunks, dis.readInt())
        assertEquals(offset, dis.readLong())
        val payloadLen = dis.readInt()
        assertEquals(fakeEncryptedPayload.size, payloadLen)
        val readPayload = ByteArray(payloadLen)
        dis.readFully(readPayload)
        assertArrayEquals(fakeEncryptedPayload, readPayload)
    }

    @Test
    fun `test audio resume response framing`() {
        val transferId = "voice_resume_789"
        val resumeOffset = 16384L

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeInt(WifiSocketTransport.MAGIC_STREAM_RESUME_REQ)
        dos.writeUTF(transferId)
        dos.writeLong(resumeOffset)
        dos.flush()

        val bais = ByteArrayInputStream(baos.toByteArray())
        val dis = DataInputStream(bais)

        val magic = dis.readInt()
        assertEquals(WifiSocketTransport.MAGIC_STREAM_RESUME_REQ, magic)
        assertEquals(transferId, dis.readUTF())
        assertEquals(resumeOffset, dis.readLong())
    }

    @Test
    fun `test audio ack framing`() {
        val transferId = "voice_ack_001"

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeInt(WifiSocketTransport.MAGIC_STREAM_ACK)
        dos.writeUTF(transferId)
        dos.flush()

        val bais = ByteArrayInputStream(baos.toByteArray())
        val dis = DataInputStream(bais)

        val magic = dis.readInt()
        assertEquals(WifiSocketTransport.MAGIC_STREAM_ACK, magic)
        assertEquals(transferId, dis.readUTF())
    }
}
