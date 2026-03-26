package ru.tgmaksim.activium.utilities.datastore

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DnevnikCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): DnevnikCacheDao
}
