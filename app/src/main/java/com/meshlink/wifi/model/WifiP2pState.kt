package com.meshlink.wifi.model

sealed interface WifiP2pState {
    object Disabled : WifiP2pState
    object Unavailable : WifiP2pState
    object Enabled : WifiP2pState
    object Discovering : WifiP2pState
    data class Connecting(val deviceAddress: String) : WifiP2pState
    data class Connected(val groupOwnerAddress: String, val isGroupOwner: Boolean) : WifiP2pState
    object Disconnected : WifiP2pState
    data class Error(val message: String) : WifiP2pState
    object Recovering : WifiP2pState
}
