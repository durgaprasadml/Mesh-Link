package com.meshlink.domain.mapper

import com.meshlink.database.data.local.MessageEntity
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType

fun MessageEntity.toDomain(): Message {
    return Message(
        messageId = this.messageId,
        chatId = this.chatId,
        text = this.text,
        senderId = this.senderId,
        timestamp = this.timestamp,
        isFromMe = this.isFromMe,
        status = DeliveryStatus.valueOf(this.status.name),
        messageType = MessageType.valueOf(this.messageType.name),
        mediaPath = this.mediaPath,
        mediaDurationMs = this.mediaDurationMs,
        latitude = this.latitude,
        longitude = this.longitude,
        batteryPercent = this.batteryPercent
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        messageId = this.messageId,
        chatId = this.chatId,
        text = this.text,
        senderId = this.senderId,
        timestamp = this.timestamp,
        isFromMe = this.isFromMe,
        status = com.meshlink.database.data.local.DeliveryStatus.valueOf(this.status.name),
        messageType = com.meshlink.database.data.local.MessageType.valueOf(this.messageType.name),
        mediaPath = this.mediaPath,
        mediaDurationMs = this.mediaDurationMs,
        latitude = this.latitude,
        longitude = this.longitude,
        batteryPercent = this.batteryPercent
    )
}
