package com.sliide.usermanager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sliide.usermanager.data.local.DatabaseFactory
import com.sliide.usermanager.di.sharedModule
import com.sliide.usermanager.ui.UserListScreen
import com.sliide.usermanager.ui.theme.SliideTheme
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize static context for DatabaseFactory
        DatabaseFactory.appContext = applicationContext
        
        // 2. Safely start Koin
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(sharedModule())
            }
        }

        enableEdgeToEdge()

        setContent {
            KoinContext {
                SliideTheme {
                    UserListScreen()
                }
            }
        }
    }
}
