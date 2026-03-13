package com.sliide.usermanager.data.remote

import com.sliide.usermanager.data.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Interfaces with dummyjson.com.
 * Maps DummyJSON data to the app's existing UserDto structure.
 */
class GoRestApi(
    private val client: HttpClient
) {
    companion object {
        private const val BASE = "https://dummyjson.com"
        private const val PAGE_SIZE = 20
    }

    /**
     * Fetches users from the literal last page of the /users endpoint.
     *
     * To find the "last page," we first probe the API to get the total count of users,
     * then calculate the correct 'skip' value to fetch only the final slice of data.
     */
    suspend fun getLastPageUsers(): Result<List<UserDto>> = runCatching {
        // 1. Probe the API with limit=1 to efficiently retrieve the 'total' count.
        val probe: DummyJsonUsersResponse = client.get("$BASE/users") {
            parameter("limit", 1)
        }.body()

        val total = probe.total
        if (total <= 0) return@runCatching emptyList()

        // 2. Calculate the skip value for the literal last page.
        // Example: If total is 208 and PAGE_SIZE is 20, the last page starts at index 200.
        val skip = ((total - 1) / PAGE_SIZE) * PAGE_SIZE

        // 3. Fetch the users belonging to that final page.
        val response: DummyJsonUsersResponse = client.get("$BASE/users") {
            parameter("limit", PAGE_SIZE)
            parameter("skip", skip)
        }.body()

        response.users.map { it.toUserDto() }
    }

    /**
     * Simulates creating a user on DummyJSON.
     * Maps the single 'name' field to 'firstName' and 'lastName' required by the API.
     */
    suspend fun createUser(name: String, email: String, gender: String): Result<UserDto> =
        runCatching {
            val names = name.split(" ")
            val firstName = names.getOrNull(0) ?: name
            val lastName = names.getOrNull(1) ?: ""

            val response: HttpResponse = client.post("$BASE/users/add") {
                contentType(ContentType.Application.Json)
                setBody(DummyJsonAddUserRequest(firstName, lastName, email, gender))
            }
            check(response.status.isSuccess()) { "Create user failed: ${response.status}" }
            
            // DummyJSON returns the new user object with a unique ID
            response.body<DummyJsonUser>().toUserDto()
        }

    /**
     * Simulates deleting a user on DummyJSON.
     */
    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val response: HttpResponse = client.delete("$BASE/users/$id")
        check(response.status.isSuccess()) { "Delete user $id failed: ${response.status}" }
        
        val deleteResult = response.body<DummyJsonDeleteResponse>()
        check(deleteResult.isDeleted) { "Delete failed: isDeleted was false" }
    }
}

// Internal DTOs for DummyJSON schema
@Serializable
private data class DummyJsonUsersResponse(
    val users: List<DummyJsonUser>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

@Serializable
private data class DummyJsonUser(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val gender: String
) {
    fun toUserDto() = UserDto(
        id = id,
        name = "$firstName $lastName".trim(),
        email = email,
        gender = gender,
        status = "active"
    )
}

@Serializable
private data class DummyJsonAddUserRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val gender: String
)

@Serializable
private data class DummyJsonDeleteResponse(
    val id: Long,
    val isDeleted: Boolean,
    val deletedOn: String? = null
)
