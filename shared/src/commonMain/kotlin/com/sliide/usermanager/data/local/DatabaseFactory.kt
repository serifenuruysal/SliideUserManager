package com.sliide.usermanager.data.local

import com.sliide.usermanager.data.local.db.UserDatabase

expect class DatabaseFactory() {
    fun create(): UserDatabase
}
