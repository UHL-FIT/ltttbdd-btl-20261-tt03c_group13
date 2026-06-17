package com.example.eggtimer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EggDao {
    @Insert
    suspend fun themLsTrung(nkyTrung: NhatKy)//Điều này bắt buộc nó phải được chạy trong một Coroutine (trong dự án là viewModelScope.launch).

    @Query("SELECT * FROM bang_lich_su ORDER BY thoi_gian DESC")
    fun getAllLogs(): Flow<List<NhatKy>>

    @Query("DELETE FROM bang_lich_su")
    suspend fun clearHistory()
}
