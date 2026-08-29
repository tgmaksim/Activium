package ru.tgmaksim.activium

import android.app.Application

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        SettingsManager.init(this)
        CacheManager.init(this)

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Utilities.log(throwable, "Coroutine Error at App.onCreate")
        }
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

        applicationScope.launch {
            MemoryDataManager.sessionId.value = SettingsManager.getSessionId()
            MemoryDataManager.darkTheme.value = SettingsManager.getDarkTheme()
            MemoryDataManager.themeInitialized = true
        }

        try {
            // Загрузка Firebase
            FirebaseApp.initializeApp(this@App)

            // В фоне запустится регистрация устройства и будет вызван MessagingService.onRegistered
            FirebaseMessaging.getInstance().register()
        } catch (e: Exception) {
            Utilities.log("Firebase Init Error: ${e.message}")
        }
    }
}