# State Restoration Guide

## Overview
This guide details the implementation of `StateRestorationManager`, responsible for persisting and recovering dynamic user state in Mesh Link across process deaths and configuration changes.

## Components Saved
- `lastScreen`: Route navigation string
- `activeChatId`: Current open chat room
- `pendingMessageDraft`: Unsent message in the text field
- `pendingUploadIds`: Transitive IDs for file uploads
- `pendingDownloadIds`: Transitive IDs for file downloads
- `meshStatus`: Local status for quick UI resumption

## Recovery Strategy
The state is persisted using Android `DataStore` (Preferences). 
On startup, `CrashRecoveryManager` intercepts the `stateFlow` to optionally restart services, while the UI layer can collect the state and navigate appropriately without prompting the user to manually restore their previous session.
