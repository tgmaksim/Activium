package ru.tgmaksim.activium.utilities

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope

import com.google.firebase.messaging.FirebaseMessagingService

import ru.tgmaksim.activium.api.Settings

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        applicationScope.launch {
            Settings.updateFirebase(token)
        }
    }
}