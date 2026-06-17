package com.example.eggtimer
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bang_lich_su")
data class NhatKy(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val loai_trung: String,
    val calories: Int,
    val protein: Int,
    val chat_beo: Int,
    val canxi: Int,
    val vitamin_d: Int,
    val thoi_gian: Long,
    val so_luong: Int = 1
)