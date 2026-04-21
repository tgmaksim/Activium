package ru.tgmaksim.activium

import android.app.Application

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler

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

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Utilities.log("Coroutine Error: ${throwable.message}")
        }
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

        applicationScope.launch {
            MemoryDataManager.sessionId.value = SettingsManager.getSessionId()
            MemoryDataManager.darkTheme.value = SettingsManager.getDarkTheme()
            MemoryDataManager.themeInitialized = true
        }

        // Загрузка Firebase
        applicationScope.launch {
            try {
                FirebaseApp.initializeApp(this@App)

                val gmsAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(this@App) == com.google.android.gms.common.ConnectionResult.SUCCESS

                if (gmsAvailable) {
                    fetchFirebaseToken(applicationScope)
                } else {
                    Utilities.log("Firebase: Google Play Services unavailable")
                }
            } catch (e: Exception) {
                Utilities.log("Firebase Init Error: ${e.message}")
            }
        }
    }

    private fun fetchFirebaseToken(scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Utilities.log(task.exception ?: Exception("Token task failed"))
                return@addOnCompleteListener
            }

            val token = task.result
            if (token == null) {
                Utilities.log("FirebaseMessaging: null result")
                return@addOnCompleteListener
            }

            scope.launch {
                SettingsManager.setFirebaseMessagingToken(token)

                Settings.updateFirebase(token)
            }
        }
    }
}