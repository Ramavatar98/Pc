package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VirtualFileRepository(private val dao: VirtualFileDao) {

    val allFiles: Flow<List<VirtualFile>> = dao.getAllFilesFlow()

    fun getFilesByPathFlow(path: String): Flow<List<VirtualFile>> = dao.getFilesByPathFlow(path)

    suspend fun getFilesByPath(path: String): List<VirtualFile> = withContext(Dispatchers.IO) {
        dao.getFilesByPath(path)
    }

    suspend fun getFileByPathAndName(path: String, name: String): VirtualFile? = withContext(Dispatchers.IO) {
        dao.getFileByPathAndName(path, name)
    }

    suspend fun getFileById(id: Long): VirtualFile? = withContext(Dispatchers.IO) {
        dao.getFileById(id)
    }

    suspend fun insertFile(file: VirtualFile): Long = withContext(Dispatchers.IO) {
        dao.insertFile(file)
    }

    suspend fun createDirectory(path: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getFileByPathAndName(path, name)
        if (existing == null) {
            dao.insertFile(VirtualFile(name = name, path = path, content = "", isDirectory = true))
            true
        } else {
            false
        }
    }

    suspend fun createFile(path: String, name: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getFileByPathAndName(path, name)
        if (existing == null) {
            dao.insertFile(VirtualFile(name = name, path = path, content = content, isDirectory = false))
            true
        } else {
            if (!existing.isDirectory) {
                dao.insertFile(existing.copy(content = content, lastModified = System.currentTimeMillis()))
                true
            } else {
                false
            }
        }
    }

    suspend fun saveFileContent(id: Long, content: String) = withContext(Dispatchers.IO) {
        val existing = dao.getFileById(id)
        if (existing != null && !existing.isDirectory) {
            dao.insertFile(existing.copy(content = content, lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun deleteFile(path: String, name: String) = withContext(Dispatchers.IO) {
        dao.deleteFileByName(path, name)
    }

    suspend fun forceCreateInitialFilesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countFiles() == 0) {
            dao.insertFile(VirtualFile(name = "kali", path = "/home", content = "", isDirectory = true, isSystem = true))
            dao.insertFile(VirtualFile(name = "Desktop", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
            dao.insertFile(VirtualFile(name = "Documents", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
            dao.insertFile(VirtualFile(name = "Downloads", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
            dao.insertFile(VirtualFile(name = "Music", path = "/home/kali", content = "", isDirectory = true, isSystem = false))
            dao.insertFile(VirtualFile(
                name = "welcome_kali.txt",
                path = "/home/kali/Desktop",
                content = "==============================================\n✨ WELCOME TO KALI LINUX MINI-PC SIMULATOR ✨\n==============================================\n...",
                isDirectory = false
            ))
        }
    }
}
