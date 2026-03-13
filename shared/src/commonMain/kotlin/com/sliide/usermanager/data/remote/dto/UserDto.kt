package com.sliide.usermanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: String,
    val status: String = "active"
)

/** Maps DTO → GoRest field names expected by the API. */
@Serializable
data class GoRestError(
    val field: String,
    val message: String
)
