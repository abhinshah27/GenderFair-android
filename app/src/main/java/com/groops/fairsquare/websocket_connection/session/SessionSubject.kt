package com.groops.fairsquare.websocket_connection.session

object SessionSubject {
    private val observers = ArrayList<SessionObserver>()
    private lateinit var sessionState: SessionState
    private lateinit var serverRequest: Array<Any>

    private var errorDescription: String? = null

    fun setSessionState(sessionState: SessionState, errorDescription: String?, serverRequest: Array<Any>) {
        this.sessionState = sessionState
        this.errorDescription = errorDescription
        this.serverRequest = serverRequest
        notifyAllObservers()
    }

    fun attach(observer: SessionObserver) {
        observers.add(observer)
    }

    private fun notifyAllObservers() {
        for (observer in observers) {
            observer.onSessionUpdated(sessionState, errorDescription, serverRequest)
        }
    }
}