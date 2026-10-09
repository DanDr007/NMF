package com.example.nmf.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSiteDao {

    @Query("SELECT * FROM blocked_sites ORDER BY dateAdded DESC")
    fun getAllSites(): Flow<List<BlockedSite>>

    @Query("SELECT domain FROM blocked_sites")
    suspend fun getAllDomainsSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: BlockedSite)

    @Delete
    suspend fun deleteSite(site: BlockedSite)

    @Query("DELETE FROM blocked_sites WHERE id = :id")
    suspend fun deleteById(id: Long)
}