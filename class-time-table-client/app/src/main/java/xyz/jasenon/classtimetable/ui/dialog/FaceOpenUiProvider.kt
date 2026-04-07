package xyz.jasenon.classtimetable.ui.dialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ai.face.core.utils.FaceAICameraType
import com.ai.face.faceSearch.search.FaceSearchEngine
import com.ai.face.faceSearch.search.SearchProcessBuilder
import com.ai.face.faceSearch.search.SearchProcessCallBack
import com.ai.face.faceSearch.utils.FaceSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.config.DeviceRuntimeConfigObservable
import xyz.jasenon.classtimetable.ui.dialog.FaceOpenUiProvider.Companion.TAG
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 人脸识别开门 UI 提供者
 *
 * 本文件实现「人脸识别开门」的完整 UI 与逻辑：
 * - [FaceOpenUiProvider]：继承 [DoorOpeningUIProvider]，实现 [Ui]：圆形预览区、倒计时环、状态文案、人脸框叠加
 * - [FaceRecognitionCameraView]：CameraX 前置摄像头 + FaceSearchEngine 实时人脸检测与识别
 * - [GraphicOverlay]：在预览上绘制人脸框与姓名/分数
 * - [CircularProgressIndicator]：自定义圆形倒计时进度条
 *
 * 流程简述：
 * 1. 打开弹窗后启动前置摄像头与 FaceSearchEngine，每帧送入 runSearchWithImageProxy
 * 2. 检测到人脸后绘制框与状态；若匹配到库内人脸则 onRecognitionSuccess，延迟后调用 onClose + setCardVisible(false) 关门
 * 3. 倒计时在 LaunchedEffect 中每 100ms 递减，超时未识别则 onClose(Timeout) + setCardVisible(false)
 */
// ==================== 人脸识别开门 Provider ====================

/**
 * 人脸识别开门 UI 提供者
 *
 * 在 [DoorOpeningDialog] 的内容区绘制：圆形裁剪的摄像头预览、人脸框叠加、圆形倒计时环、状态文案（请面向摄像头/识别成功）。
 * 持有由 Card 传入的 [setCardVisible]，在识别成功或超时时调用 setCardVisible(false) 实现关门。
 *
 * @param title 弹窗标题
 * @param description 说明文案，在等待状态下显示于预览下方
 * @param modifier 内容区尺寸，默认 600.dp 正方形
 */
class FaceOpenUiProvider(
    title: String,
    description: String,
    modifier: Modifier = Modifier.size(600.dp)
) : DoorOpeningUIProvider(title, description, modifier) {

    /**
     * 绘制人脸识别开门的内容 UI
     *
     * 包含：圆形预览区（内嵌 [FaceRecognitionCameraView] + [GraphicOverlay] + [CircularProgressIndicator]）、
     * 状态文案、倒计时逻辑。识别成功或超时后调用 [onClose] 与 [setCardVisible](false) 关门。
     */
    @Composable
    override fun Ui(
        visible: Boolean,
        onTimeoutUpdate: (Long) -> Unit,
        setCardVisible: (Boolean) -> Unit
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        // 使用服务端下发的运行时配置
        val timeout = DeviceRuntimeConfigObservable.getTimeout()
        val facePrecision = DeviceRuntimeConfigObservable.getFacePrecision()
        val limitMillis = remember(timeout) { timeout * 1000L }

        var remaining by remember { mutableStateOf(limitMillis) }
        var recognitionStatus by remember { mutableStateOf<RecognitionStatus>(RecognitionStatus.Waiting) }
        var faceResults by remember { mutableStateOf<List<FaceSearchResult>>(emptyList()) }
        var imageSize by remember { mutableStateOf(Size(0f, 0f)) }
        val isRecognized = remember { AtomicBoolean(false) }

        // 将剩余时间回传给 Dialog 标题栏「Xs 后自动关闭」
        LaunchedEffect(remaining) { onTimeoutUpdate(remaining) }

        // 倒计时：每 100ms 递减，超时且未识别则关门
        LaunchedEffect(limitMillis) {
            while (remaining > 0L && !isRecognized.get()) {
                delay(100)
                remaining -= 100
            }
            if (!isRecognized.get() && remaining <= 0L) {
                setCardVisible(false)
            }
        }

        // 圆形进度条：已过去时间占比（0..1），从 12 点方向顺时针
        val progress = remember(remaining, limitMillis) {
            if (limitMillis > 0) 1.0f - ((limitMillis.toFloat() - remaining.toFloat()) / limitMillis.toFloat()) else 0.0f
        }
        val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(100), label = "")

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 圆形预览区：摄像头 + 人脸框 + 倒计时环 + 状态文案
            Box(
                modifier = Modifier.size(400.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FaceRecognitionCameraView(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    facePrecision = facePrecision,
                    onResults = { results ->
                        faceResults = results
                        if (results.isNotEmpty() && recognitionStatus == RecognitionStatus.Waiting) {
                            recognitionStatus = RecognitionStatus.Detecting
                        }
                    },
                    onImageSizeChanged = { width, height ->
                        if(imageSize.width == 0f) imageSize = Size(width.toFloat(), height.toFloat())
                    },
                    onRecognitionSuccess = { faceID, score ->
                        if (isRecognized.compareAndSet(false, true)) {
                            recognitionStatus = RecognitionStatus.Success
                            Log.d(TAG, "人脸识别成功: faceID=$faceID, score=$score")
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(500)
                                setCardVisible(false)
                            }
                        }
                    },
                    onRecognitionFailed = { if (!isRecognized.get()) recognitionStatus = RecognitionStatus.Failed },
                    modifier = Modifier.fillMaxSize()
                )

                GraphicOverlay(
                    results = faceResults,
                    imageWidth = imageSize.width.toInt(),
                    imageHeight = imageSize.height.toInt(),
                    isFrontCamera = true
                )

                CircularProgressIndicator(progress = animatedProgress, modifier = Modifier.fillMaxSize())

                when (recognitionStatus) {
                    RecognitionStatus.Waiting -> if (faceResults.isEmpty()) {
                        StatusText(text = "请面向摄像头", color = Color.White)
                    }
                    RecognitionStatus.Success -> StatusText(text = "识别成功", color = Color(0xFF4CAF50))
                    else -> Unit
                }
            }

            if (recognitionStatus == RecognitionStatus.Waiting) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    /** 圆形预览区内的状态文案（请面向摄像头 / 识别成功） */
    @Composable
    private fun StatusText(text: String, color: Color) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }

    override fun onConfirm() {}
    override fun onCancel() {}

    companion object {
        /** 日志与 [GraphicOverlay] 等内部引用使用 */
        const val TAG = "FaceOpenUiProvider"
    }
}

/** 人脸识别状态：等待人脸 / 检测中 / 识别成功 / 识别失败 */
private enum class RecognitionStatus { Waiting, Detecting, Success, Failed }

/**
 * 自定义圆形倒计时进度条
 *
 * 灰色底环 + 绿色弧（从 12 点方向顺时针，progress 0..1 表示已过去比例）
 */
@Composable
private fun CircularProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        drawCircle(color = Color.Gray.copy(alpha = 0.3f), radius = radius, center = center, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        if (progress > 0) {
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 人脸识别摄像头视图（CameraX + FaceSearchEngine）
 *
 * 使用前置摄像头，Preview 输出到 [PreviewView]，[ImageAnalysis] 每帧送入 FaceSearchEngine.runSearchWithImageProxy。
 * 检测结果通过 [onResults] 回传；若某帧匹配到库内人脸（faceName 非空且 score > 0）则调用 [onRecognitionSuccess]，
 * 否则无人脸时调用 [onRecognitionFailed]。[onImageSizeChanged] 用于 [GraphicOverlay] 的坐标换算。
 */
@Composable
private fun FaceRecognitionCameraView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    facePrecision: Float,
    onResults: (List<FaceSearchResult>) -> Unit,
    onImageSizeChanged: (Int, Int) -> Unit,
    onRecognitionSuccess: (String, Float) -> Unit,
    onRecognitionFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = context.findActivity()

    /** 遮罩关闭时用于解绑相机并停止人脸引擎，避免继续跑帧和回调 */
    val cameraProviderHolder = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analyzerExecutorHolder = remember { mutableStateOf<ExecutorService?>(null) }

    LaunchedEffect(facePrecision) {
        try {
            val searchProcessBuilder = SearchProcessBuilder.Builder(activity)
                .setLifecycleOwner(lifecycleOwner)
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
                .setThreshold(facePrecision)
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_M)
                .setMirror(true)
                .setProcessCallBack(object : SearchProcessCallBack() {
                    override fun onFaceDetected(result: MutableList<FaceSearchResult>) {
                        if (result.isNotEmpty()) {
                            Log.d(TAG, "[CALLBACK] onFaceDetected: ${result.size} faces found. First face rect: ${result.first().rect}")
                        }
                        onResults(result)
                        result.filter { it.faceName.isNotBlank() && it.faceScore > 0 }.maxByOrNull { it.faceScore }?.let {
                            onRecognitionSuccess(it.faceName, it.faceScore)
                        } ?: if (result.isEmpty()) onRecognitionFailed() else Unit
                    }
                    override fun onProcessTips(code: Int) { Log.d(TAG, "Process Tip: $code") }
                    override fun onLog(log: String) { Log.d(TAG, "FaceAI Log: $log") }
                    override fun onMostSimilar(faceID: String, score: Float, bitmap: Bitmap) {}
                })
                .create()

            FaceSearchEngine.getInstance().initSearchParams(searchProcessBuilder)

        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            onRecognitionFailed()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderHolder.value?.unbindAll()
            cameraProviderHolder.value = null
            analyzerExecutorHolder.value?.shutdown()
            analyzerExecutorHolder.value = null
            FaceSearchEngine.getInstance().stopSearchProcess()
            Log.d(TAG, "FaceRecognitionCameraView onDispose: unbind camera, stop face engine")
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = Executors.newSingleThreadExecutor()
            analyzerExecutorHolder.value = executor

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(previewView.display.rotation)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            onImageSizeChanged(imageProxy.width, imageProxy.height)
                            FaceSearchEngine.getInstance().runSearchWithImageProxy(imageProxy, 0)
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                    cameraProviderHolder.value = cameraProvider
                } catch (exc: Exception) {
                    Log.e(TAG, "CameraX binding failed", exc)
                    onRecognitionFailed()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

/**
 * 人脸框与姓名/分数叠加层
 *
 * 将 [results] 中的人脸矩形从图像坐标系（imageWidth x imageHeight）变换到当前 Canvas 尺寸，
 * 前置摄像头时水平翻转以镜像显示。已识别（faceName 非空）画绿色框并显示「姓名 | 分数」，未识别画蓝色框。
 */
@Composable
fun GraphicOverlay(
    results: List<FaceSearchResult>,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val boxPaint = remember { Paint().apply { style = PaintingStyle.Stroke; strokeWidth = 6f } }
    val textPaint = remember { android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 32f } }

    Canvas(modifier = modifier) {
        if (imageWidth == 0 || imageHeight == 0) return@Canvas

        if (results.isNotEmpty()) {
            Log.d(FaceOpenUiProvider.TAG, "[GraphicOverlay] Drawing ${results.size} boxes.")
        }

        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight

        drawIntoCanvas { canvas ->
            for (result in results) {
                val rect = result.rect
                val matrix = Matrix().apply {
                    preScale(scaleX, scaleY)
                    if (isFrontCamera) {
                        preScale(-1f, 1f)
                        postTranslate(size.width, 0f)
                    }
                }

                val rectF = RectF(rect)
                matrix.mapRect(rectF)

                boxPaint.color = if (result.faceName.isNotBlank()) Color(0xFF4CAF50) else Color.Blue
                canvas.nativeCanvas.drawRect(rectF, boxPaint.asFrameworkPaint())

                if (result.faceName.isNotBlank()) {
                    val text = "${result.faceName} | ${String.format("%.2f", result.faceScore)}"
                    canvas.nativeCanvas.drawText(text, rectF.left, rectF.top - 10, textPaint)
                }
            }
        }
    }
}

/** 将配置中的「保持时间」数值与单位转换为毫秒（本文件内人脸开门超时用） */
private fun toMillis(value: Int?, unit: TimeUnit?): Long {
    val v = value ?: 30
    val u = unit ?: TimeUnit.SECONDS
    return u.toMillis(v.toLong())
}

/** 从 Context 向上查找所属 Activity，用于 FaceSearchEngine 等需要 Activity 的 API */
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}
