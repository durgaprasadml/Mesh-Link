package com.meshlink.ui.components.chat

import androidx.compose.runtime.Immutable
import com.meshlink.domain.model.DeliveryStatus

@Immutable
sealed interface DeliveryUiState {
    object Sending : DeliveryUiState
    object Sent : DeliveryUiState
    object Delivered : DeliveryUiState
    object Seen : DeliveryUiState
    object Failed : DeliveryUiState

    companion object {
        fun fromDomain(status: DeliveryStatus): DeliveryUiState {
            return when (status) {
                DeliveryStatus.PENDING,
                DeliveryStatus.QUEUED,
                DeliveryStatus.SENDING,
                DeliveryStatus.WAITING_FOR_ROUTE,
                DeliveryStatus.WAITING_FOR_ACK,
                DeliveryStatus.RETRYING -> Sending

                DeliveryStatus.SENT -> Sent

                DeliveryStatus.DELIVERED,
                DeliveryStatus.RELAYED -> Delivered

                DeliveryStatus.SEEN -> Seen

                DeliveryStatus.FAILED,
                DeliveryStatus.PERMANENT_FAILURE,
                DeliveryStatus.CANCELLED,
                DeliveryStatus.EXPIRED -> Failed
            }
        }
    }
}
