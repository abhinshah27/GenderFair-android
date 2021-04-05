package com.groops.fairsquare.websocket_connection.session

interface SessionObserver {
    fun onSessionUpdated(sessionState: SessionState, errorDescription: String?, serverRequest: Array<Any> = emptyArray())
}