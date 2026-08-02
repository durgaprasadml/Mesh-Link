package com.meshlink.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.media.models.TransferUi
import com.meshlink.ui.media.transfer.TransferHistoryList

/**
 * Chronological transfer history component for Media & Files.
 */
@Composable
fun TransferHistory(
    transfers: List<TransferUi>,
    onRetryClick: ((String) -> Unit)? = null,
    onCancelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TransferHistoryList(
        transfers = transfers,
        onRetryClick = onRetryClick,
        onCancelClick = onCancelClick,
        modifier = modifier
    )
}
