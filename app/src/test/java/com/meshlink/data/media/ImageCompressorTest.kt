package com.meshlink.media.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import android.util.Log

class ImageCompressorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test invalid URI returns null`() {
        val uri = mockk<Uri>()
        // Given that openInputStream will return null (relaxed mock),
        // BitmapFactory.decodeStream won't be able to decode bounds.
        // It should return null without crashing.
        
        val result = ImageCompressor.compress(context, uri)
        assertNull(result)
    }

    // Creating actual Bitmap instances in unit tests without Robolectric 
    // will throw "Method X not mocked". We can mock BitmapFactory.decodeStream 
    // but the compression logic uses Bitmap instance methods.
    // Given the constraints, we verify the error handling path.
}
