package com.sliide.usermanager.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sliide.usermanager.data.local.db.UserDatabase
import com.sliide.usermanager.data.local.db.UserEntity
import com.sliide.usermanager.data.remote.GoRestApi
import com.sliide.usermanager.data.remote.dto.UserDto
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class UserRepositoryImpl(
    private val api: GoRestApi,
    db: UserDatabase,
    private val clock: Clock = Clock.System
) : UserRepository {

    private val queries = db.userDatabaseQueries

    override fun observeUsers(): Flow<List<User>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> 
                entities.map { it.toDomain() }
                    .sortedByDescending { it.createdAtEpochMs }
            }
            .flowOn(Dispatchers.Default)

    override suspend fun syncUsers(): Result<Unit> =
        api.getLastPageUsers().map { dtos ->
            val now = clock.now().toEpochMilliseconds()
            queries.transaction {
                dtos.forEach { dto ->
                    // Only sync users from the server that we haven't deleted locally
                    val isDeleted = queries.isDeleted(dto.id).executeAsOne()
                    if (!isDeleted) {
                        queries.insert(dto.id, dto.name, dto.email, dto.gender, dto.status, now)
                    }
                }
            }
        }

    override suspend fun addUser(name: String, email: String, gender: String): Result<User> =
        api.createUser(name, email, gender).map { dto ->
            val now = clock.now().toEpochMilliseconds()
            
            // Fix: Use current time as ID for local users to prevent ID collisions
            // DummyJSON returns the same static ID for all 'adds', causing overwrites.
            // Using 'now' ensures every locally added user is unique.
            val uniqueLocalId = now 
            
            queries.insert(uniqueLocalId, dto.name, dto.email, dto.gender, dto.status, now)
            dto.toDomain(now).copy(id = uniqueLocalId)
        }

    override suspend fun deleteUser(id: Long): Result<Unit> =
        api.deleteUser(id).also {
            queries.transaction {
                queries.deleteById(id)
                queries.insertDeletedId(id)
            }
        }

    private fun UserEntity.toDomain() =
        User(id, name, email, gender, status, createdAtEpochMs)

    private fun UserDto.toDomain(createdAt: Long) =
        User(id, name, email, gender, status, createdAt)
}
