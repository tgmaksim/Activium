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
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        SettingsManager.init(this)

        applicationScope.launch {
            MemoryDataManager.sessionId.value = SettingsManager.getSessionId()
            MemoryDataManager.darkTheme.value = SettingsManager.getDarkTheme()
        }

        CacheManager.init(this)

        // Загрузка Firebase
        FirebaseApp.initializeApp(this)

        // Проверка изменений firebaseToken
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful)
                return@addOnCompleteListener

            applicationScope.launch {
                val firebaseMessagingToken = task.result

                if (SettingsManager.getFirebaseMessagingToken() != firebaseMessagingToken) {
                    SettingsManager.setFirebaseMessagingToken(firebaseMessagingToken)

                    if (MemoryDataManager.sessionId.value != null)
                        Settings.updateFirebase(firebaseMessagingToken)
                }
            }
        }
    }
}
