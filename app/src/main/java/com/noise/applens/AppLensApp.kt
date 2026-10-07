package com.noise.applens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noise.applens.navigation.AppNavigator
import com.noise.applens.navigation.Screen
import com.noise.applens.state.AppLensViewModel
import com.noise.applens.ui.apps.AppListScreen
import com.noise.applens.ui.dashboard.DashboardScreen

/**
 * Root of the application: owns navigation, triggers the first scan and routes the current screen.
 */
@Composable
fun AppLensApp(viewModel: AppLensViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = remember { AppNavigator() }

    // Real scan, started once; a completed inventory is never rescanned silently.
    LaunchedEffect(Unit) { viewModel.refresh() }

    BackHandler(enabled = navigator.canGoBack) { navigator.pop() }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when (val screen = navigator.current) {
            Screen.Dashboard -> DashboardScreen(
                state = state,
                onOpenApps = { filter ->
                    viewModel.openList(filter)
                    navigator.navigateTo(Screen.AppList(filter))
                },
                onRetry = { viewModel.refresh(force = true) },
                modifier = Modifier.padding(padding),
            )

            is Screen.AppList -> {
                val listState by viewModel.listState.collectAsStateWithLifecycle()
                AppListScreen(
                    analyses = state.analyses,
                    listState = listState,
                    iconLoader = viewModel::loadIcon,
                    onBack = { navigator.pop() },
                    onFilterChange = viewModel::setListFilter,
                    onQueryChange = viewModel::setListQuery,
                    onSortChange = viewModel::setListSort,
                    onOpenApp = { packageName ->
                        navigator.navigateTo(Screen.AppDetail(packageName))
                    },
                    modifier = Modifier.padding(padding),
                )
            }

            is Screen.AppDetail -> PendingScreen(
                title = "Application details",
                modifier = Modifier.padding(padding),
            )

            is Screen.Compare -> PendingScreen(
                title = "Comparison",
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** Placeholder for screens that are implemented in a later phase; removed as each phase lands. */
@Composable
private fun PendingScreen(title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}
