package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VirtualFile
import com.example.viewmodel.DesktopApp
import com.example.viewmodel.DesktopWindow
import com.example.viewmodel.SystemViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Color scheme for Kali Linux Desktop Environment
val KaliDarkBackground = Color(0xFF0F111A)
val KaliMenuDark = Color(0xFF161925)
val KaliPrimaryTeal = Color(0xFF00FFCC)
val KaliAccentBlue = Color(0xFF0F80FF)
val KaliWindowBorder = Color(0xFF2E3440)
val KaliTerminalGreen = Color(0xFF00FF66)
val KaliTerminalGrey = Color(0xFF1E1E2E)
val KaliWindowHeader = Color(0xFF242938)
val CodeComment = Color(0xFF75715E)
val CodeKeyword = Color(0xFFF92672)
val CodeString = Color(0xFFE6DB74)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaliDesktopScreen(
    viewModel: SystemViewModel,
    onShutdown: () -> Unit
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentWorkDir.collectAsStateWithLifecycle()

    var isMenuOpen by remember { mutableStateOf(false) }
    var isPowerMenuOpen by remember { mutableStateOf(false) }

    // Run system tick daemon in desktop layout
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateTimeTicker()
            delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KaliDarkBackground)
            .testTag("kali_dekstop_box")
    ) {
        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight
        val density = LocalDensity.current.density

        // Desktop Wallpaper - Drawing a beautiful ambient gradient & high-contrast cyberpunk grids
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1a2238), KaliDarkBackground),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width,
                    tileMode = TileMode.Clamp
                )
            )

            // Tech lines / grids representation
            val gridStep = size.width / 12f
            for (i in 0..12) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    start = Offset(i * gridStep, 0f),
                    end = Offset(i * gridStep, size.height),
                    strokeWidth = 1f
                )
            }
            val horizontalSteps = (size.height / gridStep).toInt()
            for (j in 0..horizontalSteps) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    start = Offset(0f, j * gridStep),
                    end = Offset(size.width, j * gridStep),
                    strokeWidth = 1f
                )
            }

            // Draw a high-contrast cybernetic vector crest logo representation (styled like a central tech shields/dragon)
            val path = Path().apply {
                moveTo(size.width / 2f, size.height / 2f - 180f)
                quadraticTo(size.width / 2f + 120f, size.height / 2f - 60f, size.width / 2f + 160f, size.height / 2f + 80f)
                lineTo(size.width / 2f, size.height / 2f + 220f)
                lineTo(size.width / 2f - 160f, size.height / 2f + 80f)
                quadraticTo(size.width / 2f - 120f, size.height / 2f - 60f, size.width / 2f, size.height / 2f - 180f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0055FF).copy(alpha = 0.08f), Color(0xFF00FFCC).copy(alpha = 0.03f))
                )
            )
        }

        // Column for desktop top-panel and grid layer
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP STATUS PANEL (XFCE Style Kali Linux bar)
            TopStatusBar(
                viewModel = viewModel,
                onMenuToggle = { isMenuOpen = !isMenuOpen },
                onPowerToggle = { isPowerMenuOpen = !isPowerMenuOpen }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Desktop Shortcut Icons Grid (Filtered only installed apps)
                val shortcuts = installedApps.filter { it.isInstalled }
                DesktopAppIconGrid(
                    apps = shortcuts,
                    onAppLaunch = { appId ->
                        isMenuOpen = false
                        viewModel.launchApp(appId)
                    }
                )

                // Render dynamic windows stack
                viewModel.activeWindows.forEach { window ->
                    MovableWindow(
                        window = window,
                        viewModel = viewModel,
                        screenHeight = maxHeightDp,
                        screenWidth = maxWidthDp,
                        density = density
                    ) {
                        WindowAppDispatcher(appId = window.appId, viewModel = viewModel)
                    }
                }
            }
        }

        // WHISKER MENU PANEL (KALI MAIN LIST APPLICATION LAUNCHER)
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = fadeIn(animationSpec = spring()),
            exit = fadeOut(animationSpec = spring()),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 8.dp)
        ) {
            WhiskerApplicationMenu(
                apps = installedApps,
                onAppLaunch = { appId ->
                    isMenuOpen = false
                    viewModel.launchApp(appId)
                },
                onCloseMenu = { isMenuOpen = false }
            )
        }

        // POWER DRAWER DIALOG
        AnimatedVisibility(
            visible = isPowerMenuOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PowerDialog(
                onShutdown = onShutdown,
                onRestart = {
                    isPowerMenuOpen = false
                    isMenuOpen = false
                    viewModel.activeWindows.clear()
                    viewModel.executeTerminalCommand("clear")
                },
                onCancel = { isPowerMenuOpen = false }
            )
        }
    }
}

// XFCE Styled Top bar for Kali Linux Mini PC
@Composable
fun TopStatusBar(
    viewModel: SystemViewModel,
    onMenuToggle: () -> Unit,
    onPowerToggle: () -> Unit
) {
    val currentTime by viewModel.currentTime
    val cpuUsage by viewModel.systemCpuLoad
    val ramState by viewModel.systemRamUsage

    // Update Cpu workload simulation slightly for immersion
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            viewModel.systemCpuLoad.value = (10..38).random()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(KaliMenuDark)
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Kali Launcher Logo trigger & System Menu label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { onMenuToggle() }
                .padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Kali Icon",
                tint = KaliPrimaryTeal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Applications",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Launcher",
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }

        // Running tabs fast navigation rail indicators
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            viewModel.activeWindows.forEach { win ->
                val isFocused = viewModel.focusedAppId.value == win.appId
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) KaliAccentBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.05f))
                        .clickable { viewModel.focusWindow(win.appId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = win.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                }
            }
        }

        // Resource monitor metrics & Local Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // CPU Load
            Icon(
                imageVector = Icons.Default.DeveloperMode,
                contentDescription = "CPU Info",
                tint = KaliPrimaryTeal,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "CPU: $cpuUsage%",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 10.dp)
            )

            // RAM State
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = "RAM Info",
                tint = KaliAccentBlue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = ramState,
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Date / Time Ticker
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Clock Info",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(10.dp))
            // Shut Down command button
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power Trigger",
                tint = Color(0xFFFF5252),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onPowerToggle() }
            )
        }
    }
}

// Applications grid shortcut systems
@Composable
fun DesktopAppIconGrid(
    apps: List<DesktopApp>,
    onAppLaunch: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 85.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(apps) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAppLaunch(app.id) }
                    .padding(8.dp)
                    .testTag("app_icon_${app.id}")
            ) {
                // Customized cybernetic container box representing Kali XFCE icons
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2E3440), Color(0xFF1E2530))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (app.id == "terminal") KaliTerminalGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val appIcon = getSystemShortcutIcon(app.id)
                    Icon(
                        imageVector = appIcon,
                        contentDescription = app.name,
                        tint = when (app.id) {
                            "terminal" -> KaliTerminalGreen
                            "vscode" -> Color(0xFF007ACC)
                            "chrome" -> Color(0xFF4CAF50)
                            "vlc" -> Color(0xFFFF9800)
                            "termux" -> Color(0xFF8BC34A)
                            "file_explorer" -> Color(0xFFFFD54F)
                            else -> KaliPrimaryTeal
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, blurRadius = 4f)
                    )
                )
            }
        }
    }
}

// Map application string IDs to default standard material icon references
fun getSystemShortcutIcon(appId: String): ImageVector {
    return when (appId) {
        "terminal" -> Icons.Default.Terminal
        "vscode" -> Icons.Default.Code
        "chrome" -> Icons.Default.Language
        "vlc" -> Icons.Default.MusicNote
        "termux" -> Icons.Default.Security
        "file_explorer" -> Icons.Default.Folder
        "store" -> Icons.Default.Settings
        else -> Icons.Default.Launch
    }
}

// MOVABLE, DRAGGABLE, RESIZABLE DYNAMIC WINDOW COMPONENT PRESETS
@Composable
fun MovableWindow(
    window: DesktopWindow,
    viewModel: SystemViewModel,
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    density: Float,
    content: @Composable () -> Unit
) {
    val isFocused = viewModel.focusedAppId.value == window.appId

    // Floating offsets and sizes
    val currentX = remember { mutableStateOf(window.initialX) }
    val currentY = remember { mutableStateOf(window.initialY) }

    // Apply constraints triggers when pos values update on model
    LaunchedEffect(window.initialX, window.initialY) {
        currentX.value = window.initialX
        currentY.value = window.initialY
    }

    val windowModifier = if (window.isMaximized) {
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    } else {
        Modifier
            .size(window.width.dp, window.height.dp)
            .offset { IntOffset(
                (currentX.value * density).roundToInt(),
                (currentY.value * density).roundToInt()
            ) }
    }

    Card(
        modifier = windowModifier
            .padding(if (window.isMaximized) 0.dp else 4.dp)
            .shadow(
                elevation = if (isFocused) 16.dp else 6.dp, 
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.focusWindow(window.appId) }
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) KaliPrimaryTeal.copy(alpha = 0.8f) else KaliWindowBorder,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = KaliTerminalGrey)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // WINDOW HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(KaliWindowHeader)
                    .pointerInput(window.isMaximized) {
                        if (!window.isMaximized) {
                            detectDragGestures(
                                onDragStart = { viewModel.focusWindow(window.appId) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = currentX.value + (dragAmount.x / density)
                                    val newY = currentY.value + (dragAmount.y / density)
                                    currentX.value = newX
                                    currentY.value = newY
                                    viewModel.updateWindowPosition(window.appId, newX, newY)
                                }
                            )
                        }
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Window icon and descriptive name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getSystemShortcutIcon(window.appId),
                        contentDescription = "App win icon",
                        tint = KaliPrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = window.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Window control action buttons (Min, Max, Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Minimize button (yellow)
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB020))
                            .clickable { viewModel.closeWindow(window.appId) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Maximize toggle (green)
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                            .clickable { viewModel.toggleMaximizeWindow(window.appId) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Close (red icon)
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1744))
                            .clickable { viewModel.closeWindow(window.appId) }
                    )
                }
            }

            // WINDOW APP INTERIOR VIEW BODY
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(KaliDarkBackground)
            ) {
                content()
            }
        }
    }
}

// WINDOW APP DELEGATOR
@Composable
fun WindowAppDispatcher(appId: String, viewModel: SystemViewModel) {
    when (appId) {
        "terminal" -> TerminalAppSimulator(viewModel)
        "file_explorer" -> FileExplorerAppSimulator(viewModel)
        "vscode" -> VsCodeAppSimulator(viewModel)
        "chrome" -> ChromeBrowserAppSimulator(viewModel)
        "vlc" -> VlcMediaAppSimulator(viewModel)
        "termux" -> TermuxSecurityAppSimulator(viewModel)
        "store" -> SystemStoreAppSimulator(viewModel)
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "App simulator missing", color = Color.White)
            }
        }
    }
}

// ============================ KALI APP 1: TERMINAL SIMULATOR ============================
@Composable
fun TerminalAppSimulator(viewModel: SystemViewModel) {
    val logs = viewModel.terminalOutputLogs
    val currentInput by viewModel.terminalInputLine
    val currentPath by viewModel.currentWorkDir.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B10))
            .padding(6.dp)
    ) {
        // Log histories scroll lists
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs.toList()) { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("kali@") || line.startsWith("root@")) KaliPrimaryTeal else if (line.contains("FAILED") || line.contains("error")) Color(0xFFFF5252) else if (line.contains("SUCCESS") || line.contains("CRACKED")) Color(0xFF00FF66) else KaliTerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Active typing row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(0.5.dp, color = Color.White.copy(alpha = 0.12f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "kali@kali-pc:${currentPath}$ ",
                color = KaliPrimaryTeal,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            
            BasicTextField(
                value = currentInput,
                onValueChange = { viewModel.terminalInputLine.value = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.executeTerminalCommand(currentInput)
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .testTag("terminal_input_text")
            )

            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send Cmd",
                tint = KaliPrimaryTeal,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { viewModel.executeTerminalCommand(currentInput) }
            )
        }
    }
}

// ============================ KALI APP 2: FILE EXPLORER SIMULATOR ============================
@Composable
fun FileExplorerAppSimulator(viewModel: SystemViewModel) {
    val currentPath by viewModel.currentWorkDir.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    // Filter files strictly inside the active path
    val folderFiles = allFiles.filter { it.path == currentPath }

    var localFileNameInput by remember { mutableStateOf("") }
    var localDirNameInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Browser navigation head
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KaliWindowHeader)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val parts = currentPath.split("/")
                    if (parts.size > 2) {
                        viewModel.executeTerminalCommand("cd ..")
                    }
                },
                enabled = currentPath != "/"
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back dir",
                    tint = if (currentPath != "/") Color.White else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = currentPath,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Folder files grids selection
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF13151F))
                .padding(6.dp)
        ) {
            items(folderFiles) { file ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            if (file.isDirectory) {
                                viewModel.executeTerminalCommand("cd ${file.name}")
                            } else {
                                // Open file inside VS code instantly
                                viewModel.launchApp("vscode")
                                viewModel.openFileInCode(file)
                            }
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = "File item icon",
                        tint = if (file.isDirectory) Color(0xFFFFD54F) else KaliAccentBlue,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Quick creation actions panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KaliWindowHeader)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // New File textfield & button
            BasicTextField(
                value = localFileNameInput,
                onValueChange = { localFileNameInput = it },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black)
                    .padding(4.dp),
                decorationBox = { innerTextField ->
                    if (localFileNameInput.isEmpty()) {
                        Text("Add file (e.g. hack.py)", color = Color.Gray, fontSize = 10.sp)
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = {
                    if (localFileNameInput.trim().isNotEmpty()) {
                        viewModel.executeTerminalCommand("echo \"# New script created\" > ${localFileNameInput.trim()}")
                        localFileNameInput = ""
                    }
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaliAccentBlue)
            ) {
                Text("File +", fontSize = 10.sp)
            }
        }
    }
}

// ============================ KALI APP 3: VS CODE SIMULATOR ============================
@Composable
fun VsCodeAppSimulator(viewModel: SystemViewModel) {
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val openTabs = viewModel.openTabs
    val activeTabFileId by viewModel.activeTabFileId
    val textEditorContent by viewModel.vsCodeTextEditor
    val consoleOutput by viewModel.vsCodeConsoleOut

    // Filter only codes files from virtual DB partitions
    val searchableCodeFiles = allFiles.filter { !it.isDirectory }

    Row(modifier = Modifier.fillMaxSize()) {
        // Explorer Left list
        Column(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .background(Color(0xFF252526))
                .border(width = 0.5.dp, color = Color.Black)
        ) {
            Text(
                text = "EXPLORER",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(6.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(searchableCodeFiles) { f ->
                    val isActive = f.id == activeTabFileId
                    Text(
                        text = f.name,
                        color = if (isActive) KaliPrimaryTeal else Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openFileInCode(f) }
                            .background(if (isActive) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Editor & Runner workspace (split landscape vertical rows)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E))
        ) {
            // Editor action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF2D2D2D)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (openTabs.isEmpty()) {
                        Text(
                            text = "[No file open]",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    } else {
                        openTabs.forEach { tabFile ->
                            val isActive = tabFile.id == activeTabFileId
                            Row(
                                modifier = Modifier
                                    .background(if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D))
                                    .clickable { viewModel.openFileInCode(tabFile) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tabFile.name,
                                    color = if (isActive) Color.White else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "x",
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    modifier = Modifier.clickable {
                                        viewModel.openTabs.remove(tabFile)
                                        if (viewModel.activeTabFileId.value == tabFile.id) {
                                            val fallback = viewModel.openTabs.lastOrNull()
                                            if (fallback != null) {
                                                viewModel.openFileInCode(fallback)
                                            } else {
                                                viewModel.activeTabFileId.value = 0L
                                                viewModel.vsCodeTextEditor.value = ""
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // VS Code run buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.saveCodeContent() }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save file",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.executeCodeScript() }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Script",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Central editing pane
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
            ) {
                BasicTextField(
                    value = textEditorContent,
                    onValueChange = { viewModel.vsCodeTextEditor.value = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .testTag("vscode_editor_input"),
                    decorationBox = { innerTextField ->
                        if (textEditorContent.isEmpty()) {
                            Text(
                                text = "// Click a script file from explorer to start coding...\n// Use print() commands then hit Play arrow to compile logs.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // VS Code bottom build outputs console
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .border(width = 0.5.dp, color = Color.Black)
                    .padding(6.dp)
            ) {
                Text(
                    text = "CONSOLE OUTPUT",
                    color = Color.Green,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = consoleOutput,
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ============================ KALI APP 4: CHROME BROWSER SIMULATOR ============================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromeBrowserAppSimulator(viewModel: SystemViewModel) {
    val activeUrl = viewModel.chromeUrl.value
    var addressInput by remember { mutableStateOf(activeUrl) }

    LaunchedEffect(activeUrl) {
        addressInput = activeUrl
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Address controls panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF202124))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.chromeUrl.value = "https://www.google.com" }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home browser",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Real address bar field
            BasicTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black, shape = RoundedCornerShape(4.dp))
                    .padding(6.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    var finalUrl = addressInput.trim()
                    if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "https://$finalUrl"
                    }
                    viewModel.chromeUrl.value = finalUrl
                })
            )

            IconButton(onClick = {
                viewModel.chromeUrl.value = addressInput
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh browser",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick developer bookmarks row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2D30))
                .padding(vertical = 2.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val bookmarks = listOf(
                "Kali Docs" to "https://www.kali.org/docs/",
                "Google" to "https://www.google.com",
                "DuckDuckGo" to "https://duckduckgo.com",
                "GitHub" to "https://github.com",
                "YouTube Mobile" to "https://m.youtube.com"
            )

            bookmarks.forEach { (name, link) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable { viewModel.chromeUrl.value = link }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = name, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Real functional platform interactive WebView frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(activeUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != activeUrl) {
                        webView.loadUrl(activeUrl)
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("chrome_webview")
            )
        }
    }
}

// ============================ KALI APP 5: VLC MUSIC PLAYER SIMULATOR ============================
@Composable
fun VlcMediaAppSimulator(viewModel: SystemViewModel) {
    val playing by viewModel.vlcPlaying.collectAsStateWithLifecycle()
    val trackIdx by viewModel.currentTrackIdx.collectAsStateWithLifecycle()
    val currentTrack = viewModel.vlcPlaylist[trackIdx]

    // Create an animated infinite wave offset representing the audio spectrum
    val infiniteTransition = rememberInfiniteTransition(label = "audio_spectrum")
    val waveHeightModifier = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spectrums"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B1C10))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // VLC icon & title indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music disc",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = currentTrack.title,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentTrack.artist,
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Animated sound spectrum visualizer
        Row(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0..11) {
                // Generate relative offsets for visual spectrum complexity
                val relativeWeight = if (playing) {
                    val computedPhase = (waveHeightModifier.value + (i * 0.15f)) % 1.0f
                    if (computedPhase < 0.2f) 0.2f else computedPhase
                } else {
                    0.1f
                }

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(relativeWeight)
                        .background(Color(0xFFFF9800), shape = RoundedCornerShape(2.dp))
                )
            }
        }

        // Music player controllers center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Slider mock representing track progress loops
            var progress by remember { mutableStateOf(0.3f) }
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFFFF9800),
                    thumbColor = Color(0xFFFF9800)
                ),
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.prevTrack() }) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev track", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                FloatingActionButton(
                    onClick = { viewModel.toggleVlcPlaying() },
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play button",
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { viewModel.nextTrack() }) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next track", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ============================ KALI APP 6: TERMUX SECURITY PENETRATION SUITE ============================
@Composable
fun TermuxSecurityAppSimulator(viewModel: SystemViewModel) {
    val termuxLogs = viewModel.termuxTerminalLogs
    val isScanning by viewModel.isScanning
    val progress by viewModel.toolProgress
    val currentToolName by viewModel.activeRunningToolName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(10.dp)
    ) {
        Text(
            text = "TERMUX AUDITING & NETWORKS PANEL",
            color = KaliTerminalGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Select security script modules
        Text(
            text = "Select penetration routine to audit:",
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val toolsList = listOf("Nmap Port Tracker", "Hydra ftp cracker", "Metasploit Exploit console")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            toolsList.forEach { tool ->
                Button(
                    onClick = { viewModel.executeSecurityAudit(tool) },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentToolName == tool) KaliPrimaryTeal else Color(0xFF1F2937)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (tool.contains("Nmap")) "Nmap" else if (tool.contains("Hydra")) "Hydra" else "Metasploit",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scanning indicator active load elements
        if (isScanning) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Running audit: $currentToolName (${(progress * 100).toInt()}%)",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = KaliPrimaryTeal,
                    trackColor = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Audit live report logger
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                .padding(6.dp)
        ) {
            if (termuxLogs.isEmpty()) {
                Text(
                    text = "No scan active. Click a routine above (Nmap, Hydra, Metasploit) to generate virtual terminal cybersecurity scans and exploits.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(termuxLogs.toList()) { l ->
                        Text(
                            text = l,
                            color = if (l.contains("[SUCCESS]")) Color.Green else if (l.contains("[SCAN]")) KaliPrimaryTeal else Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================ KALI APP 7: SYSTEM APP STORE (PROVISION MANAGER) ============================
@Composable
fun SystemStoreAppSimulator(viewModel: SystemViewModel) {
    val appStoreList by viewModel.installedApps.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141721))
            .padding(10.dp)
    ) {
        Text(
            text = "MINI-PC APPLICATIONS MANAGER & STORAGE",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appStoreList) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KaliTerminalGrey),
                    border = BorderStroke(0.5.dp, color = Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Icon & Description
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSystemShortcutIcon(app.id),
                                    contentDescription = "app store win",
                                    tint = KaliPrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = app.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = app.description,
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Switch toggle controllers
                        if (app.isSystem) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFB020).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SYSTEM",
                                    color = Color(0xFFFFB020),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Switch(
                                checked = app.isInstalled,
                                onCheckedChange = { viewModel.toggleAppInstall(app.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = KaliPrimaryTeal,
                                    checkedTrackColor = KaliPrimaryTeal.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Application application Menu popup panel (XFCE Whisker menu)
@Composable
fun WhiskerApplicationMenu(
    apps: List<DesktopApp>,
    onAppLaunch: (String) -> Unit,
    onCloseMenu: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .heightIn(max = 350.dp)
            .border(width = 1.dp, color = KaliPrimaryTeal.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = KaliMenuDark)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // User root credentials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .clip(CircleShape)
                        .background(KaliAccentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "root@kali-pc", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Administrator VM", color = Color.Green, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Scroll lists of executable apps inside desktop system menu
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(apps) { app ->
                    val isInstalled = app.isInstalled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                if (isInstalled) {
                                    onAppLaunch(app.id)
                                }
                            }
                            .background(if (isInstalled) Color.Transparent else Color.White.copy(alpha = 0.02f))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getSystemShortcutIcon(app.id),
                            contentDescription = app.name,
                            tint = if (isInstalled) KaliPrimaryTeal else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = app.name,
                                color = if (isInstalled) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isInstalled) {
                                Text(
                                    text = "(Uninstalled)",
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            
            // Footer closes
            Text(
                text = "Close menu",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCloseMenu() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }
    }
}

// Shutdown desktop power overlay warning
@Composable
fun PowerDialog(
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .border(width = 1.dp, color = Color(0xFFFF5252).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = KaliTerminalGrey)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "shutdown symbol",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "KALI POWER OFF CONSOLE",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Are you sure you want to shut down or restart this Kali virtual mini-PC VM session?",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShutdown,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SHUTDOWN MACHINE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = KaliAccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESTART MACHINE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }

                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCEL", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
