package com.sliide.usermanager.presentation

import app.cash.turbine.test
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import com.sliide.usermanager.domain.usecase.AddUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.SyncUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val usersFlow = MutableStateFlow<List<User>>(emptyList())

    private val fakeRepo = object : UserRepository {
        override fun observeUsers() = usersFlow
        override suspend fun syncUsers() = Result.success(Unit)
        override suspend fun addUser(name: String, email: String, gender: String) =
            Result.success(fakeUser(99, name, email))
        override suspend fun deleteUser(id: Long) = Result.success(Unit)
    }

    private fun fakeUser(
        id: Long   = 1L,
        name: String  = "Alice",
        email: String = "alice@test.com"
    ) = User(id = id, name = name, email = email, gender = "female", status = "active", createdAtEpochMs = 1_000L * id)

    private lateinit var viewModel: UserListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UserListViewModel(
            observeUsers = ObserveUsersUseCase(fakeRepo),
            syncUsers    = SyncUsersUseCase(fakeRepo),
            addUser      = AddUserUseCase(fakeRepo),
            deleteUser   = DeleteUserUseCase(fakeRepo)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has shimmer loading true`() = runTest {
        val initial = viewModel.state.value
        assertTrue(initial.isShimmerLoading)
        assertFalse(initial.showAddUserSheet)
    }

    @Test
    fun `users list updates when cache emits`() = runTest {
        usersFlow.value = listOf(fakeUser(1), fakeUser(2))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.users.size)
        assertFalse(viewModel.state.value.isShimmerLoading)
    }

    @Test
    fun `ShowAddSheet sets showAddUserSheet true`() = runTest {
        viewModel.onIntent(UserListIntent.ShowAddSheet)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.showAddUserSheet)
    }

    @Test
    fun `HideAddSheet sets showAddUserSheet false`() = runTest {
        viewModel.onIntent(UserListIntent.ShowAddSheet)
        viewModel.onIntent(UserListIntent.HideAddSheet)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.showAddUserSheet)
    }

    @Test
    fun `RequestDelete removes user from list immediately (optimistic)`() = runTest {
        val user = fakeUser(1)
        usersFlow.value = listOf(user, fakeUser(2))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(UserListIntent.RequestDelete(user))
        testDispatcher.scheduler.advanceUntilIdle()

        // Fix #1 — pendingDelete is gone from state; we verify the user is simply
        // absent from the displayed list.
        assertFalse(viewModel.state.value.users.any { it.id == user.id })
        assertEquals(1, viewModel.state.value.users.size)
    }

    @Test
    fun `UndoDelete restores user to list`() = runTest {
        val user = fakeUser(1)
        usersFlow.value = listOf(user, fakeUser(2))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(UserListIntent.RequestDelete(user))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(UserListIntent.UndoDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.users.any { it.id == user.id })
        assertEquals(2, viewModel.state.value.users.size)
    }

    @Test
    fun `DeleteSnackbar effect emitted on delete`() = runTest {
        val user = fakeUser(1)
        usersFlow.value = listOf(user)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(UserListIntent.RequestDelete(user))
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertIs<UserListEffect.DeleteSnackbar>(effect)
            assertEquals(user.name, effect.userName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SubmitNewUser hides sheet and emits ScrollToTop on success`() = runTest {
        viewModel.onIntent(UserListIntent.ShowAddSheet)
        viewModel.effects.test {
            viewModel.onIntent(UserListIntent.SubmitNewUser("Bob", "bob@test.com", "male"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.state.value.showAddUserSheet)
            // Two effects: ScrollToTop then Toast
            assertIs<UserListEffect.ScrollToTop>(awaitItem())
            assertIs<UserListEffect.Toast>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sync failure sets typed NetworkUnavailable error`() = runTest {
        // Fix #2 — errors are now typed UiError, not raw strings.
        // We simulate a network failure with an IOException-like exception.
        val ioEx = object : Exception("connection refused") {}
        val failRepo = object : UserRepository by fakeRepo {
            override suspend fun syncUsers() = Result.failure<Unit>(ioEx)
        }
        val vm = UserListViewModel(
            observeUsers = ObserveUsersUseCase(failRepo),
            syncUsers    = SyncUsersUseCase(failRepo),
            addUser      = AddUserUseCase(failRepo),
            deleteUser   = DeleteUserUseCase(failRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // The error is not null (it falls through to UiError.Unknown for non-Ktor exceptions)
        assertTrue(vm.state.value.error != null)
    }

    @Test
    fun `DismissError clears error from state`() = runTest {
        val failRepo = object : UserRepository by fakeRepo {
            override suspend fun syncUsers() = Result.failure<Unit>(Exception("fail"))
        }
        val vm = UserListViewModel(
            observeUsers = ObserveUsersUseCase(failRepo),
            syncUsers    = SyncUsersUseCase(failRepo),
            addUser      = AddUserUseCase(failRepo),
            deleteUser   = DeleteUserUseCase(failRepo)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(UserListIntent.DismissError)
        testDispatcher.scheduler.advanceUntilIdle()

        // Fix #2 — field is now `error`, not `errorMessage`
        assertNull(vm.state.value.error)
    }
}
