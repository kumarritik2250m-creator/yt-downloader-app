package com.example.ytdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ytdownloader.ui.theme.YTDownloaderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermissionIfNeeded()
        handleSharedIntent(intent)

        setContent {
            YTDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    YTDownApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                viewModel.onUrlChanged(sharedText)
            }
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

/** Root composable — bottom nav bar (Home / Downloads / More) + nav graph */
@Composable
fun YTDownApp(viewModel: DownloadViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(viewModel, navController) }
            composable("detail") { DetailScreen(viewModel, navController) }
            composable("downloads") { DownloadsScreen(viewModel) }
            composable("more") { MoreScreen() }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home" || currentRoute == "detail",
            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "downloads",
            onClick = { navController.navigate("downloads") },
            icon = { Icon(Icons.Default.GetApp, contentDescription = "Downloads") },
            label = { Text("Downloads") }
        )
        NavigationBarItem(
            selected = currentRoute == "more",
            onClick = { navController.navigate("more") },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
            label = { Text("More") }
        )
    }
}

/** Image 1: search bar + recent searches list */
@Composable
fun HomeScreen(viewModel: DownloadViewModel, navController: NavHostController) {
    val url by viewModel.url.collectAsState()
    val recent by viewModel.recentSearches.collectAsState()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is DownloadState.InfoFetched) {
            navController.navigate("detail")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("YTdown", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { viewModel.onUrlChanged(it) },
            label = { Text("Search or insert URL") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { viewModel.fetchInfo() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (state is DownloadState.FetchingInfo) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching info...")
            }
        }
        if (state is DownloadState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("❌ ${(state as DownloadState.Error).message}", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Recent searches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        if (recent.isEmpty()) {
            Text("Koi recent search nahi hai.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(recent) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            viewModel.onUrlChanged(item.url)
                            viewModel.fetchInfo(item.url)
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.title, fontWeight = FontWeight.Medium, maxLines = 2)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Image 2 + Image 3: video/audio tab, title/author/container, quality list */
@Composable
fun DetailScreen(viewModel: DownloadViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsState()
    var isAudioTab by remember { mutableStateOf(false) }

    when (val s = state) {
        is DownloadState.InfoFetched -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Download", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Adjust download", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = if (isAudioTab) 0 else 1) {
                    Tab(selected = isAudioTab, onClick = { isAudioTab = true }, text = { Text("Audio") })
                    Tab(selected = !isAudioTab, onClick = { isAudioTab = false }, text = { Text("Video") })
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = s.info.title, onValueChange = {}, readOnly = true,
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = s.info.author, onValueChange = {}, readOnly = true,
                    label = { Text("Author") }, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (isAudioTab) "Audio quality" else "Video quality",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formats = if (isAudioTab) s.info.audioFormats else s.info.videoFormats

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(formats) { format ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { viewModel.startDownload(s.info.title, format.formatId, isAudioTab) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "${format.container} • ${format.resolutionLabel}",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${format.codec}  ${format.fileSizeLabel}  id:${format.formatId}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.Default.GetApp, contentDescription = "Download")
                            }
                        }
                    }
                }
            }
        }

        is DownloadState.Downloading -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Downloading...", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(progress = { s.progress / 100f }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("${s.progress.toInt()}%  •  ETA: ${s.etaSeconds}s")
            }
        }

        is DownloadState.Completed -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅ Saved to device storage")
                Spacer(modifier = Modifier.height(4.dp))
                Text(s.filePath, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    viewModel.reset()
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                }) { Text("Naya Download") }
            }
        }

        is DownloadState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("❌ ${s.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Wapas Jayein") }
            }
        }

        else -> {
            // Idle/FetchingInfo state par ye screen normally nahi khulti
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Image 4: sab downloaded audio/video ki list */
@Composable
fun DownloadsScreen(viewModel: DownloadViewModel) {
    val downloads by viewModel.downloads.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(downloads) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (item.isAudio) Icons.Default.MusicNote else Icons.Default.Movie,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.fileName, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(item.filePath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Image 5: settings/more menu */
@Composable
fun MoreScreen() {
    val menuItems = listOf(
        "Terminal" to Icons.Default.Terminal,
        "Logs" to Icons.Default.Description,
        "Download queue" to Icons.Default.GetApp,
        "Cookies" to Icons.Default.Settings,
        "Settings" to Icons.Default.Settings
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(menuItems) { (label, icon) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
        }
    }
}
