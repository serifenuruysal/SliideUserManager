package com.sliide.usermanager

import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.usermanager.di.sharedModule
import com.sliide.usermanager.ui.UserListScreen
import com.sliide.usermanager.ui.theme.SliideTheme
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

/**
 * Ensures Koin is only started once on the main thread.
 */
private fun initKoin() {
    if (!koinInitialized) {
        startKoin {
            modules(sharedModule())
        }
        koinInitialized = true
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        // Disable the performance sanity check to bypass the Info.plist requirement for now
        enforceStrictPlistSanityCheck = false
    }
) {
    initKoin()

    KoinContext {
        SliideTheme {
            UserListScreen()
        }
    }
}
