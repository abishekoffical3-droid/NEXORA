package com.example.ui.screens.food

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ai.DetectedFoodItem
import com.example.ai.FoodNutritionAnalysis
import com.example.ai.FoodNutritionCalculator
import com.example.ai.RickyAiService
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FoodAnalyzerCameraScreen(
    rickyAiService: RickyAiService,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission needed to scan food meals", Toast.LENGTH_SHORT).show()
        }
    }

    // CameraX state
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    // Analysis & Editing state
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isManualMealMode by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<FoodNutritionAnalysis?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeScanToken by remember { mutableIntStateOf(0) }

    var editingItem by remember { mutableStateOf<DetectedFoodItem?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    fun resetScan() {
        activeScanToken++
        capturedBitmap = null
        isManualMealMode = false
        isAnalyzing = false
        analysisResult = null
        errorMessage = null
        editingItem = null
        showAddItemDialog = false
    }

    fun startFoodAnalysis(bitmap: Bitmap) {
        val token = ++activeScanToken
        capturedBitmap = bitmap
        isManualMealMode = false
        isAnalyzing = true
        analysisResult = null
        errorMessage = null

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    rickyAiService.analyzeFoodNutrition(bitmap)
                }
                if (token == activeScanToken) {
                    analysisResult = result
                }
            } catch (e: Exception) {
                if (token == activeScanToken) {
                    errorMessage = e.localizedMessage
                        ?: "Could not verify food nutrition from image. Check lighting or connection and retry."
                }
            } finally {
                if (token == activeScanToken) {
                    isAnalyzing = false
                }
            }
        }
    }

    // Photo picker fallback (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUri(context, uri) }
                if (bitmap != null) {
                    startFoodAnalysis(bitmap)
                } else {
                    Toast.makeText(context, "Unable to read selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler(enabled = capturedBitmap != null || isManualMealMode) {
        resetScan()
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Rebind CameraX whenever lensFacing changes while on camera preview
    LaunchedEffect(lensFacing, hasCameraPermission, capturedBitmap, isManualMealMode) {
        val view = previewView
        if (!hasCameraPermission || view == null || capturedBitmap != null || isManualMealMode) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                boundCamera = cam
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    isTorchOn = false
                } else {
                    cam.cameraControl.enableTorch(isTorchOn)
                }
            } catch (e: Exception) {
                Log.e("CameraX", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedBitmap == null && !isManualMealMode) {
            // Live CameraX Preview Mode
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { view ->
                            previewView = view
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission Denied Notice
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant camera access to scan your meal plate, pick a food photo from your gallery, or build a USDA meal manually.",
                        color = Color(0xFFA1A1B5),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                    ) {
                        Text("Grant Permission", color = Color.White)
                    }
                }
            }

            // Top Camera HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(40.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x88181824))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = NexoraPurpleHighlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEXORA Food Vision",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasCameraPermission && lensFacing == CameraSelector.LENS_FACING_BACK) {
                        IconButton(
                            onClick = {
                                val nextTorch = !isTorchOn
                                isTorchOn = nextTorch
                                boundCamera?.cameraControl?.enableTorch(nextTorch)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isTorchOn) NexoraPurplePrimary else Color(0x66000000))
                                .testTag("torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = if (isTorchOn) "Turn Flashlight Off" else "Turn Flashlight On",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Camera Viewfinder Guidance Overlay
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(2.dp, NexoraPurpleHighlight.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Frame full plate or bowl in clear light",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x70000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Bottom Shutter & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp, start = 28.dp, end = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zero-Permission Photo Gallery Picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x88181824))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .testTag("pick_food_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Choose meal photo from gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NexoraPurplePrimary, NexoraNeonPurpleGlow)
                            )
                        )
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = hasCameraPermission) {
                            val capture = imageCapture
                            if (capture != null) {
                                capture.takePicture(
                                    captureExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = imageProxyToBitmap(image)
                                            image.close()
                                            scope.launch(Dispatchers.Main) {
                                                startFoodAnalysis(bmp)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraX", "Capture failed", exception)
                                            scope.launch(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Capture failed. Try again or pick from gallery.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Camera initializing...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("capture_food_photo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Manual USDA Meal Builder (Replaces fake gray-bitmap demo button)
                IconButton(
                    onClick = {
                        isManualMealMode = true
                        capturedBitmap = null
                        errorMessage = null
                        analysisResult = null
                        showAddItemDialog = true
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x88181824))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .testTag("manual_food_entry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Build meal manually from USDA database",
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Results & Interactive Nutrition Dashboard View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { resetScan() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Camera",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nutrition Dashboard",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { resetScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Scan", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Captured Food Image Preview with Active Analysis Overlay
                capturedBitmap?.let { bmp ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Scanned Food Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (isAnalyzing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xAA0A0A14)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NexoraPurpleHighlight,
                                            modifier = Modifier.size(46.dp),
                                            strokeWidth = 3.5.dp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Analyzing visible ingredients, gram weights & USDA macros...",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Error State Banner (with Retry + USDA Manual Fallback)
                errorMessage?.let { err ->
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = NexoraErrorRed.copy(alpha = 0.55f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Analysis error",
                                        tint = NexoraErrorRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Food Analysis Incomplete",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    capturedBitmap?.let { bmp ->
                                        Button(
                                            onClick = { startFoodAnalysis(bmp) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showAddItemDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add USDA Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Manual Mode Empty Prompt before first item is added
                if (isManualMealMode && analysisResult == null && !isAnalyzing) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Manual USDA Meal Builder",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add weighed ingredients from the verified USDA FoodData Central table or enter custom macros.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Button(
                                    onClick = { showAddItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Food Ingredient", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Upgraded Dashboard Cards (Charts, Per-Item Breakdown, Atwater & Uncertainty Badges, Edit Controls)
                analysisResult?.let { food ->
                    item {
                        FoodNutritionDashboardCards(
                            food = food,
                            onEditItem = { item -> editingItem = item },
                            onDeleteItem = { itemId ->
                                val updated = food.items.filterNot { it.id == itemId }
                                analysisResult = FoodNutritionCalculator.recalculateMealAnalysis(food, updated)
                            },
                            onOpenAddItem = { showAddItemDialog = true },
                            onLogMeal = {
                                Toast.makeText(
                                    context,
                                    "Logged '${food.foodName}' (${food.calories} kcal, ${food.proteinGrams}g P) to NEXORA!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Edit Portion / Cooking State / Manual Macro Dialog
    editingItem?.let { targetItem ->
        EditFoodItemDialog(
            item = targetItem,
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                val current = analysisResult
                if (current != null) {
                    val updatedList = current.items.map {
                        if (it.id == updatedItem.id) updatedItem else it
                    }
                    analysisResult = FoodNutritionCalculator.recalculateMealAnalysis(current, updatedList)
                }
                editingItem = null
            }
        )
    }

    // Add Ingredient Dialog (USDA Database or Custom)
    if (showAddItemDialog) {
        AddFoodIngredientDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { newItem ->
                val current = analysisResult
                analysisResult = if (current != null && current.items.isNotEmpty()) {
                    FoodNutritionCalculator.recalculateMealAnalysis(current, current.items + newItem)
                } else {
                    FoodNutritionCalculator.buildMealFromItems(
                        mealName = newItem.name,
                        rawItems = listOf(newItem),
                        healthScore = 82,
                        summary = "Built from verified ingredient composition and validated via Atwater 4-4-9 energy rules.",
                        rickyTip = "Add another ingredient or tap edit on any row to fine-tune gram weights."
                    )
                }
                errorMessage = null
                showAddItemDialog = false
            }
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = image.imageInfo.rotationDegrees
    val rotated = if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } else {
        decoded
    }
    return downscaleForVisionIfNeeded(rotated, 1600)
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
                val w = info.size.width.coerceAtLeast(1)
                val h = info.size.height.coerceAtLeast(1)
                val maxDim = max(w, h)
                if (maxDim > 1600) {
                    val scale = 1600f / maxDim.toFloat()
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
            downscaleForVisionIfNeeded(decoded, 1600)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            if (maxDim > 1600) {
                while (maxDim / sampleSize > 1600) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null
            downscaleForVisionIfNeeded(decoded, 1600)
        }
    } catch (e: Exception) {
        Log.e("FoodScanner", "Failed to load gallery image", e)
        null
    }
}

private fun downscaleForVisionIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    val currentMax = max(w, h)
    if (currentMax <= maxDim) return bitmap
    val scale = maxDim.toFloat() / currentMax.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (w * scale).roundToInt().coerceAtLeast(1),
        (h * scale).roundToInt().coerceAtLeast(1),
        true
    )
}
