package com.laotoua.dawnislandk.data.local.dao

import androidx.room.*
import com.laotoua.dawnislandk.data.local.Cookie


@Dao
interface CookieDao {
    @Query("SELECT * FROM Cookie")
    suspend fun getAll(): List<Cookie>

    @Query("SELECT * FROM Cookie WHERE cookieHash=:cookieHash")
    suspend fun getCookieByUserHash(cookieHash: String): Cookie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cookie: Cookie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cookieList: List<Cookie>)

    @Transaction
    suspend fun resetCookies(cookieList: List<Cookie>) {
        nukeTable()
        insertAll(cookieList)
    }

    @Update
    suspend fun updateCookie(cookie: Cookie)

    @Delete
    suspend fun delete(cookie: Cookie)

    @Query("DELETE FROM Cookie")
    fun nukeTable()
}