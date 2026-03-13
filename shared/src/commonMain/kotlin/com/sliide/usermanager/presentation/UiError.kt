package com.sliide.usermanager.presentation

import io.ktor.client.plugins.ResponseException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.utils.io.errors.IOException

sealed interface UiError {
    data object NetworkUnavailable : UiError
    data object Timeout : UiError
    data object ServerError : UiError
    data class ApiError(val code: Int, val rawMessage: String? = null) : UiError
    data class ValidationFailed(val detail: String) : UiError
    data class Unknown(val message: String) : UiError
}

fun UiError.toDisplayMessage(): String = when (this) {
    UiError.NetworkUnavailable ->
        "Network unreachable. Check your internet connection."
    UiError.Timeout ->
        "Connection timed out. Check your GoRest Token in gradle.properties."
    UiError.ServerError ->
        "Server error (5xx). Please try again later."
    is UiError.ApiError ->
        "Request failed ($code). Check your API token."
    is UiError.ValidationFailed ->
        "Validation failed: $detail"
    is UiError.Unknown ->
        message
}

fun Throwable.toUiError(): UiError = when (this) {
    is HttpRequestTimeoutException, is SocketTimeoutException -> UiError.Timeout
    is ResponseException -> when (response.status.value) {
        401, 403 -> UiError.ApiError(response.status.value, "Unauthorized")
        422 -> UiError.ValidationFailed(message ?: "Validation failed")
        in 500..599 -> UiError.ServerError
        else -> UiError.ApiError(response.status.value, message)
    }
    is IOException -> UiError.NetworkUnavailable
    // Handle the "check(status.isSuccess())" case from GoRestApi.kt
    is IllegalStateException -> {
        val msg = message ?: ""
        when {
            msg.contains("500") || msg.contains("503") -> UiError.ServerError
            msg.contains("401") || msg.contains("403") -> UiError.ApiError(401, "Unauthorized")
            else -> UiError.Unknown(msg)
        }
    }
    else -> UiError.Unknown(this.toString())
}
