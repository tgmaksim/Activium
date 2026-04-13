package ru.tgmaksim.activium

import android.app.Application

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        SettingsManager.init(this)
        CacheManager.init(this)

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        applicationScope.launch {
            MemoryDataManager.sessionId.value = SettingsManager.getSessionId()
            MemoryDataManager.darkTheme.value = SettingsManager.getDarkTheme()
            MemoryDataManager.themeInitialized = true
        }

        // Загрузка Firebase
        applicationScope.launch {
            FirebaseApp.initializeApp(this@App)

            // Проверка изменений firebaseToken
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful || task.result == null) {
                    Utilities.log("FirebaseMessaging: isSuccessful = ${task.isSuccessful}, result = ${if (task.isSuccessful) task.result else "null"}")
                    return@addOnCompleteListener
                }

                val firebaseMessagingToken = task.result

                applicationScope.launch {
                    SettingsManager.setFirebaseMessagingToken(firebaseMessagingToken)

                    if (SettingsManager.getSessionId() != null)
                        Settings.updateFirebase(firebaseMessagingToken)
                }
            }
        }
    }
}