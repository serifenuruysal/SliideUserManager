package com.sliide.usermanager.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sliide.usermanager.data.local.db.UserDatabase

actual class DatabaseFactory {
    companion object {
        lateinit var appContext: Context
    }
    actual fun create(): UserDatabase =
        UserDatabase(AndroidSqliteDriver(UserDatabase.Schema, appContext, "user_database.db"))
}
