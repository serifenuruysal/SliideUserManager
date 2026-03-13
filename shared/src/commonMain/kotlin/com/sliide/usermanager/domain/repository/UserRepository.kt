package com.sliide.usermanager.domain.repository

import com.sliide.usermanager.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Emits cached list whenever the DB changes. */
    fun observeUsers(): Flow<List<User>>

    /** Fetches the last page of remote users and upserts into cache. */
    suspend fun syncUsers(): Result<Unit>

    /** POSTs a new user and inserts into cache with local timestamp. */
    suspend fun addUser(name: String, email: String, gender: String): Result<User>

    /** DELETEs remotely and removes from cache. */
    suspend fun deleteUser(id: Long): Result<Unit>
}
