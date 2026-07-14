package com.meshlink.util

import android.net.Uri

class FakeMediaTransferManager {
    fun sendMedia(uri: Uri, targetPeerId: String) {}
    fun cancelTransfer(transferId: String) {}
}
