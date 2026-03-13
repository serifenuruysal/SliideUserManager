package com.sliide.usermanager.domain.usecase

import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class ObserveUsersUseCase(private val repo: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repo.observeUsers()
}

class SyncUsersUseCase(private val repo: UserRepository) {
    suspend operator fun invoke(): Result<Unit> = repo.syncUsers()
}

class AddUserUseCase(private val repo: UserRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        gender: String
    ): Result<User> = repo.addUser(name, email, gender)
}

class DeleteUserUseCase(private val repo: UserRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = repo.deleteUser(id)
}
