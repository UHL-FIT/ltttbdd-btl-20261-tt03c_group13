package com.example.eggtimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eggtimer.ui.theme.EggTimerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val EterminationSansFont = FontFamily(Font(R.font.eterminationsans))

sealed class BNavItem(val route: String, val title: String, val icon: ImageVector) {
    object TrangThai : BNavItem("trangthai", "Trạng Thái", Icons.Filled.Home)
    object LuocTrung : BNavItem("luachon", "Luộc Trứng", Icons.Filled.PlayArrow)
    object DinhDuong : BNavItem("dinhduong", "Dinh Dưỡng", Icons.Filled.Info)
    object CaNhan : BNavItem("canhan", "Cá Nhân", Icons.Filled.Person)
    object BoDem : BNavItem("bodem", "Đồng Hồ", Icons.Filled.PlayArrow)
    object About : BNavItem("about", "Giới Thiệu", Icons.Filled.Info)
}

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivityLifeCycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            val viewModel: EggTrangThai = viewModel()
            val darkTheme = when {
                viewModel.followSystemTheme.value -> isSystemInDarkTheme()
                else -> viewModel.isDarkMode.value
            }
            EggTimerTheme(darkTheme = darkTheme) {
                EggApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Egg Timer Channel"
            val descriptionText = "Channel for Egg Timer notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("EGG_TIMER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun EggApp(viewModel: EggTrangThai) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(//đăng ký một "bộ khởi chạy" để nhận kết quả trả về từ hệ thống
        contract = ActivityResultContracts.RequestPermission(),//hợp đồng xin một quyền
        onResult = { _ -> }
    )

    LaunchedEffect(Unit) {//LaunchedEffect(Unit) — chạy khối code một lần duy nhất khi Composable xuất hiện
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)//launcher.launch(...) — bắn ra hộp thoại xin quyền thông báo
        }
    }

    if (viewModel.showTimerFinishedDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Đã xong!", fontFamily = EterminationSansFont, fontWeight = FontWeight.Bold) },
            text = { Text("Trứng của bạn đã luộc xong. Hãy tắt bếp ngay!", fontFamily = EterminationSansFont) },
            confirmButton = {
                Button(
                    onClick = { viewModel.stopAlarm() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Dừng", fontFamily = EterminationSansFont)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )

        LaunchedEffect(viewModel.showTimerFinishedDialog.value) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, "EGG_TIMER_CHANNEL")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("EggMst")
                .setContentText("Luộc trứng đã xong!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(1, notification)
        }
    }

    if (!viewModel.daNhapThongTin.value) {
        MHOnboarding(viewModel, snackbarHostState)
    } else {
        val navController = rememberNavController()
        val bNavItems = listOf(
            BNavItem.TrangThai,
            BNavItem.LuocTrung,
            BNavItem.DinhDuong,
            BNavItem.CaNhan
        )
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    bNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(26.dp)) },
                            label = { Text(item.title, fontSize = 11.sp, fontFamily = EterminationSansFont) },
                            selected = currentRoute == item.route || (item == BNavItem.LuocTrung && currentRoute == BNavItem.BoDem.route),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(//định nghĩa các điểm đến (composable).
                navController = navController,//Tạo và quản lý trạng thái lịch sử di chuyển giữa các màn hình trong ứng dụng.
                startDestination = BNavItem.TrangThai.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BNavItem.TrangThai.route) { MHTrangThai(viewModel, navController, snackbarHostState) }
                composable(BNavItem.LuocTrung.route) { MHLuaChon(viewModel, navController) }
                composable(BNavItem.BoDem.route) { MHBoDem(viewModel) }
                composable(BNavItem.DinhDuong.route) { MHDinhDuong(viewModel) }
                composable(BNavItem.CaNhan.route) { MHCaNhan(viewModel, navController, snackbarHostState) }
                composable(BNavItem.About.route) { MHAbout(viewModel, navController) }
            }
        }
    }
}

@Composable
fun MHOnboarding(viewModel: EggTrangThai, snackbarHostState: SnackbarHostState) {
    var tempName by remember { mutableStateOf("") }
    var tempGender by remember { mutableStateOf("Nam") }
    var nameError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(120.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(80.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Chào mừng bạn đến với EggMst",
                fontSize = 28.sp,
                fontFamily = EterminationSansFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            OutlinedTextField(
                value = tempName,
                onValueChange = {
                    tempName = it
                    nameError = it.isBlank()
                },
                label = { Text("Tên của bạn", fontFamily = EterminationSansFont) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = EterminationSansFont, fontSize = 18.sp),
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text("Tên không được để trống", color = MaterialTheme.colorScheme.error, fontFamily = EterminationSansFont)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Giới tính:", fontFamily = EterminationSansFont, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.onBackground)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                RadioButton(
                    selected = tempGender == "Nam",
                    onClick = { tempGender = "Nam" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Nam", fontFamily = EterminationSansFont, fontSize = 18.sp, modifier = Modifier.clickable { tempGender = "Nam" }, color = MaterialTheme.colorScheme.onBackground)

                Spacer(modifier = Modifier.width(32.dp))

                RadioButton(
                    selected = tempGender == "Nữ",
                    onClick = { tempGender = "Nữ" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Nữ", fontFamily = EterminationSansFont, fontSize = 18.sp, modifier = Modifier.clickable { tempGender = "Nữ" }, color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.luuThongTin(tempName, tempName, "", tempGender, 25, 170, 60)
                        scope.launch { snackbarHostState.showSnackbar("Chào mừng $tempName!") }
                    } else {
                        nameError = true
                        scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập tên hợp lệ") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = tempName.isNotBlank()
            ) {
                Text("Bắt đầu ngay", fontSize = 20.sp, fontFamily = EterminationSansFont)
            }
        }
    }
}

@Composable
fun MHTrangThai(viewModel: EggTrangThai, navController: NavController, snackbarHostState: SnackbarHostState) {
    val prefix = if (viewModel.gioiTinh.value == "Nam") "Anh" else "Chị"
    val fullName = "$prefix ${viewModel.tenNguoiDung.value}"
    var showAddEggDialog by remember { mutableStateOf(false) }
    var tempEggType by remember { mutableStateOf("Gà") }
    var tempQuantity by remember { mutableStateOf("1") }
    var qtyError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showAddEggDialog) {
        AlertDialog(
            onDismissRequest = { showAddEggDialog = false },
            title = { Text("Nhập Trứng", fontFamily = EterminationSansFont, color = Color.White) },
            containerColor = MaterialTheme.colorScheme.primary,
            text = {
                Column {
                    Text("Hôm nay bạn ăn loại trứng nào?", fontFamily = EterminationSansFont, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { tempEggType = "Gà" }) {
                        RadioButton(
                            selected = tempEggType == "Gà", 
                            onClick = { tempEggType = "Gà" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.6f))
                        )
                        Text("Trứng Gà", fontFamily = EterminationSansFont, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { tempEggType = "Cút" }) {
                        RadioButton(
                            selected = tempEggType == "Cút", 
                            onClick = { tempEggType = "Cút" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.6f))
                        )
                        Text("Trứng Cút", fontFamily = EterminationSansFont, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempQuantity,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                tempQuantity = it
                                qtyError = it.isEmpty() || it.toInt() <= 0
                            }
                        },
                        label = { Text("Số lượng", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = qtyError,
                        supportingText = {
                            if (qtyError) {
                                Text("Số lượng phải lớn hơn 0", color = Color.Yellow, fontSize = 12.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = Color.Yellow,
                            errorLabelColor = Color.Yellow,
                            errorSupportingTextColor = Color.Yellow
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = tempQuantity.toIntOrNull() ?: 0
                    if (qty > 0) {
                        viewModel.themTrung(tempEggType, qty)
                        showAddEggDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Đã thêm $qty trứng $tempEggType") }
                    } else {
                        qtyError = true
                    }
                }) {
                    Text("Xác nhận", fontFamily = EterminationSansFont, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEggDialog = false }) {
                    Text("Hủy", fontFamily = EterminationSansFont, color = Color.White.copy(alpha = 0.8f))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(54.dp), shadowElevation = 2.dp) {
                Box(contentAlignment = Alignment.Center) { 
                    Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "EggMst:", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
                Text(text = "Trứng & Dinh Dưỡng", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
            }
        }

        Text(text = "Xin chào, $fullName!", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Trạng Thái Trứng", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Tuần này: ${viewModel.thongKeTuan.intValue} trứng", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        Text("TB: ${String.format(Locale.getDefault(), "%.1f", viewModel.trungBinhNgay.floatValue)}/ngày", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(110.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Image(painter = painterResource(id = R.drawable.khaytrung), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("${viewModel.soLuongTrongGio.intValue} / ${viewModel.tongTrongGio.intValue}", fontWeight = FontWeight.Bold, fontSize = 26.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        Text("Trứng trong giỏ", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                    }
                    Box(modifier = Modifier.size(110.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.chao), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton("Nhập Trứng", "🥚", Modifier.weight(1f)) { showAddEggDialog = true }
            ActionButton("Thực Đơn", "📜", Modifier.weight(1f)) { /* TODO */ }
            ActionButton("Dinh Dưỡng", "📊", Modifier.weight(1f)) {
                navController.navigate(BNavItem.DinhDuong.route)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(BNavItem.DinhDuong.route) },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tóm Tắt Dinh Dưỡng", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                Text("Hôm nay bạn đã ăn ${viewModel.soTrungAnHomNay.intValue} trứng", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                Spacer(modifier = Modifier.height(20.dp))
                NutrientRow("Calo", viewModel.caloHienTai.intValue, viewModel.mucTieuCalo.intValue, "kcal", Color(0xFFFF9800))
                NutrientRow("Protein", viewModel.proteinHienTai.intValue, viewModel.mucTieuProtein.intValue, "g", MaterialTheme.colorScheme.primary)
                NutrientRow("Chất Béo", viewModel.chatBeoHienTai.intValue, viewModel.mucTieuChatBeo.intValue, "g", Color(0xFFFF9800))
                NutrientRow("Canxi", viewModel.canxiHienTai.intValue, viewModel.mucTieuCanxi.intValue, "mg", MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MHDinhDuong(viewModel: EggTrangThai) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(56.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(54.dp), shadowElevation = 2.dp) {
                    Box(contentAlignment = Alignment.Center) { 
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = "EggMst:", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
                    Text(text = "Trứng & Dinh Dưỡng", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "chi tiết dinh dưỡng", fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Tóm tắt hôm nay", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                            Text("Hôm nay bạn đã ăn ${viewModel.soTrungAnHomNay.intValue} trứng?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tuần này: ${viewModel.thongKeTuan.intValue} trứng", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                            Text("TB: ${String.format(Locale.getDefault(), "%.1f", viewModel.trungBinhNgay.floatValue)}/ngày", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    NutrientRow("Calo", viewModel.caloHienTai.intValue, viewModel.mucTieuCalo.intValue, "kcal", Color(0xFFFF9800))
                    NutrientRow("Protein", viewModel.proteinHienTai.intValue, viewModel.mucTieuProtein.intValue, "g", MaterialTheme.colorScheme.primary)
                    NutrientRow("Chất Béo", viewModel.chatBeoHienTai.intValue, viewModel.mucTieuChatBeo.intValue, "g", Color(0xFFFF9800))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientRow("Canxi", viewModel.canxiHienTai.intValue, viewModel.mucTieuCanxi.intValue, "mg", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientRow("Vitamin D", viewModel.vitaminDHienTai.intValue, viewModel.mucTieuVitaminD.intValue, "IU", Color(0xFFFF9800))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Lịch sử tiêu thụ", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    Text("Trứng đã ăn trong tuần", fontSize = 14.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        viewModel.lichSuTuan.forEachIndexed { index, count ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.width(20.dp).height((count * 15).coerceAtLeast(2).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                Text(days.getOrNull(index)?.take(1) ?: "?", fontSize = 10.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thấp nhất", fontSize = 12.sp, fontFamily = EterminationSansFont, color = Color.Gray)
                            Text("${viewModel.minTrongTuan.value} trứng", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cao nhất", fontSize = 12.sp, fontFamily = EterminationSansFont, color = Color.Gray)
                            Text("${viewModel.maxTrongTuan.value} trứng", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        item {
            Text("Nhật ký tiêu thụ", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }

        if (viewModel.danhSachNhatKy.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có nhật ký nào", fontFamily = EterminationSansFont, color = Color.Gray)
                    }
                }
            }
        }

        items(viewModel.danhSachNhatKy) { log ->
            NhatKyItem(log)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Nguồn Trứng", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    EggSourceItem("Trứng gà", R.drawable.trungga, "70kcal | 6g Protein", "Chất béo - 5g")
                    Spacer(modifier = Modifier.height(12.dp))
                    EggSourceItem("Trứng cút", R.drawable.trungcut, "14kcal | 1g Protein", "Vitamin D - 8IU")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun NhatKyItem(log: NhatKyDinhDuong) {
    val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
    val timeStr = sdf.format(Date(log.thoiGian))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (log.loai == "Gà") "🥚" else "🥚", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trứng ${log.loai}${if (log.soLuong > 1) " (x${log.soLuong})" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = EterminationSansFont,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(timeStr, fontSize = 12.sp, color = Color.Gray, fontFamily = EterminationSansFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${log.calo} kcal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFF9800), fontFamily = EterminationSansFont)
                Text("${log.protein}g Protein", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = EterminationSansFont)
            }
        }
    }
}

@Composable
fun EggSourceItem(name: String, imageRes: Int, info1: String, info2: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
            Text(info1, fontSize = 12.sp, fontFamily = EterminationSansFont, color = Color.Gray)
            Text(info2, fontSize = 11.sp, fontFamily = EterminationSansFont, color = Color.Gray)
        }
    }
}

@Composable
fun ActionButton(label: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.height(115.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp).fillMaxSize()
        ) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = EterminationSansFont,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun NutrientRow(label: String, current: Int, target: Int, unit: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
            Text("$current/$target$unit", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (target > 0) current.toFloat() / target.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun MHCaNhan(viewModel: EggTrangThai, navController: NavController, snackbarHostState: SnackbarHostState) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showEditDialog) {
        EditProfileDialog(viewModel, snackbarHostState) { showEditDialog = false }
    }

    if (showGoalDialog) {
        EditGoalsDialog(viewModel, snackbarHostState) { showGoalDialog = false }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xác nhận xóa", fontFamily = EterminationSansFont, fontWeight = FontWeight.Bold) },
            text = { Text("Tất cả dữ liệu cá nhân và lịch sử tiêu thụ sẽ bị xóa vĩnh viễn. Bạn có chắc chắn muốn tiếp tục?", fontFamily = EterminationSansFont) },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.xoaToanBoDuLieu()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa hết", color = Color.White, fontFamily = EterminationSansFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(40.dp)) { 
                        Box(contentAlignment = Alignment.Center) { 
                            Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(24.dp))
                        } 
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("EggMst:", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
                        Text("Trứng & Dinh Dưỡng", fontSize = 12.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Hồ sơ Cá Nhân", fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(viewModel.tenNguoiDung.value, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
                    Text(viewModel.hoTen.value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
                    Text(viewModel.email.value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontFamily = EterminationSansFont)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Thông Tin Cá Nhân", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItem("Tuổi", "${viewModel.tuoi.intValue}")
                    InfoItem("Giới tính", viewModel.gioiTinh.value)
                    InfoItem("Chiều cao", "${viewModel.chieuCao.intValue}cm")
                    InfoItem("Cân nặng", "${viewModel.canNang.intValue}kg")
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cài Đặt Mục Tiêu", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { showGoalDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit Goals", tint = MaterialTheme.colorScheme.primary) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    GoalItem("Mục tiêu Calo hàng ngày", viewModel.caloHienTai.intValue, viewModel.mucTieuCalo.intValue, "kcal", Color(0xFFFF9800))
                    Spacer(modifier = Modifier.height(16.dp))
                    GoalItem("Mục tiêu Protein hàng ngày", viewModel.proteinHienTai.intValue, viewModel.mucTieuProtein.intValue, "g", MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoItem("Sức chứa của giỏ", "${viewModel.tongTrongGio.intValue} trứng")
                    InfoItem("Trứng hiện có", "${viewModel.soLuongTrongGio.intValue} trứng")
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Giao Diện", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Theo hệ thống", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = viewModel.followSystemTheme.value, onCheckedChange = { viewModel.toggleFollowSystem(it) })
                    }
                    if (!viewModel.followSystemTheme.value) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Chế độ tối", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isDarkMode.value, onCheckedChange = { viewModel.toggleDarkMode(it) })
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).clickable { navController.navigate(BNavItem.About.route) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Giới thiệu ứng dụng", fontWeight = FontWeight.Medium, fontSize = 16.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { showDeleteConfirm = true }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                shape = RoundedCornerShape(16.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xóa Toàn Bộ Dữ Liệu", fontSize = 18.sp, fontFamily = EterminationSansFont)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MHAbout(viewModel: EggTrangThai, navController: NavController) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Giới Thiệu",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = EterminationSansFont,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(120.dp),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(80.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("EggMst", fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
        Text("Phiên bản 1.0.3", fontSize = 16.sp, color = Color.Gray, fontFamily = EterminationSansFont)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Ứng dụng EggMst giúp bạn theo dõi lượng trứng tiêu thụ hàng ngày, tính toán dinh dưỡng và hỗ trợ căn thời gian luộc trứng hoàn hảo.",
                    textAlign = TextAlign.Center,
                    fontFamily = EterminationSansFont,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Xuất dữ liệu tiêu thụ:", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { shareData(context, viewModel.getCsvData(), "egg_data.csv") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Xuất CSV", fontFamily = EterminationSansFont)
            }
            Button(
                onClick = { shareData(context, viewModel.getJsonData(), "egg_data.json") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Xuất JSON", fontFamily = EterminationSansFont)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Phát triển bởi:", fontSize = 14.sp, color = Color.Gray, fontFamily = EterminationSansFont)
        Text("Đội ngũ EggMst", fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text("© 2024 EggMst Team", fontSize = 12.sp, color = Color.Gray, fontFamily = EterminationSansFont)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun shareData(context: Context, data: String, filename: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, data)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Xuất dữ liệu $filename")
    context.startActivity(shareIntent)
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontFamily = EterminationSansFont)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun GoalItem(label: String, current: Int, value: Int, unit: String, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
            Text("$value$unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (value > 0) current.toFloat() / value.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun EditProfileDialog(viewModel: EggTrangThai, snackbarHostState: SnackbarHostState, onDismiss: () -> Unit) {
    var editTen by remember { mutableStateOf(viewModel.tenNguoiDung.value) }
    var editHoTen by remember { mutableStateOf(viewModel.hoTen.value) }
    var editEmail by remember { mutableStateOf(viewModel.email.value) }
    var editTuoi by remember { mutableStateOf(viewModel.tuoi.intValue.toString()) }
    var editChieuCao by remember { mutableStateOf(viewModel.chieuCao.intValue.toString()) }
    var editCanNang by remember { mutableStateOf(viewModel.canNang.intValue.toString()) }

    var showErrorAlert by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { showErrorAlert = false },
            title = { Text("Lỗi nhập liệu", fontFamily = EterminationSansFont) },
            text = { Text(errorMessage, fontFamily = EterminationSansFont) },
            confirmButton = {
                TextButton(onClick = { showErrorAlert = false }) {
                    Text("Đã hiểu", fontFamily = EterminationSansFont)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập nhật thông tin", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = editTen,
                    onValueChange = { editTen = it },
                    label = { Text("Tên hiển thị") },
                    isError = editTen.isBlank(),
                    supportingText = { if (editTen.isBlank()) Text("Không được để trống") }
                )
                OutlinedTextField(value = editHoTen, onValueChange = { editHoTen = it }, label = { Text("Họ và tên") })
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Email") },
                    isError = editEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail).matches(),
                    supportingText = { if (editEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail).matches()) Text("Email không hợp lệ") }
                )
                OutlinedTextField(value = editTuoi, onValueChange = { editTuoi = it }, label = { Text("Tuổi") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = editChieuCao, onValueChange = { editChieuCao = it }, label = { Text("Chiều cao (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = editCanNang, onValueChange = { editCanNang = it }, label = { Text("Cân nặng (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                val age = editTuoi.toIntOrNull() ?: 0
                val height = editChieuCao.toIntOrNull() ?: 0
                val weight = editCanNang.toIntOrNull() ?: 0

                if (editTen.isBlank()) {
                    errorMessage = "Tên hiển thị không được để trống."
                    showErrorAlert = true
                } else if (age <= 0 || age > 120) {
                    errorMessage = "Tuổi không hợp lệ (1-120)."
                    showErrorAlert = true
                } else if (height < 50 || height > 250) {
                    errorMessage = "Chiều cao không hợp lệ (50-250cm)."
                    showErrorAlert = true
                } else {
                    viewModel.luuThongTin(editTen, editHoTen, editEmail, viewModel.gioiTinh.value, age, height, weight)
                    scope.launch { snackbarHostState.showSnackbar("Đã cập nhật hồ sơ") }
                    onDismiss()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Lưu", fontFamily = EterminationSansFont)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface) } }
    )
}

@Composable
fun EditGoalsDialog(viewModel: EggTrangThai, snackbarHostState: SnackbarHostState, onDismiss: () -> Unit) {
    var editCalo by remember { mutableStateOf(viewModel.mucTieuCalo.intValue.toString()) }
    var editProtein by remember { mutableStateOf(viewModel.mucTieuProtein.intValue.toString()) }
    var editBasketMax by remember { mutableStateOf(viewModel.tongTrongGio.intValue.toString()) }
    var editBasketCurrent by remember { mutableStateOf(viewModel.soLuongTrongGio.intValue.toString()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Cài đặt mục tiêu", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = editCalo,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editCalo = it },
                    label = { Text("Mục tiêu Calo (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = editCalo.isEmpty() || (editCalo.toIntOrNull() ?: 0) < 500,
                    supportingText = { if (editCalo.isNotEmpty() && (editCalo.toIntOrNull() ?: 0) < 500) Text("Calo tối thiểu là 500") }
                )
                OutlinedTextField(
                    value = editProtein,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editProtein = it },
                    label = { Text("Mục tiêu Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = editBasketMax,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editBasketMax = it },
                    label = { Text("Sức chứa tối đa của giỏ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = editBasketCurrent,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editBasketCurrent = it },
                    label = { Text("Số trứng hiện có trong giỏ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = (editBasketCurrent.toIntOrNull() ?: 0) > (editBasketMax.toIntOrNull() ?: 0),
                    supportingText = { if ((editBasketCurrent.toIntOrNull() ?: 0) > (editBasketMax.toIntOrNull() ?: 0)) Text("Vượt quá sức chứa!") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val calo = editCalo.toIntOrNull() ?: 1800
                val protein = editProtein.toIntOrNull() ?: 75
                val basketMax = editBasketMax.toIntOrNull() ?: 12
                val basketCurrent = editBasketCurrent.toIntOrNull() ?: 7

                if (basketCurrent > basketMax) {
                    scope.launch { snackbarHostState.showSnackbar("Lỗi: Trứng hiện có không được lớn hơn sức chứa!") }
                } else {
                    viewModel.capNhatMucTieu(calo, protein, basketMax, basketCurrent)
                    scope.launch { snackbarHostState.showSnackbar("Mục tiêu đã được lưu") }
                    onDismiss()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Lưu", fontFamily = EterminationSansFont)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface) } }
    )
}

@Composable
fun MHLuaChon(viewModel: EggTrangThai, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.title_choose_egg), fontSize = 28.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp), fontFamily = EterminationSansFont, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)

        RadioButtonOption("Trứng Gà", selectedValue = viewModel.loaiTrung.value, value = "Gà", imageRes = R.drawable.trungga)
        { viewModel.loaiTrung.value = "Gà" }

        RadioButtonOption("Trứng Cút", selectedValue = viewModel.loaiTrung.value, value = "Cút", imageRes = R.drawable.trungcut)
        { viewModel.loaiTrung.value = "Cút" }

        RadioButtonOption ("Luộc Cả Hai Loại", selectedValue = viewModel.loaiTrung.value, value = "Cả hai", imageRes = R.drawable.trungga, imageRes2 = R.drawable.trungcut)
        { viewModel.loaiTrung.value = "Cả hai" }

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                viewModel.calculateAndSetTimer()
                navController.navigate(BNavItem.BoDem.route)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.btn_confirm), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont)
        }
    }
}

@Composable
fun RadioButtonOption(text: String, value: String, selectedValue: String, imageRes: Int, imageRes2: Int? = null, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if(value == selectedValue) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = if(value == selectedValue) 2.dp else 0.dp,
                color = if(value == selectedValue) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = (value == selectedValue),
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 8.dp), fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(imageRes), contentDescription = null, modifier = Modifier.size(44.dp))
                if (imageRes2 != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(painter = painterResource(imageRes2), contentDescription = null, modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

@Composable
fun MHBoDem(viewModel: EggTrangThai) {
    val tgConLai by viewModel.thoiGianConLai
    val dangChay by viewModel.dangChay
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Text(text = "Thời gian luộc trứng",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = EterminationSansFont,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(10.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(20.dp)
        ) {
            Text(text = String.format(Locale.getDefault(), "%02d:%02d", tgConLai / 60, tgConLai % 60),
                 fontSize = 64.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = EterminationSansFont)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Cài đặt nhanh:", fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Nhỏ", "Vừa", "To").forEach { size ->
                        FilterChip(
                            selected = viewModel.kichThuoc.value == size,
                            onClick = { viewModel.kichThuoc.value = size; viewModel.calculateAndSetTimer() },
                            label = { Text(size, fontFamily = EterminationSansFont, fontSize = 16.sp, color = if(viewModel.kichThuoc.value == size) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Tái", "Chín").forEach { chin ->
                        FilterChip(
                            selected = viewModel.doChin.value == chin,
                            onClick = { viewModel.doChin.value = chin; viewModel.calculateAndSetTimer() },
                            label = { Text(if(chin == "Tái") "Lòng đào" else "Chín kỹ", fontFamily = EterminationSansFont, fontSize = 16.sp, color = if(viewModel.doChin.value == chin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Phòng", "Tủ lạnh").forEach { temp ->
                        FilterChip(
                            selected = viewModel.nhietDo.value == temp,
                            onClick = { viewModel.nhietDo.value = temp; viewModel.calculateAndSetTimer() },
                            label = { Text(temp, fontFamily = EterminationSansFont, fontSize = 16.sp, color = if(viewModel.nhietDo.value == temp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (dangChay) viewModel.dungDem() else viewModel.batDauDem() },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (dangChay) "Tạm Dừng" else "Bắt Đầu", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont)
            }
            OutlinedButton(
                onClick = { viewModel.reset() },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Đặt Lại", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = EterminationSansFont, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
