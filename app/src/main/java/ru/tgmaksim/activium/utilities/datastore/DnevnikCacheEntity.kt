package ru.tgmaksim.activium.utilities.datastore

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(
    tableName = "dnevnik_cache",
    primaryKeys = ["child_id", "name", "param"]
)
data class DnevnikCacheEntity(
    @ColumnInfo("child_id") val childId: Long,
    val name: String,
    val param: String,
    val value: String,
    @ColumnInfo("updated_at") val updatedAt: Long
)
