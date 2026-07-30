package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.CustomAlarm
import com.example.prayer.City
import com.example.prayer.PrayerCalculator
import com.example.prayer.PrayerTimes
import com.example.repository.NamazRepository
import com.example.scheduler.PrayerScheduler
import com.example.service.NotificationReplyService
import com.example.ui.theme.SmartNamazTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class MainViewModel(private val repository: NamazRepository) : ViewModel() {

    val selectedCity: StateFlow<City> = repository.selectedCity
    val isManualNamazActive: StateFlow<Boolean> = repository.isNamazModeManualActive
    val autoReplyText: StateFlow<String> = repository.autoReplyText
    val isJummahModeEnabled: StateFlow<Boolean> = repository.isJummahModeEnabled
    val jummahDurationMinutes: StateFlow<Int> = repository.jummahDurationMinutes
    val isEidModeEnabled: StateFlow<Boolean> = repository.isEidModeEnabled
    val prayerSilenceDurationMins: StateFlow<Int> = repository.prayerSilenceDurationMins

    val customAlarms: StateFlow<List<CustomAlarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedCity(city: City) {
        repository.setSelectedCity(city)
    }

    fun toggleManualNamazMode() {
        val current = isManualNamazActive.value
        repository.setManualNamazMode(!current)
    }

    fun updateAutoReplyText(text: String) {
        repository.setAutoReplyText(text)
    }

    fun setJummahEnabled(enabled: Boolean) {
        repository.setJummahModeEnabled(enabled)
    }

    fun setJummahDuration(duration: Int) {
        repository.setJummahDurationMinutes(duration)
    }

    fun setEidEnabled(enabled: Boolean) {
        repository.setEidModeEnabled(enabled)
    }

    fun setPrayerSilenceDuration(duration: Int) {
        repository.setPrayerSilenceDurationMins(duration)
    }

    fun addCustomAlarm(title: String, hour: Int, minute: Int, duration: Int) {
        viewModelScope.launch {
            repository.addAlarm(
                CustomAlarm(
                    title = title,
                    hour = hour,
                    minute = minute,
                    durationMinutes = duration
                )
            )
        }
    }

    fun toggleAlarmEnabled(alarm: CustomAlarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun deleteAlarm(alarm: CustomAlarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }
}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(NamazRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule prayer alarms on app start
        PrayerScheduler.scheduleAllPrayersAndAlarms(applicationContext)

        setContent {
            SmartNamazTheme {
                val viewModel: MainViewModel = viewModel(factory = ViewModelFactory(applicationContext))
                NamazMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamazMainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val isManualActive by viewModel.isManualNamazActive.collectAsState()
    val isScheduledActive = remember { NamazRepository.isNamazModeActive(context) }
    val isNamazActive = isManualActive || isScheduledActive

    val selectedCity by viewModel.selectedCity.collectAsState()
    val todayPrayerTimes = remember(selectedCity) {
        PrayerCalculator.calculateTimes(selectedCity, Date())
    }

    var hasDndPermission by remember { mutableStateOf(checkDndPermission(context)) }
    var hasNotificationListenerPermission by remember { mutableStateOf(checkNotificationListenerPermission(context)) }
    var isInternetActive by remember { mutableStateOf(checkInternetConnection(context)) }

    // Periodically refresh permission & connectivity states
    LaunchedEffect(Unit) {
        while (true) {
            hasDndPermission = checkDndPermission(context)
            hasNotificationListenerPermission = checkNotificationListenerPermission(context)
            isInternetActive = checkInternetConnection(context)
            delay(3000)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Prayers") },
                    label = { Text("Prayers") },
                    modifier = Modifier.testTag("nav_tab_prayers")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Message, contentDescription = "Auto Reply") },
                    label = { Text("Auto Reply") },
                    modifier = Modifier.testTag("nav_tab_auto_reply")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.NightsStay, contentDescription = "Ibadat") },
                    label = { Text("Ibadat") },
                    modifier = Modifier.testTag("nav_tab_ibadat")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Hero Banner Header
            HeroHeaderCard(
                city = selectedCity,
                isNamazActive = isNamazActive,
                isInternetActive = isInternetActive,
                hasDndPermission = hasDndPermission,
                todayPrayerTimes = todayPrayerTimes,
                onToggleNamazMode = {
                    viewModel.toggleManualNamazMode()
                    PrayerScheduler.scheduleAllPrayersAndAlarms(context)
                }
            )

            // Dynamic Tab Views
            when (selectedTab) {
                0 -> PrayerTimesTab(viewModel, todayPrayerTimes)
                1 -> AutoReplyTab(viewModel, isInternetActive)
                2 -> CustomIbadatTab(viewModel)
                3 -> SettingsPermissionsTab(
                    hasDndPermission = hasDndPermission,
                    hasNotificationListenerPermission = hasNotificationListenerPermission,
                    onOpenDndSettings = { openDndSettings(context) },
                    onOpenNotificationListenerSettings = { openNotificationListenerSettings(context) }
                )
            }
        }
    }
}

@Composable
fun HeroHeaderCard(
    city: City,
    isNamazActive: Boolean,
    isInternetActive: Boolean,
    hasDndPermission: Boolean,
    todayPrayerTimes: PrayerTimes,
    onToggleNamazMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Hero Mosque Background Artwork
            Image(
                painter = painterResource(id = R.drawable.img_hero_mosque),
                contentDescription = "Mosque Artwork Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.45f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "City Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${city.name}, Punjab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status Badge
                    Surface(
                        color = if (isNamazActive) Color(0xFFD97706) else Color(0xFF10B981),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isNamazActive) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isNamazActive) "NAMAZ MODE ACTIVE" else "MODE INACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Karachi / Hanafi Method",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Dhuhr: ${todayPrayerTimes.dhuhr} | Asr: ${todayPrayerTimes.asr}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = onToggleNamazMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNamazActive) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("toggle_namaz_mode_button")
                    ) {
                        Text(text = if (isNamazActive) "Turn OFF" else "Quick Silence")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Connectivity & Permission Badges Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BadgeChip(
                        icon = if (isInternetActive) Icons.Default.Wifi else Icons.Default.WifiOff,
                        label = if (isInternetActive) "Internet Auto-Reply Ready" else "No Internet Connection",
                        isActive = isInternetActive
                    )
                    BadgeChip(
                        icon = Icons.Default.Security,
                        label = if (hasDndPermission) "DND Granted" else "DND Required",
                        isActive = hasDndPermission
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeChip(icon: ImageVector, label: String, isActive: Boolean) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun PrayerTimesTab(viewModel: MainViewModel, prayerTimes: PrayerTimes) {
    val selectedCity by viewModel.selectedCity.collectAsState()
    val jummahEnabled by viewModel.isJummahModeEnabled.collectAsState()
    val jummahDuration by viewModel.jummahDurationMinutes.collectAsState()
    val eidEnabled by viewModel.isEidModeEnabled.collectAsState()
    val prayerSilenceDuration by viewModel.prayerSilenceDurationMins.collectAsState()

    var showCityDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Punjab City Selection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCityDropdown = true }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedCity.name} (${selectedCity.latitude}° N, ${selectedCity.longitude}° E)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Change", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        DropdownMenu(
                            expanded = showCityDropdown,
                            onDismissRequest = { showCityDropdown = false }
                        ) {
                            PrayerCalculator.PUNJAB_CITIES.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.name) },
                                    onClick = {
                                        viewModel.setSelectedCity(city)
                                        showCityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily 5 Prayers List
        item {
            Text(
                text = "Today's Prayer Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(
            listOf(
                Triple("Fajr", prayerTimes.fajr, "Morning twilight angle: 18.0°"),
                Triple("Sunrise", prayerTimes.sunrise, "Solar zenith angle: 0.833°"),
                Triple("Dhuhr", prayerTimes.dhuhr, "Zawal buffer: +2 mins"),
                Triple("Asr (Hanafi)", prayerTimes.asr, "Hanafi double shadow length factor = 2"),
                Triple("Maghrib", prayerTimes.maghrib, "Sunset refraction angle: 0.833°"),
                Triple("Isha", prayerTimes.isha, "Evening twilight angle: 18.0°")
            )
        ) { item ->
            PrayerItemCard(name = item.first, timeStr = item.second, note = item.third)
        }

        // Jummah & Eid Modes Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Friday Jummah & Special Modes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Jummah Mode (Extended Silence)", fontWeight = FontWeight.SemiBold)
                            Text("Extends Friday silent window for Bayan & Khutbah", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = jummahEnabled,
                            onCheckedChange = { viewModel.setJummahEnabled(it) }
                        )
                    }

                    if (jummahEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Jummah Silent Window: $jummahDuration mins", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = jummahDuration.toFloat(),
                            onValueChange = { viewModel.setJummahDuration(it.toInt()) },
                            valueRange = 45f..120f,
                            steps = 15
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Eid Mode (Eid-ul-Fitr & Eid-ul-Adha)", fontWeight = FontWeight.SemiBold)
                            Text("Special morning prayer timing profile", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = eidEnabled,
                            onCheckedChange = { viewModel.setEidEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Standard Prayer Silence Duration: $prayerSilenceDuration mins", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = prayerSilenceDuration.toFloat(),
                        onValueChange = { viewModel.setPrayerSilenceDuration(it.toInt()) },
                        valueRange = 15f..60f,
                        steps = 9
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun PrayerItemCard(name: String, timeStr: String, note: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = timeStr,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun AutoReplyTab(viewModel: MainViewModel, isInternetActive: Boolean) {
    val autoReplyText by viewModel.autoReplyText.collectAsState()
    var editedText by remember(autoReplyText) { mutableStateOf(autoReplyText) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict Non-Cellular Internet-Only Policy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app NEVER reads SIM calls or sends GSM SMS. SIM credit will NEVER be consumed. Auto-reply works strictly over Wi-Fi / Mobile Data via notification direct replies for WhatsApp, Messenger, Telegram & Instagram.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure Social Auto-Reply Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("auto_reply_text_input"),
                        label = { Text("Auto-Reply Message") },
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.updateAutoReplyText(editedText) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_auto_reply_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Reply Text")
                    }
                }
            }
        }

        item {
            Text(
                text = "Supported Social Messaging Apps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair("WhatsApp & WhatsApp Business", "com.whatsapp"),
                    Pair("Facebook Messenger", "com.facebook.orca"),
                    Pair("Telegram Messenger", "org.telegram.messenger"),
                    Pair("Instagram Direct", "com.instagram.android"),
                    Pair("Signal Private Messenger", "org.thoughtcrime.securesms")
                ).forEach { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = app.first, fontWeight = FontWeight.SemiBold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Supported",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomIbadatTab(viewModel: MainViewModel) {
    val alarms by viewModel.customAlarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_custom_ibadat_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Custom Ibadat & Silent Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set custom recurring or one-time silent timers for Tahajjud, Nawafil, Taraweeh, or Itikaf with automatic internet auto-reply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (alarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Custom Alarms Configured",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the + button to add a custom Tahajjud or Nawafil timer.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(alarms) { alarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = alarm.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%02d:%02d (%d mins duration)", alarm.hour, alarm.minute, alarm.durationMinutes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Days: ${alarm.repeatDays}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { viewModel.toggleAlarmEnabled(alarm) }
                                )
                                IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAlarmDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, hour, min, duration ->
                    viewModel.addCustomAlarm(title, hour, min, duration)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, hour: Int, min: Int, duration: Int) -> Unit
) {
    var title by remember { mutableStateOf("Tahajjud Prayer") }
    var hour by remember { mutableIntStateOf(3) }
    var minute by remember { mutableIntStateOf(30) }
    var duration by remember { mutableIntStateOf(45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Ibadat Alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alarm Title (e.g. Tahajjud, Nawafil)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { hour = it.toIntOrNull() ?: 0 },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute.toString(),
                        onValueChange = { minute = it.toIntOrNull() ?: 0 },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = duration.toString(),
                    onValueChange = { duration = it.toIntOrNull() ?: 30 },
                    label = { Text("Silent Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, hour, minute, duration) }) {
                Text("Add Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsPermissionsTab(
    hasDndPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    onOpenDndSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Required Permissions & Quick Settings Tile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // DND Card
        item {
            PermissionCard(
                title = "Do Not Disturb (DND) Access",
                description = "Required to toggle phone ringers to Silent mode automatically during prayer times.",
                isGranted = hasDndPermission,
                buttonLabel = "Grant DND Permission",
                onClick = onOpenDndSettings
            )
        }

        // Notification Listener Card
        item {
            PermissionCard(
                title = "Notification Listener Service",
                description = "Required to read incoming WhatsApp/social notifications and send automated internet text replies.",
                isGranted = hasNotificationListenerPermission,
                buttonLabel = "Grant Notification Listener",
                onClick = onOpenNotificationListenerSettings
            )
        }

        // Quick Settings Tile Guide Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notification Shade Quick Toggle Tile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Swipe down from top of your screen to open Quick Settings shade.\n" +
                                "2. Tap the Edit (Pencil) icon.\n" +
                                "3. Drag 'Namaz Mode' tile into your active tiles section.\n" +
                                "4. Now you can toggle Namaz Mode instantly alongside Wi-Fi & Bluetooth!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Non-cellular Guarantee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Cellular Blind Guarantee",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "No SIM call or SMS permissions. 0% credit consumption risk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "ACTION REQUIRED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

// Permission & Connectivity helpers
private fun checkDndPermission(context: Context): Boolean {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.isNotificationPolicyAccessGranted
    } else {
        true
    }
}

private fun openDndSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun checkNotificationListenerPermission(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun checkInternetConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val activeNetworkInfo = cm.activeNetworkInfo
        @Suppress("DEPRECATION")
        activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}
