package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ViewMode
import com.example.service.BackgroundSyncService
import com.example.service.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.NavigationTabs
import com.example.ui.components.TopHeaderBar
import com.example.ui.dialogs.AdjustScheduleDialog
import com.example.ui.dialogs.EventDetailDialog
import com.example.ui.screens.ExecutiveMatrixScreen
import com.example.ui.screens.ScheduleComparisonScreen
import com.example.ui.screens.WebMonitorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        // Xóa sạch toàn bộ thông báo cũ trên thanh thông báo khi mở ứng dụng
        NotificationHelper.clearAllNotifications(this)

        // Bắt buộc tự động chạy ngầm giám sát Cổng 8888 liên tục
        val savedInterval = BackgroundSyncService.getSavedIntervalSeconds(this)
        BackgroundSyncService.startService(this, savedInterval)

        val openTab = intent.getStringExtra("OPEN_TAB")

        setContent {
            MyApplicationTheme {
                MainApp(initialTab = openTab)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel(), initialTab: String? = null) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+ (Tiramisu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab == "WEB_MONITOR") {
            viewModel.setViewMode(ViewMode.WEB_MONITOR)
        }
    }

    val currentView by viewModel.currentView.collectAsStateWithLifecycle()
    val currentWeekStart by viewModel.currentWeekStart.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLeaderId by viewModel.selectedLeaderId.collectAsStateWithLifecycle()
    val selectedDayFilter by viewModel.selectedDayFilter.collectAsStateWithLifecycle()
    val currentWeekEvents by viewModel.currentWeekEvents.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val leaders by viewModel.leaders.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val weekDays by viewModel.currentWeekDates.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val monitorStatus by viewModel.monitorStatus.collectAsStateWithLifecycle()
    val changeLogs by viewModel.changeLogs.collectAsStateWithLifecycle()

    val selectedDetailEvent by viewModel.selectedDetailEvent.collectAsStateWithLifecycle()
    var showAdjustDialog by remember { mutableStateOf(false) }
    var executiveDisplayMode by rememberSaveable { mutableStateOf("LEADERS") }

    // Toast / Snackbar observation
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopHeaderBar(
                config = config,
                weekStart = currentWeekStart,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onPrevWeek = { viewModel.prevWeek() },
                onNextWeek = { viewModel.nextWeek() },
                onCurrentWeek = { viewModel.currentWeek() },
                onTitleClick = {
                    executiveDisplayMode = "LEADERS"
                    viewModel.setSelectedLeaderId("tran-hoang-do")
                    viewModel.setViewMode(ViewMode.BY_LEADER)
                },
                onOpenAdjustDialog = { showAdjustDialog = true },
                onOpenComparison = {
                    executiveDisplayMode = "COMPARISON"
                    viewModel.setViewMode(ViewMode.BY_LEADER)
                },
                scanStatusMessage = monitorStatus.statusMessage,
                onScanStatusClick = { viewModel.setViewMode(ViewMode.WEB_MONITOR) }
            )
        },
        bottomBar = {
            NavigationTabs(
                currentView = currentView,
                onViewChange = { viewModel.setViewMode(it) },
                changeLogsCount = changeLogs.count { it.changeType != "KHÔNG THAY ĐỔI" }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentView) {
                ViewMode.BY_LEADER -> {
                    ExecutiveMatrixScreen(
                        events = currentWeekEvents,
                        leaders = leaders,
                        config = config,
                        weekDays = weekDays,
                        selectedLeaderId = selectedLeaderId,
                        onSelectLeader = { viewModel.setSelectedLeaderId(it) },
                        selectedDayFilter = selectedDayFilter,
                        onSelectDayFilter = { viewModel.setSelectedDayFilter(it) },
                        onViewDetail = { viewModel.selectedDetailEvent.value = it },
                        onOpenAdjustDialog = { showAdjustDialog = true },
                        onOpenComparison = {
                            executiveDisplayMode = "COMPARISON"
                            viewModel.setViewMode(ViewMode.BY_LEADER)
                        },
                        scanStatus = monitorStatus.statusMessage,
                        displayMode = executiveDisplayMode,
                        onToggleDisplayMode = { executiveDisplayMode = it },
                        allEvents = allEvents,
                        currentWeekStart = currentWeekStart,
                        onPrevWeek = { viewModel.prevWeek() },
                        onNextWeek = { viewModel.nextWeek() },
                        onCurrentWeek = { viewModel.currentWeek() },
                        archivedPreviousEvents = viewModel.getArchivedPreviousEvents(),
                        archivedPreviousTitle = viewModel.getArchivedPreviousTitle()
                    )
                }

                ViewMode.DIFF_COMPARISON,
                ViewMode.WEB_MONITOR -> {
                    WebMonitorScreen(
                        monitorStatus = monitorStatus,
                        changeLogs = changeLogs,
                        totalEventsCount = allEvents.size,
                        allEvents = allEvents,
                        currentWeekStart = currentWeekStart,
                        onPrevWeek = { viewModel.prevWeek() },
                        onNextWeek = { viewModel.nextWeek() },
                        onCurrentWeek = { viewModel.currentWeek() },
                        onCheckNow = { viewModel.checkWebNow() },
                        onSimulateChange = { viewModel.simulateWebUpdate() },
                        onClearLogs = { viewModel.clearChangeLogs() },
                        onUpdateTargetUrl = { viewModel.updateTargetUrl(it) },
                        onUpdateAccount = { u, p -> viewModel.updateMonitorAccount(u, p) },
                        onImportPastedSchedule = { viewModel.applyAdjustedScheduleEdition2(it) },
                        onSyncLatest = { viewModel.syncLatestFromWeb() },
                        onSetMonitorInterval = { viewModel.setMonitorInterval(it) }
                    )
                }
            }
        }
    }

    // Read-only Event Detail Dialog
    if (selectedDetailEvent != null) {
        EventDetailDialog(
            event = selectedDetailEvent,
            leaders = leaders,
            onDismiss = { viewModel.selectedDetailEvent.value = null }
        )
    }

    // Dialog for Updating / Applying Lịch Điều Chỉnh Mới Nhất
    if (showAdjustDialog) {
        AdjustScheduleDialog(
            onDismiss = { showAdjustDialog = false },
            onApplyEdition = { customText ->
                viewModel.applyAdjustedScheduleEdition2(customText)
            },
            onSyncLatest = {
                viewModel.syncLatestFromWeb()
            }
        )
    }
}
