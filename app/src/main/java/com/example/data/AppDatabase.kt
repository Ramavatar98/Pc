package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [VirtualFile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun virtualFileDao(): VirtualFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kali_pc_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.virtualFileDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: VirtualFileDao) {
                if (dao.countFiles() == 0) {
                    // Create base folder structure
                    dao.insertFile(VirtualFile(name = "kali", path = "/home", content = "", isDirectory = true, isSystem = true))
                    dao.insertFile(VirtualFile(name = "Desktop", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
                    dao.insertFile(VirtualFile(name = "Documents", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
                    dao.insertFile(VirtualFile(name = "Downloads", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
                    dao.insertFile(VirtualFile(name = "Music", path = "/home/kali", content = "", isDirectory = true, isSystem = false))

                    // Predefined text files and tool documentation in home
                    dao.insertFile(VirtualFile(
                        name = "welcome_kali.txt",
                        path = "/home/kali/Desktop",
                        content = """==============================================
✨ WELCOME TO KALI LINUX MINI-PC SIMULATOR ✨
==============================================

👉 Active Modules in this mini-PC:
- Terminal: Complete with commands (ls, cd, mkdir, cat, echo, write, nmap, hydra, rm, clear, systeminfo, cowsay, help, install). You can also run Custom Python/Shell commands!
- VS Code: Clean styled IDE with a custom terminal, file selector, code tabs, syntax highlights, and actual script execution!
- Chrome Web Browser: Built-in search, quick access buttons (YouTube, VS Code, Google, Wikipedia), bookmarks, and tab manager.
- VLC Music Streamer: Preloaded with lofi beats + stream URLs, visual play controls, sound spectrum analyzer, and offline file loader.
- Termux Security Center: Penetration tools simulator where you can execute virtual vulnerability assessments (Nmap Scanning, Password Brute-forcing, Metasploit payload simulator).
- App Store: Install or remove applications instantly, extending your system with custom software modules.

✨ System commands available:
- 'ls' (lists directories/files)
- 'cat [filename]' (reads document)
- 'echo "text" > [filename]' (creates text records)
- 'systeminfo' (specifies hardware/OS metrics)
- 'cowsay [msg]' (renders animated ascii terminal speaking cow!)

Stay Secure. Stay Anonymous.
        """,
                        isDirectory = false
                    ))

                    dao.insertFile(VirtualFile(
                        name = "network_scan_manual.txt",
                        path = "/home/kali/Documents",
                        content = """📌 KALI TERMINAL - EXPERT MANUAL 📌

How to execute security simulations:
1. Open the Termux or Terminal app.
2. Run 'nmap 192.168.1.1' to simulate a port scan.
3. Run 'hydra -u admin -p wordlist.txt ftp://192.168.1.5' to simulate FTP brute-forcing.
4. Run 'msfconsole' to configure target parameters and deploy simulated payloads.
5. Create your own custom bash scripts in VS Code, then run them directly in the Terminal!
""",
                        isDirectory = false
                    ))

                    dao.insertFile(VirtualFile(
                        name = "cyberpunk_vibes.mp3",
                        path = "/home/kali/Music",
                        content = "lofi_beats_01", // This is used to map to offline synthetic or direct lofi streams in VLC
                        isDirectory = false
                    ))
                }
            }
        }
    }
}
