package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VirtualFile
import com.example.data.VirtualFileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Represents an installed app on this Kali Mini-PC
data class DesktopApp(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String, // Icon reference
    val isSystem: Boolean = false,
    val isInstalled: Boolean = true
)

// Representing active window status on the Desktop
data class DesktopWindow(
    val appId: String,
    val title: String,
    val initialX: Float,
    val initialY: Float,
    val width: Int = 360,
    val height: Int = 450,
    val isMaximized: Boolean = false,
    val idx: Int = 0 // Depth layer focus
)

// VLC playlist track structure
data class MusicTrack(
    val title: String,
    val artist: String,
    val source: String,
    val length: String,
    val url: String = ""
)

class SystemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VirtualFileRepository

    // File System States
    val allFiles: StateFlow<List<VirtualFile>>
    private val _currentWorkDir = MutableStateFlow("/home/kali")
    val currentWorkDir = _currentWorkDir.asStateFlow()

    // App List (Interactive Store)
    private val _installedApps = MutableStateFlow<List<DesktopApp>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    // Multi-Window Desk States
    val activeWindows = mutableStateListOf<DesktopWindow>()
    val focusedAppId = mutableStateOf("")

    // Desktop Customizers (Wallpapers, Audio triggers, System stats)
    val wallpaperId = mutableStateOf(0) // Default Kali metallic dragon background
    val systemCpuLoad = mutableStateOf(14)
    val systemRamUsage = mutableStateOf("1.4GB / 4.0GB")
    val currentTime = mutableStateOf("")

    // VLC Player States
    private val _vlcPlaying = MutableStateFlow(false)
    val vlcPlaying = _vlcPlaying.asStateFlow()
    
    private val _currentTrackIdx = MutableStateFlow(0)
    val currentTrackIdx = _currentTrackIdx.asStateFlow()

    val vlcPlaylist = listOf(
        MusicTrack("Resonance of Shadows", "Lofi Dev", "Synthetic Track A", "03:15"),
        MusicTrack("Midnight Pentesting", "Cypherpunk Beats", "Synthetic Track B", "02:45"),
        MusicTrack("Hack the Planet", "Kali Synth", "Synthetic Track C", "04:10"),
        MusicTrack("VS Code Dark Theme", "Cozy Devs", "Synthetic Track D", "03:00")
    )

    // VS Code States
    val openTabs = mutableStateListOf<VirtualFile>()
    val activeTabFileId = mutableStateOf(0L)
    val vsCodeTextEditor = mutableStateOf("")
    val vsCodeConsoleOut = mutableStateOf("")

    // Chrome Web States
    val chromeUrl = mutableStateOf("https://www.google.com")
    val chromeHistory = mutableStateListOf("https://www.google.com")
    val chromeTabs = mutableStateListOf("Home", "YouTube", "VS Code Online")
    val chromeActiveTabIdx = mutableStateOf(0)

    // Termux/Security Center Tools & Output
    val termuxTerminalLogs = mutableStateListOf<String>()
    val activeRunningToolName = mutableStateOf("") // e.g. "nmap", "hydra", "metasploit"
    val toolProgress = mutableStateOf(0f)
    val isScanning = mutableStateOf(false)

    // Shell Terminal Command Line States
    val terminalInputLine = mutableStateOf("")
    val terminalOutputLogs = mutableStateListOf<String>()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = VirtualFileRepository(database.virtualFileDao())
        allFiles = repository.allFiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize lists & metadata
        loadInstalledApps()
        updateTimeTicker()

        terminalOutputLogs.add("==================================================")
        terminalOutputLogs.add("Kali GNU/Linux Rolling 2026.1 (Mini PC Simulator)")
        terminalOutputLogs.add("Logged in as: root@kali-pc")
        terminalOutputLogs.add("Type 'help' to list available Linux commands.")
        terminalOutputLogs.add("==================================================")
        terminalOutputLogs.add("kali@kali-pc:~$ ")
    }

    private fun loadInstalledApps() {
        val apps = listOf(
            DesktopApp("terminal", "Kali Terminal", "Run bash shells, custom file commands, cowsay animations & system metrics.", "ic_launcher_foreground", isSystem = true),
            DesktopApp("file_explorer", "File Explorer", "Navigate modern virtual partitions, edit file text, preview folders.", "ic_launcher_foreground", isSystem = true),
            DesktopApp("vscode", "Visual Studio Code", "Professional text simulator with actual file tab saves, console logs, running scripts.", "ic_launcher_foreground"),
            DesktopApp("chrome", "Chrome Browser", "Web exploration engine complete with bookmarks, video streamers, search hubs.", "ic_launcher_foreground"),
            DesktopApp("vlc", "VLC Music", "Tactile media stream controller with spectrum animations, progress bars, sound packs.", "ic_launcher_foreground"),
            DesktopApp("termux", "Termux Security Suite", "Penetration terminal simulation, vulnerability auditing tools (Hydra, Metasploit, Nmap).", "ic_launcher_foreground"),
            DesktopApp("store", "Mini-PC App Store", "Provision apps, check system load, uninstall modules, view system specifications.", "ic_launcher_foreground", isSystem = true)
        )
        _installedApps.value = apps
    }

    // Toggle app installation via virtual store
    fun toggleAppInstall(appId: String) {
        val updated = _installedApps.value.map { app ->
            if (app.id == appId) {
                // Toggle state
                app.copy(isInstalled = !app.isInstalled)
            } else {
                app
            }
        }
        _installedApps.value = updated
        
        // If app was uninstalled, close its window too
        val isNowInstalled = updated.firstOrNull { it.id == appId }?.isInstalled ?: false
        if (!isNowInstalled) {
            closeWindow(appId)
        }
    }

    // Time ticker daemon simulation
    fun updateTimeTicker() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        currentTime.value = sdf.format(Date())
    }

    // Window Controllers
    fun launchApp(appId: String) {
        // Confirm if application is installed
        val appObj = _installedApps.value.firstOrNull { it.id == appId }
        if (appObj != null && !appObj.isInstalled) {
            // Not installed, show quick warning inside central terminal
            terminalOutputLogs.add("System error: App '${appObj.name}' is uninstalled. Please install it from the Store.")
            return
        }

        val existingIndex = activeWindows.indexOfFirst { it.appId == appId }
        if (existingIndex != -1) {
            // Bring to focus to top layer
            val cached = activeWindows[existingIndex]
            activeWindows.removeAt(existingIndex)
            activeWindows.add(cached.copy(idx = activeWindows.size))
            focusedAppId.value = appId
        } else {
            // Open as new window with staggered starting offset
            val offsetMultiplier = (activeWindows.size % 4) * 45f
            val title = appObj?.name ?: "Application"
            activeWindows.add(
                DesktopWindow(
                    appId = appId,
                    title = title,
                    initialX = 40f + offsetMultiplier,
                    initialY = 120f + offsetMultiplier,
                    idx = activeWindows.size
                )
            )
            focusedAppId.value = appId
        }
    }

    fun closeWindow(appId: String) {
        activeWindows.removeAll { it.appId == appId }
        if (focusedAppId.value == appId) {
            focusedAppId.value = activeWindows.lastOrNull()?.appId ?: ""
        }
    }

    fun toggleMaximizeWindow(appId: String) {
        val index = activeWindows.indexOfFirst { it.appId == appId }
        if (index != -1) {
            val element = activeWindows[index]
            activeWindows[index] = element.copy(isMaximized = !element.isMaximized)
        }
    }

    fun updateWindowPosition(appId: String, newX: Float, newY: Float) {
        val index = activeWindows.indexOfFirst { it.appId == appId }
        if (index != -1) {
            val element = activeWindows[index]
            // Constraint coordinate check to preserve drag handles on deck
            val cappedY = if (newY < 40f) 40f else newY
            activeWindows[index] = element.copy(initialX = newX, initialY = cappedY)
        }
    }

    fun focusWindow(appId: String) {
        val index = activeWindows.indexOfFirst { it.appId == appId }
        if (index != -1 && index != activeWindows.size - 1) {
            val element = activeWindows[index]
            activeWindows.removeAt(index)
            activeWindows.add(element)
        }
        focusedAppId.value = appId
    }

    // Terminal Commands Executor (Standard CLI Simulator)
    fun executeTerminalCommand(inputText: String) {
        val command = inputText.trim()
        if (command.isEmpty()) return

        terminalOutputLogs.add(command)

        val args = command.split(" ")
        val baseCmd = args[0].lowercase()

        viewModelScope.launch {
            when (baseCmd) {
                "help" -> {
                    terminalOutputLogs.add("=================== KALI TERMINAL MANUAL ===================")
                    terminalOutputLogs.add("ls              - List directories & files in current workdir")
                    terminalOutputLogs.add("cd [path]       - Change directory path (e.g., cd Desktop)")
                    terminalOutputLogs.add("mkdir [name]    - Create dynamic virtual directories")
                    terminalOutputLogs.add("cat [file]      - Read contents of normal textual file")
                    terminalOutputLogs.add("echo [msg] > f  - Write custom files (e.g. echo Code > index.py)")
                    terminalOutputLogs.add("cowsay [msg]    - Draw classic ascii cows speaking custom lines")
                    terminalOutputLogs.add("systeminfo      - Print real system virtualization specifications")
                    terminalOutputLogs.add("nmap [IP]       - Network trace simulation audits ports")
                    terminalOutputLogs.add("hydra [targ]    - Password brute-forcing script simulation")
                    terminalOutputLogs.add("clear           - Wipe trace outputs of this terminal screen")
                    terminalOutputLogs.add("appstore        - Quick launch Mini PC Store config tool")
                    terminalOutputLogs.add("install [app]   - Install store apps directly (vlc, vscode, chrome, termux)")
                    terminalOutputLogs.add("============================================================")
                }
                "ls" -> {
                    val files = repository.getFilesByPath(_currentWorkDir.value)
                    if (files.isEmpty()) {
                        terminalOutputLogs.add("(empty directory)")
                    } else {
                        val formatted = files.joinToString("   ") { 
                            if (it.isDirectory) "[DIR] ${it.name}" else it.name 
                        }
                        terminalOutputLogs.add(formatted)
                    }
                }
                "cd" -> {
                    if (args.size < 2) {
                        _currentWorkDir.value = "/home/kali"
                    } else {
                        val target = args[1]
                        if (target == "..") {
                            val parts = _currentWorkDir.value.split("/")
                            if (parts.size > 2) {
                                _currentWorkDir.value = parts.subList(0, parts.size - 1).joinToString("/")
                            } else {
                                _currentWorkDir.value = "/"
                            }
                        } else {
                            // Check if folder exists
                            val matches = repository.getFilesByPath(_currentWorkDir.value)
                            val targetDir = matches.firstOrNull { it.name.lowercase() == target.lowercase() && it.isDirectory }
                            if (targetDir != null) {
                                _currentWorkDir.value = if (_currentWorkDir.value == "/") "/${targetDir.name}" else "${_currentWorkDir.value}/${targetDir.name}"
                            } else {
                                terminalOutputLogs.add("cd: no such file or directory: $target")
                            }
                        }
                    }
                }
                "mkdir" -> {
                    if (args.size < 2) {
                        terminalOutputLogs.add("mkdir: missing operand")
                    } else {
                        val name = args[1]
                        val success = repository.createDirectory(_currentWorkDir.value, name)
                        if (success) {
                            terminalOutputLogs.add("Directory '$name' made successfully.")
                        } else {
                            terminalOutputLogs.add("mkdir: cannot create directory '$name': File exists")
                        }
                    }
                }
                "cat" -> {
                    if (args.size < 2) {
                        terminalOutputLogs.add("cat: missing filename")
                    } else {
                        val name = args[1]
                        val file = repository.getFileByPathAndName(_currentWorkDir.value, name)
                        if (file != null) {
                            if (file.isDirectory) {
                                terminalOutputLogs.add("cat: $name: Is a directory")
                            } else {
                                terminalOutputLogs.add(file.content)
                            }
                        } else {
                            terminalOutputLogs.add("cat: $name: No such file in directories")
                        }
                    }
                }
                "echo" -> {
                    // Quick crude regex parse for redirect operators
                    // e.g. echo "class Hello:" > main.py
                    val lineStr = command.substring(4).trim()
                    if (lineStr.contains(">")) {
                        val parts = lineStr.split(">")
                        val content = parts[0].replace("\"", "").trim()
                        val filename = parts[1].trim()
                        if (filename.isNotEmpty()) {
                            val success = repository.createFile(_currentWorkDir.value, filename, content)
                            if (success) {
                                terminalOutputLogs.add("Recorded written code contents to '$filename'.")
                            } else {
                                terminalOutputLogs.add("echo error: Target file block blocked or is directory")
                            }
                        } else {
                            terminalOutputLogs.add("echo error: Invalid redirect output filename reference")
                        }
                    } else {
                        terminalOutputLogs.add(lineStr)
                    }
                }
                "cowsay" -> {
                    val msg = if (args.size < 2) "I am a Kali developer cow! Moo!" else command.substring(7)
                    terminalOutputLogs.add(" ________________________________________")
                    terminalOutputLogs.add("< $msg >")
                    terminalOutputLogs.add(" ----------------------------------------")
                    terminalOutputLogs.add("        \\   ^__^")
                    terminalOutputLogs.add("         \\  (oo)\\_______")
                    terminalOutputLogs.add("            (__)\\       )\\/\\")
                    terminalOutputLogs.add("                ||----w |")
                    terminalOutputLogs.add("                ||     ||")
                }
                "systeminfo" -> {
                    terminalOutputLogs.add("🔧 KALI MINI-PC VIRTUAL MACHINE METRICS 🔧")
                    terminalOutputLogs.add("----------------------------------------")
                    terminalOutputLogs.add("OS: Kali GNU/Linux Rolling x86_64")
                    terminalOutputLogs.add("Kernel: Simulator build v2.6.46")
                    terminalOutputLogs.add("Uptime: Continuous Virtual Docker Engine")
                    terminalOutputLogs.add("CPU load: ${systemCpuLoad.value}% (4 simulated cores)")
                    terminalOutputLogs.add("Memory state: ${systemRamUsage.value} total")
                    terminalOutputLogs.add("Host System: Android Edge Client")
                    terminalOutputLogs.add("Networking: NAT via Loopback Virtual IP")
                    terminalOutputLogs.add("----------------------------------------")
                }
                "clear" -> {
                    terminalOutputLogs.clear()
                    terminalOutputLogs.add("Logged in as: root@kali-pc")
                }
                "nmap" -> {
                    val targetIP = if (args.size < 2) "127.0.0.1" else args[1]
                    terminalOutputLogs.add("Initializing virtual port tracker scan on target IP: $targetIP")
                    terminalOutputLogs.add("Scanning ports... (Simulating background raw socket packets)")
                    terminalOutputLogs.add("Nmap scan report for $targetIP (NAT bridge mode)")
                    terminalOutputLogs.add("PORT     STATE   SERVICE")
                    terminalOutputLogs.add("21/tcp   open    ftp (proftpd v1.3.1)")
                    terminalOutputLogs.add("22/tcp   open    ssh (OpenSSH 8.4p1)")
                    terminalOutputLogs.add("80/tcp   open    http (nginx/1.18.0)")
                    terminalOutputLogs.add("443/tcp  open    https (secure secure ssl)")
                    terminalOutputLogs.add("8080/tcp open    http-proxy (Apache Tomcat)")
                    terminalOutputLogs.add("Nmap done: 1 IP address scanned in 0.52 seconds.")
                }
                "hydra" -> {
                    terminalOutputLogs.add("Starting Hydra password guesser on ssh://root (v1.0)...")
                    terminalOutputLogs.add("[ATTEMPT] root / password123 -> FAILED")
                    terminalOutputLogs.add("[ATTEMPT] root / 123456      -> FAILED")
                    terminalOutputLogs.add("[ATTEMPT] root / admin       -> FAILED")
                    terminalOutputLogs.add("[SUCCESS] root / toor        -> PASSWORD CRACKED!")
                    terminalOutputLogs.add("Hydra completed cracker script. Target cracked successfully.")
                }
                "appstore" -> {
                    launchApp("store")
                }
                "install" -> {
                    if (args.size < 2) {
                        terminalOutputLogs.add("install: Specify target app string (vlc, vscode, chrome, termux)")
                    } else {
                        val installTarget = args[1].lowercase()
                        val matchId = when (installTarget) {
                            "vlc" -> "vlc"
                            "vscode" -> "vscode"
                            "vscode-online" -> "vscode"
                            "chrome" -> "chrome"
                            "browser" -> "chrome"
                            "termux" -> "termux"
                            "security" -> "termux"
                            else -> null
                        }
                        if (matchId != null) {
                            val activeApp = _installedApps.value.firstOrNull { it.id == matchId }
                            if (activeApp != null) {
                                if (activeApp.isInstalled) {
                                    terminalOutputLogs.add("App '${activeApp.name}' is already installed.")
                                } else {
                                    toggleAppInstall(matchId)
                                    terminalOutputLogs.add("Success: App '${activeApp.name}' installed on Kali Desktop.")
                                }
                            }
                        } else {
                            terminalOutputLogs.add("install: Package '$installTarget' not found in virtual sources repository.")
                        }
                    }
                }
                "rm" -> {
                    if (args.size < 2) {
                        terminalOutputLogs.add("rm: missing operand")
                    } else {
                        val name = args[1]
                        repository.deleteFile(_currentWorkDir.value, name)
                        terminalOutputLogs.add("Removed file '$name' from partitions.")
                    }
                }
                else -> {
                    terminalOutputLogs.add("bash: command not found: $baseCmd")
                    terminalOutputLogs.add("Try running 'help' to review simulated commands.")
                }
            }
            terminalOutputLogs.add("kali@kali-pc:${_currentWorkDir.value}$ ")
        }
        terminalInputLine.value = ""
    }

    // VLC media helper
    fun toggleVlcPlaying() {
        _vlcPlaying.value = !_vlcPlaying.value
    }

    fun nextTrack() {
        val nextIdx = (_currentTrackIdx.value + 1) % vlcPlaylist.size
        _currentTrackIdx.value = nextIdx
    }

    fun prevTrack() {
        var prevIdx = _currentTrackIdx.value - 1
        if (prevIdx < 0) prevIdx = vlcPlaylist.size - 1
        _currentTrackIdx.value = prevIdx
    }

    // VS Code Actions
    fun openFileInCode(file: VirtualFile) {
        if (openTabs.none { it.id == file.id }) {
            openTabs.add(file)
        }
        activeTabFileId.value = file.id
        vsCodeTextEditor.value = file.content
        vsCodeConsoleOut.value = "Loaded file: ${file.name}"
    }

    fun saveCodeContent() {
        val fileId = activeTabFileId.value
        if (fileId != 0L) {
            viewModelScope.launch {
                val currentText = vsCodeTextEditor.value
                repository.saveFileContent(fileId, currentText)
                vsCodeConsoleOut.value = "Saved successfully at ${currentTime.value}."
                // Update copy in open list tab
                val index = openTabs.indexOfFirst { it.id == fileId }
                if (index != -1) {
                    val updatedFile = openTabs[index].copy(content = currentText)
                    openTabs[index] = updatedFile
                }
            }
        } else {
            vsCodeConsoleOut.value = "Error: Create or select a file to save."
        }
    }

    fun executeCodeScript() {
        val codeText = vsCodeTextEditor.value
        vsCodeConsoleOut.value = "Compiling and running script...\n"
        viewModelScope.launch {
            // Simulated interactive code execution analysis
            kotlinx.coroutines.delay(1000)
            if (codeText.contains("print") || codeText.contains("println")) {
                val output = StringBuilder()
                // Crude simulator searching for print lines
                codeText.lines().forEach { line ->
                    if (line.contains("print")) {
                        val matched = Regex("""print\((.*)\)""").find(line) ?: Regex("""println\((.*)\)""").find(line)
                        if (matched != null) {
                            val innerStr = matched.groupValues[1].replace("\"", "").replace("'", "")
                            output.append(innerStr).append("\n")
                        } else {
                            output.append("Parsed statement output.\n")
                        }
                    }
                }
                if (output.isEmpty()) {
                    output.append("Process completed with exit code 0.")
                }
                vsCodeConsoleOut.value = "[Execution Output]:\n$output"
            } else if (codeText.trim().isEmpty()) {
                vsCodeConsoleOut.value = "Empty script payload. Nothing to execute."
            } else {
                vsCodeConsoleOut.value = "[Execution Output]:\nCompiler detected safe code tokens.\nSystem processes running locally.\nProcess completed successfully (exit code 0)."
            }
        }
    }

    // Termux auditor simulator
    fun executeSecurityAudit(tool: String) {
        if (isScanning.value) return
        
        isScanning.value = true
        toolProgress.value = 0f
        activeRunningToolName.value = tool
        termuxTerminalLogs.clear()
        
        termuxTerminalLogs.add("[STDOUT] Starting virtual penetration auditor model run for $tool...")
        termuxTerminalLogs.add("[STDOUT] Securing socket wrappers and scanning host loopback IPs...")

        viewModelScope.launch {
            for (step in 1..10) {
                kotlinx.coroutines.delay(400)
                toolProgress.value = step / 10f
                when (tool) {
                    "Nmap Port Tracker" -> {
                        when (step) {
                            3 -> termuxTerminalLogs.add("[SCAN] Audit detected ports 22/tcp SSH, 80/tcp HTTP, 443/tcp HTTPS open on endpoint.")
                            6 -> termuxTerminalLogs.add("[SCAN] Audited vulnerability scanner CVE checks running packages.")
                            9 -> termuxTerminalLogs.add("[SCAN] TLS versions match. TLS 1.0/1.1 is currently disabled (Good patch!).")
                        }
                    }
                    "Hydra ftp cracker" -> {
                        when (step) {
                            3 -> termuxTerminalLogs.add("[ATTEMPT] root / toor -> Password failed.")
                            6 -> termuxTerminalLogs.add("[ATTEMPT] admin / root12 -> Password failed.")
                            9 -> termuxTerminalLogs.add("[SUCCESS] admin / admin123 -> FTP login successful! Cracked.")
                        }
                    }
                    "Metasploit Exploit console" -> {
                        when (step) {
                            3 -> termuxTerminalLogs.add("[CONFIG] Selected windows/smb/ms08_067_netapi exploit template.")
                            6 -> termuxTerminalLogs.add("[PAYLOAD] Generated simple shell listener inject payload wrapper.")
                            9 -> termuxTerminalLogs.add("[EXPLOIT] Tunnel socket established! Interactive shell ready for sessions.")
                        }
                    }
                }
            }
            termuxTerminalLogs.add("[STDOUT] Script routine finalized. Simulator returned success (Exit 0).")
            isScanning.value = false
        }
    }
}
