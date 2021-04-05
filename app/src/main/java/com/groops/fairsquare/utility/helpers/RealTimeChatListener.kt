/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.utility.helpers

import com.groops.fairsquare.models.chat.ChatMessage

/**
 * @author mbaldrighi on 10/29/2018.
 */
interface RealTimeChatListener {

    fun onNewMessage(newMessage: ChatMessage)
    fun onStatusUpdated(userId: String, status: Int, date: String)
    fun onActivityUpdated(userId: String, chatId: String, activity: String)
    fun onMessageDelivered(chatId: String, userId: String, date: String)
    fun onMessageRead(chatId: String, userId: String, date: String)
    fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String)

}