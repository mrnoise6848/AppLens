package com.noise.applens.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noise.applens.data.AppDiscoveryDataSource
import com.noise.applens.data.AppIconCache
import com.noise.applens.data.AppIndexStore
import com.noise.applens.domain.analysis.AppAnalysis
import com.noise.applens.domain.analysis.AppAnalysisEngine
import com.noise.applens.domain.analysis.AppFilter
import com.noise.applens.domain.analysis.AppSort
import com.noise.applens.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Composition root of AppLens: owns the inventory index, the scan and the icon cache.
 *
 * Uses `AndroidViewModel` + the ViewModel that is already on the classpath through
 * `androidx.activity` (no new dependency) so the scan state and the inventory survive
 * configuration changes instead of triggering a rescan on every rotation.
 *
 * PackageManager stays the source of truth: [refresh] always rebuilds the index from a fresh
 * scan, and the local index only caches and compares (spec §8).
 */
class AppLensViewModel(application: Application) : AndroidViewModel(application) {

    private val discovery = AppDiscoveryDataSource(application)
    private val indexStore = AppIndexStore(application)
    private val iconCache = AppIconCache(application.packageManager)
    private val analysisEngine = AppAnalysisEngine(application.packageManager)

    private val _uiState = MutableStateFlow(AppLensUiState())
    val uiState: StateFlow<AppLensUiState> = _uiState.asStateFlow()

    /** packageName → analysis, rebuilt with every completed scan. */
    private var analysisByPackage: Map<String, AppAnalysis> = emptyMap()

    init {
        // Load the previous snapshot for change detection; a missing/corrupt cache is not an error.
        viewModelScope.launch {
            val snapshot = indexStore.loadSnapshot()
            _uiState.update { it.copy(previousSnapshot = snapshot) }
        }
    }

    /**
     * Runs a full discovery pass.
     *
     * @param force `false` keeps an already loaded inventory instead of rescanning.
     */
    fun refresh(force: Boolean = false) {
        val current = _uiState.value
        if (current.scan is ScanPhase.Scanning) return
        if (!force && current.isReady) return

        viewModelScope.launch {
            _uiState.update { it.copy(scan = ScanPhase.Scanning(read = 0, total = 0)) }

            val snapshotBefore = current.previousSnapshot ?: indexStore.loadSnapshot()

            val result = runCatching {
                discovery.discover { read, total ->
                    _uiState.update { state ->
                        state.copy(scan = ScanPhase.Scanning(read = read, total = total))
                    }
                }
            }

            val discovered = result.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        scan = ScanPhase.Failed(
                            message = error.message ?: error.javaClass.simpleName,
                        ),
                    )
                }
                return@launch
            }

            // Rebuild the index from PackageManager data.
            indexStore.replaceAll(discovered.apps)
            invalidateIcons()

            // Derived analysis (permissions, review signals, dashboard counters).
            val analyses = withContext(Dispatchers.Default) {
                analysisEngine.analyzeAll(discovered.apps)
            }
            val summary = analysisEngine.summarize(analyses)
            analysisByPackage = analyses.associateBy { it.app.packageName }

            val diff = snapshotBefore?.let { indexStore.diffAgainst(it) }
            val written = indexStore.persistSnapshot(discovered.apps)

            _uiState.update {
                it.copy(
                    scan = ScanPhase.Ready(
                        failureCount = discovered.failures.size,
                        elapsedMillis = discovered.elapsedMillis,
                    ),
                    apps = discovered.apps,
                    analyses = analyses,
                    summary = summary,
                    failures = discovered.failures,
                    previousSnapshot = written,
                    diff = diff,
                )
            }
        }
    }

    /** Clears the "changed since last scan" delta once the user has seen it. */
    fun consumeDiff() {
        _uiState.update { it.copy(diff = null) }
    }

    /** O(1) lookup used by the detail and comparison screens. */
    fun app(packageName: String): InstalledApp? = indexStore.find(packageName)

    /** O(1) lookup of the derived analysis used by detail, list and comparison screens. */
    fun analysis(packageName: String): AppAnalysis? = analysisByPackage[packageName]

    // ------------------------------------------------------------------------------- list state

    private val _listState = MutableStateFlow(ListUiState())
    val listState: StateFlow<ListUiState> = _listState.asStateFlow()

    /** Selects the filter used when the list is opened from an entry point. */
    fun openList(filter: AppFilter = AppFilter.ALL) {
        _listState.update { it.copy(filter = filter) }
    }

    fun setListFilter(filter: AppFilter) {
        _listState.update { it.copy(filter = filter) }
    }

    fun setListQuery(query: String) {
        _listState.update { it.copy(query = query) }
    }

    fun setListSort(sort: AppSort) {
        _listState.update { it.copy(sort = sort) }
    }

    /** Search over the indexed inventory (spec §13). */
    fun search(query: String): List<InstalledApp> = indexStore.search(query)

    /** Loads an application icon through the bounded cache; blocking, call off the main thread. */
    fun loadIcon(packageName: String) = iconCache.getOrLoad(packageName)

    /** Clears cached icons — used when the inventory is rebuilt after a rescan. */
    fun invalidateIcons() = iconCache.clear()
}
