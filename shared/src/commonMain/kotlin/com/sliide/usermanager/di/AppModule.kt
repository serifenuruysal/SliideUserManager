package com.sliide.usermanager.di

import com.sliide.usermanager.data.local.DatabaseFactory
import com.sliide.usermanager.data.local.db.UserDatabase
import com.sliide.usermanager.data.remote.GoRestApi
import com.sliide.usermanager.data.repository.UserRepositoryImpl
import com.sliide.usermanager.domain.repository.UserRepository
import com.sliide.usermanager.domain.usecase.AddUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.SyncUsersUseCase
import com.sliide.usermanager.presentation.UserListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.koin.dsl.module

fun sharedModule() = module {

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Android; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0")
                header(HttpHeaders.Accept, "application/json")
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 20_000L
                socketTimeoutMillis  = 20_000L
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                exponentialDelay()
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message")
                    }
                }
                level = LogLevel.INFO 
            }
        }
    }

    single { GoRestApi(client = get()) }

    single { DatabaseFactory() }
    
    single<UserDatabase> { get<DatabaseFactory>().create() }

    single<Clock> { Clock.System }

    single<UserRepository> { 
        UserRepositoryImpl(
            api = get(), 
            db = get(),
            clock = get()
        ) 
    }

    factory { ObserveUsersUseCase(repo = get()) }
    factory { SyncUsersUseCase(repo = get()) }
    factory { AddUserUseCase(repo = get()) }
    factory { DeleteUserUseCase(repo = get()) }

    // Manually define the factory to avoid reflection on iOS
    factory { 
        UserListViewModel(
            observeUsers = get(), 
            syncUsers = get(), 
            addUser = get(), 
            deleteUser = get()
        ) 
    }
}
