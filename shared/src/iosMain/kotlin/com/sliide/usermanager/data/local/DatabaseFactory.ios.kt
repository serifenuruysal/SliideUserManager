package com.sliide.usermanager.data.local

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sliide.usermanager.data.local.db.UserDatabase

actual class DatabaseFactory actual constructor() {
    actual fun create(): UserDatabase {
        val driver = NativeSqliteDriver(UserDatabase.Schema, "user_database.db")
        return UserDatabase(driver)
    }
}
