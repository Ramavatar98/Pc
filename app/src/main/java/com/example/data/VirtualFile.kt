package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_files")
data class VirtualFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val path: String,       // Parent directory path, e.g., "/" or "/home/kali"
    val content: String,
    val isDirectory: Boolean,
    val isSystem: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
) {
    val fullPath: String get() = if (path == "/") "/$name" else "$path/$name"
}
