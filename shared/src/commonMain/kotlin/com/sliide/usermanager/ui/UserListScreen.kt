package com.sliide.usermanager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.presentation.UserListEffect
import com.sliide.usermanager.presentation.UserListIntent
import com.sliide.usermanager.presentation.UserListViewModel
import com.sliide.usermanager.presentation.toDisplayMessage
import com.sliide.usermanager.ui.components.DeleteConfirmationDialog
import com.sliide.usermanager.ui.components.ShimmerList
import com.sliide.usermanager.ui.components.UserCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun UserListScreen() {
    val viewModel = koinViewModel<UserListViewModel>()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val isAtTop by remember { derivedStateOf { !listState.canScrollBackward } }

    var pendingDialogUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UserListEffect.DeleteSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message     = "${effect.userName} deleted",
                        actionLabel = "Undo",
                        duration    = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(UserListIntent.UndoDelete)
                    }
                }
                is UserListEffect.Toast   -> snackbarHostState.showSnackbar(effect.message)
                UserListEffect.ScrollToTop -> listState.animateScrollToItem(0)
            }
        }
    }

    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error.toDisplayMessage())
            viewModel.onIntent(UserListIntent.DismissError)
        }
    }

    // Posh Slate Navy & Indigo (Rolled back from Pink)
    val poshBg = Color(0xFF020617)
    val poshIndigo = Color(0xFF6366F1)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = poshBg
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title  = { Text("Premium Directory", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = poshBg,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.onIntent(UserListIntent.Refresh) }) {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = "Refresh",
                                tint = poshIndigo
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    expanded = isAtTop,
                    onClick  = { viewModel.onIntent(UserListIntent.ShowAddSheet) },
                    icon     = { Icon(Icons.Default.Add, contentDescription = null) },
                    text     = { Text("Add User") },
                    containerColor = poshIndigo,
                    contentColor = Color.White
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = poshBg
        ) { innerPadding ->

            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh    = { viewModel.onIntent(UserListIntent.Refresh) },
                modifier     = Modifier
                    .fillMaxSize()
                    .background(poshBg)
                    .padding(innerPadding)
            ) {
                if (state.isShimmerLoading) {
                    ShimmerList()
                } else {
                    AdaptiveUserList(
                        users       = state.users,
                        listState   = listState,
                        onLongPress = { user -> pendingDialogUser = user },
                        onSwipeDismiss = { user ->
                            viewModel.onIntent(UserListIntent.RequestDelete(user))
                        }
                    )
                }
            }
        }
    }

    pendingDialogUser?.let { user ->
        DeleteConfirmationDialog(
            user      = user,
            onConfirm = {
                pendingDialogUser = null
                viewModel.onIntent(UserListIntent.RequestDelete(user))
            },
            onDismiss = { pendingDialogUser = null }
        )
    }

    if (state.showAddUserSheet) {
        AddUserSheet(
            sheetState = sheetState,
            isLoading  = state.isAddingUser,
            onDismiss  = { viewModel.onIntent(UserListIntent.HideAddSheet) },
            onSubmit   = { name, email, gender ->
                viewModel.onIntent(UserListIntent.SubmitNewUser(name, email, gender))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveUserList(
    users: ImmutableList<User>,
    listState: LazyListState,
    onLongPress: (User) -> Unit,
    onSwipeDismiss: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (users.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.fillMaxSize()
            )
        } else if (maxWidth >= 600.dp) {
            LazyVerticalGrid(
                columns        = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier       = Modifier.fillMaxSize()
            ) {
                items(users, key = { it.id }) { user ->
                    UserCard(
                        user       = user,
                        onLongPress = { onLongPress(user) },
                        modifier   = Modifier.animateItem()
                    )
                }
            }
        } else {
            LazyColumn(
                state          = listState,
                contentPadding = PaddingValues(bottom = 100.dp), 
                modifier       = Modifier.fillMaxSize()
            ) {
                items(users, key = { it.id }) { user ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { 
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                onSwipeDismiss(user)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state                    = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent        = { SwipeDismissBackground() },
                        modifier                 = Modifier.animateItem()
                    ) {
                        UserCard(
                            user       = user,
                            onLongPress = { onLongPress(user) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDismissBackground() {
    val color by animateColorAsState(
        targetValue    = MaterialTheme.colorScheme.errorContainer,
        animationSpec  = tween(200),
        label          = "swipe_bg"
    )
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .padding(end = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector  = Icons.Default.Delete,
            contentDescription = "Delete",
            tint         = MaterialTheme.colorScheme.onErrorContainer,
            modifier     = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Person,
            contentDescription = null,
            modifier           = Modifier.size(80.dp),
            tint               = Color(0xFF94A3B8)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text      = "No Users Yet",
            style     = MaterialTheme.typography.headlineSmall,
            color     = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Add your first user with the button below,\nor pull down to refresh.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}
