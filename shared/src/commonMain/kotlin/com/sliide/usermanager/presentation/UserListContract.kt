package com.sliide.usermanager.presentation

import androidx.compose.runtime.Immutable
import com.sliide.usermanager.domain.model.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// ---------------------------------------------------------------------------
// State
//
// Fix #1  — pendingDelete removed from the public contract. It was an internal
//           optimistic-delete mechanism, not a renderable UI fact. It now lives
//           as a private field in UserListViewModel.
//
// Fix #2  — errorMessage: String? → error: UiError? so the UI layer converts
//           a typed enum to display copy rather than echoing raw exception text.
//
// Fix #17 — @Immutable + ImmutableList<User> tells Compose the state object
//           and its list are stable, eliminating spurious full-screen recompositions.
// ---------------------------------------------------------------------------

@Immutable
data class UserListState(
    val users: ImmutableList<User> = persistentListOf(),
    val isShimmerLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isAddingUser: Boolean = false,
    val showAddUserSheet: Boolean = false,
    val error: UiError? = null
)

sealed interface UserListIntent {
    data object Initialise   : UserListIntent
    data object Refresh      : UserListIntent
    data object ShowAddSheet : UserListIntent
    data object HideAddSheet : UserListIntent
    data class SubmitNewUser(
        val name: String,
        val email: String,
        val gender: String
    ) : UserListIntent
    data class RequestDelete(val user: User) : UserListIntent
    data object UndoDelete   : UserListIntent
    data object DismissError : UserListIntent
}

sealed interface UserListEffect {
    data class Toast(val message: String)           : UserListEffect
    data class DeleteSnackbar(val userName: String) : UserListEffect
    data object ScrollToTop                         : UserListEffect
}
