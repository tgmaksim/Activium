package ru.tgmaksim.activium.utilities.datastore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface DnevnikCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(item: DnevnikCacheEntity)

    @Query(
        """
        SELECT * FROM dnevnik_cache
        WHERE child_id = :childId
          AND name = :name
          AND param IS :param
        LIMIT 1
        """
    )
    suspend fun read(
        childId: Long,
        name: String,
        param: String?
    ): DnevnikCacheEntity?
}
