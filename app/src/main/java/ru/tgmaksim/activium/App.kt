package ru.tgmaksim.activium

import android.app.Application

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        CacheManager.init(this)
        SettingsManager.init(this)

        // Загрузка Firebase
        FirebaseApp.initializeApp(this)

        // Проверка изменений firebaseToken
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful)
                return@addOnCompleteListener

            applicationScope.launch {
                val firebaseMessagingToken = task.result

                if (SettingsManager.getFirebaseMessagingToken() != firebaseMessagingToken) {
                    SettingsManager.setFirebaseMessagingToken(firebaseMessagingToken)

                    if (SettingsManager.getSessionId() != null)
                        Settings.updateFirebase(firebaseMessagingToken)
                }
            }
        }
    }
}
