package ru.tgmaksim.activium.utilities.datastore

import androidx.room.Room
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheManager {
    private const val DATABASE_NAME = "cache.db"
    private lateinit var appContext: Context
    @Volatile
    private var database: CacheDatabase? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun db(): CacheDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                appContext,
                CacheDatabase::class.java,
                File(appContext.cacheDir, DATABASE_NAME).absolutePath
            ).build().also { database = it }
        }
    }

    suspend fun writeDnevnikCache(
        childId: Long,
        name: String,
        param: String = "",
        value: String
    ) {
        withContext(Dispatchers.IO) {
            db().cacheDao().write(
                DnevnikCacheEntity(
                    childId = childId,
                    name = name,
                    param = param,
                    value = value,
                    updatedAt = System.currentTimeMillis() / 1000L
                )
            )
        }
    }

    suspend fun read(
        childId: Long,
        name: String,
        param: String = ""
    ): DnevnikCacheEntity? {
        return withContext(Dispatchers.IO) {
            db().cacheDao().read(
                childId = childId,
                name = name,
                param = param
            )
        }
    }

    fun clear() {
        db().clearAllTables()
    }
}
