package com.example.eggtimer

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import android.util.Log

data class NhatKyDinhDuong(
    val loai: String,
    val calo: Int,
    val protein: Int,
    val chatBeo: Int,
    val canxi: Int,
    val vitaminD: Int,
    val thoiGian: Long = System.currentTimeMillis(),
    val soLuong: Int = 1
)

class EggTrangThai(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("egg_mst_prefs", Context.MODE_PRIVATE)
    private val database = TrungDatabase.getDatabase(application)
    private val eggDao = database.eggDao()
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Cài đặt Theme
    var isDarkMode = mutableStateOf(prefs.getBoolean("dark_mode", false))
    //prefs: Là đối tượng SharedPreferences (bộ nhớ đệm dạng Khóa - Giá trị) đã được khởi tạo ở đầu lớp EggTrangThai
    var followSystemTheme = mutableStateOf(prefs.getBoolean("follow_system_theme", true))

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        followSystemTheme.value = false
        prefs.edit { //edit { ... }, Kotlin tự động mở trình chỉnh sửa (Editor) và tự động gọi lệnh lưu ngầm (apply())
            putBoolean("dark_mode", enabled)//putboolin ghi đè
            putBoolean("follow_system_theme", false)
        }
    }

    fun toggleFollowSystem(enabled: Boolean) {
        followSystemTheme.value = enabled
        prefs.edit { putBoolean("follow_system_theme", enabled) }
    }

    // Thông tin người dùng
    var tenNguoiDung = mutableStateOf(prefs.getString("user_name", "") ?: "")
    var hoTen = mutableStateOf(prefs.getString("user_full_name", "") ?: "")
    var email = mutableStateOf(prefs.getString("user_email", "") ?: "")
    var gioiTinh = mutableStateOf(prefs.getString("user_gender", "Nam") ?: "Nam")
    var tuoi = mutableIntStateOf(prefs.getInt("user_age", 25))
    var chieuCao = mutableIntStateOf(prefs.getInt("user_height", 170))
    var canNang = mutableIntStateOf(prefs.getInt("user_weight", 60))
    var daNhapThongTin = mutableStateOf(prefs.getBoolean("onboarding_complete", false))

    init {
        // Tải dữ liệu từ database theo thời gian thực
        viewModelScope.launch {
            eggDao.getAllLogs().collectLatest { logs ->//sử dụng Flow (collectLatest). Thay vì trạng thái Loading/Success thủ công
               Log.d("Database", "Data loaded: ${logs.size} items")
                danhSachNhatKy.clear()

                // Reset các giá trị hôm nay
                soTrungAnHomNay.intValue = 0
                caloHienTai.intValue = 0
                proteinHienTai.intValue = 0
                chatBeoHienTai.intValue = 0
                canxiHienTai.intValue = 0
                vitaminDHienTai.intValue = 0

                // Reset lịch sử tuần
                for (i in 0..6) lichSuTuan[i] = 0
                var totalThisWeek = 0

                val now = Calendar.getInstance()
                val today = now.get(Calendar.DAY_OF_YEAR)
                val thisYear = now.get(Calendar.YEAR)

                logs.forEach { log ->

                    danhSachNhatKy.add(NhatKyDinhDuong(
                        log.loai_trung, log.calories, log.protein, log.chat_beo, log.canxi, log.vitamin_d, log.thoi_gian, log.so_luong
                    ))

                    val logCal = Calendar.getInstance().apply { timeInMillis = log.thoi_gian }

                    // Kiểm tra nếu là hôm nay
                    if (logCal.get(Calendar.DAY_OF_YEAR) == today && logCal.get(Calendar.YEAR) == thisYear) {
                        soTrungAnHomNay.intValue += log.so_luong
                        caloHienTai.intValue += log.calories
                        proteinHienTai.intValue += log.protein
                        chatBeoHienTai.intValue += log.chat_beo
                        canxiHienTai.intValue += log.canxi
                        vitaminDHienTai.intValue += log.vitamin_d
                    }

                    // Thống kê tuần
                    if (now.timeInMillis - log.thoi_gian < 7 * 24 * 60 * 60 * 1000L) {
                        totalThisWeek += log.so_luong
                        val dayOfWeek = logCal.get(Calendar.DAY_OF_WEEK)
                        val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                        if (index in 0..6) lichSuTuan[index] += log.so_luong
                    }
                }
                thongKeTuan.intValue = totalThisWeek
                trungBinhNgay.floatValue = totalThisWeek / 7f
            }
        }
    }

    fun luuThongTin(ten: String, hoTenVal: String, emailVal: String, gender: String, tuoiVal: Int, chieuCaoVal: Int, canNangVal: Int) {
        tenNguoiDung.value = ten
        hoTen.value = hoTenVal
        email.value = emailVal
        gioiTinh.value = gender
        tuoi.intValue = tuoiVal
        chieuCao.intValue = chieuCaoVal
        canNang.intValue = canNangVal
        daNhapThongTin.value = true
        prefs.edit {
            putString("user_name", ten)
            putString("user_full_name", hoTenVal)
            putString("user_email", emailVal)
            putString("user_gender", gender)
            putInt("user_age", tuoiVal)
            putInt("user_height", chieuCaoVal)
            putInt("user_weight", canNangVal)
            putBoolean("onboarding_complete", true)
        }
    }

    // Trạng thái điều hướng và lựa chọn
    var loaiTrung = mutableStateOf("Gà")
    var kichThuoc = mutableStateOf("Vừa")
    var nhietDo = mutableStateOf("Phòng")
    var doChin = mutableStateOf("Tái")
    
    // Dữ liệu hiển thị
    var soLuongTrongGio = mutableIntStateOf(prefs.getInt("eggs_in_basket", 7))
    var tongTrongGio = mutableIntStateOf(prefs.getInt("total_basket_capacity", 12))
    var thongKeTuan = mutableIntStateOf(0)
    var trungBinhNgay = mutableFloatStateOf(0f)
    var soTrungAnHomNay = mutableIntStateOf(0)
    
    // Dinh dưỡng hôm nay
    var caloHienTai = mutableIntStateOf(0)
    var proteinHienTai = mutableIntStateOf(0)
    var chatBeoHienTai = mutableIntStateOf(0)
    var canxiHienTai = mutableIntStateOf(0)
    var vitaminDHienTai = mutableIntStateOf(0)
    
    // Mục tiêu
    var mucTieuCalo = mutableIntStateOf(prefs.getInt("goal_calories", 1800))
    var mucTieuProtein = mutableIntStateOf(prefs.getInt("goal_protein", 75))
    var mucTieuChatBeo = mutableIntStateOf(60)
    var mucTieuCanxi = mutableIntStateOf(1000)
    var mucTieuVitaminD = mutableIntStateOf(600)

    // Danh sách nhật ký tiêu thụ
    var danhSachNhatKy = mutableStateListOf<NhatKyDinhDuong>()

    // Lịch sử tuần (Thứ 2 - Chủ nhật)
    var lichSuTuan = mutableStateListOf(0, 0, 0, 0, 0, 0, 0)

    // Thống kê Min/Max
    val maxTrongTuan = derivedStateOf { lichSuTuan.maxOrNull() ?: 0 }
    val minTrongTuan = derivedStateOf { 
        val list = lichSuTuan.filter { it > 0 }
        if (list.isEmpty()) 0 else list.minOrNull() ?: 0
    }

    fun themTrung(loai: String, qty: Int) {
        viewModelScope.launch {
            val nky = when(loai) {
                "Gà" -> NhatKy(
                    loai_trung = "Gà",
                    calories = 70 * qty,
                    protein = 6 * qty,
                    chat_beo = 5 * qty,
                    canxi = 30 * qty,
                    vitamin_d = 40 * qty,
                    thoi_gian = System.currentTimeMillis(),
                    so_luong = qty
                )
                else -> NhatKy(
                    loai_trung = "Cút",
                    calories = 14 * qty,
                    protein = 1 * qty,
                    chat_beo = 1 * qty,
                    canxi = 6 * qty,
                    vitamin_d = 8 * qty,
                    thoi_gian = System.currentTimeMillis(),
                    so_luong = qty
                )
            }
            eggDao.themLsTrung(nky)
            
            // Cập nhật số lượng trong giỏ (kết nối)
            soLuongTrongGio.intValue = (soLuongTrongGio.intValue - qty).coerceAtLeast(0)
            saveBasketData()
        }
    }

    fun napTrungVaoGio(qty: Int) {
        soLuongTrongGio.intValue += qty
        saveBasketData()
    }

    fun capNhatMucTieu(calo: Int, protein: Int, gioMax: Int, hienTai: Int) {
        mucTieuCalo.intValue = calo
        mucTieuProtein.intValue = protein
        tongTrongGio.intValue = gioMax
        soLuongTrongGio.intValue = hienTai
        prefs.edit {
            putInt("goal_calories", calo)
            putInt("goal_protein", protein)
            putInt("total_basket_capacity", gioMax)
            putInt("eggs_in_basket", hienTai)
        }
    }

    private fun saveBasketData() {
        prefs.edit {
            putInt("eggs_in_basket", soLuongTrongGio.intValue)
            putInt("total_basket_capacity", tongTrongGio.intValue)
        }
    }

    fun xoaToanBoDuLieu() {
        viewModelScope.launch {
            eggDao.clearHistory()
            prefs.edit { clear() }
            
            // Reset thông tin cá nhân
            tenNguoiDung.value = ""
            hoTen.value = ""
            email.value = ""
            gioiTinh.value = "Nam"
            tuoi.intValue = 25
            chieuCao.intValue = 170
            canNang.intValue = 60
            daNhapThongTin.value = false
            
            // Reset giỏ trứng
            soLuongTrongGio.intValue = 7
            tongTrongGio.intValue = 12
            
            dungDem()
            thoiGianConLaiState.intValue = 0
        }
    }

    // Bộ đếm thời gian
    private val thoiGianConLaiState = mutableIntStateOf(0)
    var thoiGianConLai: State<Int> = thoiGianConLaiState
    private val dangChayState = mutableStateOf(false)
    val dangChay: State<Boolean> = dangChayState
    private var jobDemNguoc: Job? = null//Kotlin Coroutines để đại diện cho một tác vụ đang chạy
    
    var showTimerFinishedDialog = mutableStateOf(false)

    fun calculateAndSetTimer() {
        var baseSeconds = 0
        when (loaiTrung.value) {
            "Cút" -> {
                baseSeconds = if (doChin.value == "Tái") 120 else 240
                baseSeconds += when (kichThuoc.value) {
                    "Nhỏ" -> -30
                    "To" -> 30
                    else -> 0
                }
                if (nhietDo.value == "Tủ lạnh") baseSeconds += 20
            }
            "Gà", "Cả hai" -> {
                baseSeconds = if (doChin.value == "Tái") 300 else 720
                baseSeconds += when (kichThuoc.value) {
                    "Nhỏ" -> -60
                    "To" -> 60
                    else -> 0
                }
                if (nhietDo.value == "Tủ lạnh") baseSeconds += 60
            }
        }
        thoiGianConLaiState.intValue = baseSeconds
    }

    fun batDauDem() {
        if (dangChayState.value || thoiGianConLaiState.intValue <= 0) return
        dangChayState.value = true
        showTimerFinishedDialog.value = false
        jobDemNguoc = viewModelScope.launch {
            while (thoiGianConLaiState.intValue > 0) {
                delay(1000L)
                thoiGianConLaiState.intValue -= 1
            }
            dangChayState.value = false
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        showTimerFinishedDialog.value = true
        startContinuousVibration()
    }

    private fun startContinuousVibration() {
        val pattern = longArrayOf(0, 500, 500) // Rung 500ms, nghỉ 500ms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    fun stopAlarm() {
        vibrator.cancel()
        showTimerFinishedDialog.value = false
    }

    fun dungDem() {
        jobDemNguoc?.cancel()
        dangChayState.value = false
        vibrator.cancel()
    }

    fun reset() {
        dungDem()
        showTimerFinishedDialog.value = false
        calculateAndSetTimer()
    }

    override fun onCleared() {
        super.onCleared()
        vibrator.cancel()
    }

    // Xuất dữ liệu
    fun getCsvData(): String {
        val sb = StringBuilder()
        sb.append("Loai,SoLuong,Calo,Protein,ChatBeo,Canxi,VitaminD,ThoiGian\n")
        danhSachNhatKy.forEach {
            sb.append("${it.loai},${it.soLuong},${it.calo},${it.protein},${it.chatBeo},${it.canxi},${it.vitaminD},${it.thoiGian}\n")
        }
        return sb.toString()
    }

    fun getJsonData(): String {
        val jsonArray = JSONArray()
        danhSachNhatKy.forEach {
            val obj = JSONObject()
            obj.put("loai", it.loai)
            obj.put("so_luong", it.soLuong)
            obj.put("calo", it.calo)
            obj.put("protein", it.protein)
            obj.put("chatBeo", it.chatBeo)
            obj.put("canxi", it.canxi)
            obj.put("vitaminD", it.vitaminD)
            obj.put("thoiGian", it.thoiGian)
            jsonArray.put(obj)
        }
        return jsonArray.toString(4)
    }
}
