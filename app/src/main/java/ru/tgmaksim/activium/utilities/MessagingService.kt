package ru.tgmaksim.activium.utilities

import com.google.firebase.messaging.FirebaseMessagingService

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: отправить новый firebaseToken на сервер
    }
}