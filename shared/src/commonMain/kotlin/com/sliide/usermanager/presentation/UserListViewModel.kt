package com.sliide.usermanager.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.usecase.AddUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.SyncUsersUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val UNDO_WINDOW_MS = 4_000L

class UserListViewModel(
    private val observeUsers: ObserveUsersUseCase,
    private val syncUsers: SyncUsersUseCase,
    private val addUser: AddUserUseCase,
    private val deleteUser: DeleteUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<UserListEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<UserListEffect> = _effects.asSharedFlow()

    // reactive tracking of user pending deletion
    private val _pendingDeleteUser = MutableStateFlow<User?>(null)
    private var deleteJob: Job? = null

    init {
        onIntent(UserListIntent.Initialise)
    }

    fun onIntent(intent: UserListIntent) {
        when (intent) {
            UserListIntent.Initialise   -> initialise()
            UserListIntent.Refresh      -> refresh()
            UserListIntent.ShowAddSheet -> _state.update { it.copy(showAddUserSheet = true) }
            UserListIntent.HideAddSheet -> _state.update { it.copy(showAddUserSheet = false) }
            is UserListIntent.SubmitNewUser -> submitNewUser(intent.name, intent.email, intent.gender)
            is UserListIntent.RequestDelete -> requestDelete(intent.user)
            UserListIntent.UndoDelete   -> undoDelete()
            UserListIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun initialise() {
        observeLocalCache()
        refresh()
    }

    private fun observeLocalCache() {
        // Combine database emissions with the current pending-delete state
        // to reactively filter the list.
        combine(
            observeUsers(),
            _pendingDeleteUser
        ) { users, pending ->
            val filtered = if (pending != null) {
                users.filter { it.id != pending.id }
            } else {
                users
            }
            filtered.toImmutableList()
        }
        .onEach { filteredUsers ->
            _state.update { it.copy(users = filteredUsers, isShimmerLoading = false) }
        }
        .launchIn(viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            syncUsers()
                .onFailure { e ->
                    _state.update { it.copy(error = e.toUiError()) }
                }
            _state.update { it.copy(isSyncing = false) }
        }
    }

    private fun submitNewUser(name: String, email: String, gender: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAddingUser = true) }
            addUser(name, email, gender)
                .onSuccess {
                    _state.update { it.copy(showAddUserSheet = false, isAddingUser = false) }
                    _effects.emit(UserListEffect.ScrollToTop)
                    _effects.emit(UserListEffect.Toast("User added successfully"))
                }
                .onFailure { e ->
                    _state.update { it.copy(isAddingUser = false, error = e.toUiError()) }
                }
        }
    }

    private fun requestDelete(user: User) {
        deleteJob?.cancel()
        _pendingDeleteUser.value = user

        viewModelScope.launch {
            _effects.emit(UserListEffect.DeleteSnackbar(user.name))
        }

        deleteJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            commitDelete()
        }
    }

    private fun undoDelete() {
        deleteJob?.cancel()
        _pendingDeleteUser.value = null
    }

    private suspend fun commitDelete() {
        val user = _pendingDeleteUser.value ?: return
        
        // Clear pending immediately to stop filtering before the network call
        // If it fails, the error handler handles restoring consistency.
        _pendingDeleteUser.value = null

        deleteUser(user.id).onFailure { e ->
            _state.update { it.copy(error = e.toUiError()) }
            // Re-sync to restore local state from DB after remote failure
            refresh()
        }
    }
}
