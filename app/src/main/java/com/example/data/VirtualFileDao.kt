package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualFileDao {
    @Query("SELECT * FROM virtual_files ORDER BY isDirectory DESC, name ASC")
    fun getAllFilesFlow(): Flow<List<VirtualFile>>

    @Query("SELECT * FROM virtual_files WHERE path = :path ORDER BY isDirectory DESC, name ASC")
    fun getFilesByPathFlow(path: String): Flow<List<VirtualFile>>

    @Query("SELECT * FROM virtual_files WHERE path = :path ORDER BY isDirectory DESC, name ASC")
    suspend fun getFilesByPath(path: String): List<VirtualFile>

    @Query("SELECT * FROM virtual_files WHERE path = :path AND name = :name LIMIT 1")
    suspend fun getFileByPathAndName(path: String, name: String): VirtualFile?

    @Query("SELECT * FROM virtual_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): VirtualFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VirtualFile): Long

    @Delete
    suspend fun deleteFile(file: VirtualFile)

    @Query("DELETE FROM virtual_files WHERE path = :path AND name = :name")
    suspend fun deleteFileByName(path: String, name: String)

    @Query("SELECT COUNT(*) FROM virtual_files")
    suspend fun countFiles(): Int
}
