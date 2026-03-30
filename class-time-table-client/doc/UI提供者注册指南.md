# UI提供者注册指南

本文档说明如何将 `PwdOpenUiProvider` 和其他UI提供者注册到 `DoorOpenUiProviderFactory` 中。

## 目录

1. [快速开始](#快速开始)
2. [注册方案](#注册方案)
3. [使用示例](#使用示例)
4. [最佳实践](#最佳实践)

---

## 快速开始

### 方案一：使用初始化器（推荐）

**步骤：**

1. **在 MainActivity 中调用初始化器**

```kotlin
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化所有UI提供者
        DoorOpenUiProviderInitializer.initialize()
        
        // ... 其他初始化代码
    }
}
```

2. **初始化器会自动注册所有提供者**

`DoorOpenUiProviderInitializer.initialize()` 会自动注册：
- `PwdOpenUiProvider` (密码开门)
- 其他已实现的提供者

---

## 注册方案

### 方案一：在 MainActivity 中注册（已实现）

**优点：**
- 简单直接
- 适合小型项目
- 代码集中

**缺点：**
- MainActivity 可能变得臃肿
- 不适合大型项目

**实现：**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用初始化器
        DoorOpenUiProviderInitializer.initialize()
        
        // 或者手动注册
        DoorOpenUiProviderFactory.register(
            DoorOpeningType.PASSWORD,
            PwdOpenUiProvider("密码开门", "请输入6位数字密码")
        )
    }
}
```

### 方案二：创建 Application 类（推荐用于大型项目）

**优点：**
- 更规范的架构
- 适合多个组件需要初始化
- 生命周期管理更清晰

**步骤：**

1. **创建 Application 类**

```kotlin
package xyz.jasenon.classtimetable

import android.app.Application
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderInitializer

class ClassTimeTableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化UI提供者
        DoorOpenUiProviderInitializer.initialize()
        
        // 其他全局初始化...
    }
}
```

2. **在 AndroidManifest.xml 中注册**

```xml
<application
    android:name=".ClassTimeTableApplication"
    ...>
    <!-- ... -->
</application>
```

### 方案三：延迟初始化（Lazy Initialization）

**优点：**
- 按需加载，节省内存
- 首次使用时才初始化

**实现：**

```kotlin
object DoorOpenUiProviderFactory {
    private val factory: MutableMap<DoorOpeningType, DoorOpeningUIProvider> =
        ConcurrentHashMap()
    
    private var initialized = false
    
    fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    DoorOpenUiProviderInitializer.initialize()
                    initialized = true
                }
            }
        }
    }
    
    fun get(type: DoorOpeningType): DoorOpeningUIProvider {
        ensureInitialized()  // 确保已初始化
        return factory[type] 
            ?: throw RuntimeException("DoorOpenType: $type 尚未注册!")
    }
}
```

### 方案四：使用伴生对象初始化

**优点：**
- 类加载时自动初始化
- 无需手动调用

**实现：**

```kotlin
class PwdOpenUiProvider(title: String, description: String) : DoorOpeningUIProvider(title, description) {
    
    companion object {
        init {
            // 类加载时自动注册
            DoorOpenUiProviderFactory.register(
                DoorOpeningType.PASSWORD,
                PwdOpenUiProvider("密码开门", "请输入6位数字密码")
            )
        }
    }
    
    // ... 其他代码
}
```

**注意：** 这种方式需要确保类被加载，可能需要显式引用。

---

## 使用示例

### 从 Factory 获取 UI 提供者

```kotlin
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderFactory
import xyz.jasenon.classtimetable.ui.dialog.DoorOpeningType
import xyz.jasenon.classtimetable.ui.dialog.DoorOpeningDialog

@Composable
fun SomeScreen() {
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    Button(onClick = { showPasswordDialog = true }) {
        Text("密码开门")
    }
    
    if (showPasswordDialog) {
        // 从 Factory 获取 UI 提供者
        val uiProvider = DoorOpenUiProviderFactory.get(DoorOpeningType.PASSWORD)
        
        DoorOpeningDialog(
            visible = showPasswordDialog,
            onDismiss = { showPasswordDialog = false },
            type = DoorOpeningType.PASSWORD,
            uiProvider = uiProvider
        )
    }
}
```

### 检查是否已注册

```kotlin
if (DoorOpenUiProviderFactory.isRegistered(DoorOpeningType.PASSWORD)) {
    val provider = DoorOpenUiProviderFactory.get(DoorOpeningType.PASSWORD)
    // 使用 provider
} else {
    // 处理未注册的情况
}
```

---

## 最佳实践

### 1. 统一使用初始化器

**推荐：** 使用 `DoorOpenUiProviderInitializer.initialize()` 统一管理所有注册。

**原因：**
- 代码集中，易于维护
- 避免重复注册
- 便于调试和测试

### 2. 在应用启动时初始化

**推荐：** 在 `Application.onCreate()` 或 `MainActivity.onCreate()` 中初始化。

**原因：**
- 确保在使用前已注册
- 避免运行时错误

### 3. 使用非空类型

**已修复：** Factory 现在使用非空类型，避免空指针异常。

```kotlin
// ✅ 正确
fun register(type: DoorOpeningType, provider: DoorOpeningUIProvider)

// ❌ 错误（已修复）
fun register(type: DoorOpeningType?, provider: DoorOpeningUIProvider?)
```

### 4. 添加日志记录

**已实现：** Factory 和 Initializer 都添加了日志，便于调试。

### 5. 错误处理

**已实现：** `get()` 方法会在未注册时抛出明确的异常。

```kotlin
try {
    val provider = DoorOpenUiProviderFactory.get(DoorOpeningType.PASSWORD)
} catch (e: RuntimeException) {
    Log.e(TAG, "UI提供者未注册", e)
    // 处理错误
}
```

---

## 添加新的UI提供者

### 步骤

1. **创建UI提供者类**

```kotlin
class FaceRecognitionUiProvider(
    title: String, 
    description: String
) : DoorOpeningUIProvider(title, description) {
    
    @Composable
    override fun BuildContent(
        type: DoorOpeningType,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        // 实现UI内容
    }
}
```

2. **在初始化器中注册**

```kotlin
object DoorOpenUiProviderInitializer {
    fun initialize() {
        // ... 现有注册
        
        // 注册新的提供者
        DoorOpenUiProviderFactory.register(
            DoorOpeningType.FACE_RECOGNITION,
            FaceRecognitionUiProvider(
                title = "人脸识别开门",
                description = "请面向摄像头"
            )
        )
    }
}
```

---

## 总结

**当前实现：**
- ✅ Factory 已修复（非空类型、日志、错误处理）
- ✅ 初始化器已创建
- ✅ MainActivity 中已调用初始化

**使用方式：**
1. 应用启动时自动注册所有UI提供者
2. 通过 `DoorOpenUiProviderFactory.get(type)` 获取提供者
3. 使用获取的提供者创建对话框

**下一步：**
- 实现其他开门方式的UI提供者（人脸识别、二维码、刷卡）
- 在初始化器中注册它们

---

**文档版本：** 1.0  
**最后更新：** 2024年



