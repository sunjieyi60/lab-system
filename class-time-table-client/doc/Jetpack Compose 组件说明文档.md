# Jetpack Compose 组件说明文档

本文档详细说明了在实验室管理系统界面中使用的 Jetpack Compose 组件及其功能、使用场景和效果案例。

## 目录

1. [Compose 注解说明](#compose-注解说明)
2. [布局组件](#布局组件)
3. [图像组件](#图像组件)
4. [Material3 组件](#material3-组件)
5. [文本和显示组件](#文本和显示组件)
6. [交互组件](#交互组件)
7. [修饰符和效果](#修饰符和效果)
8. [布局组合模式](#布局组合模式)
9. [未使用但常用的 Compose 组件](#未使用但常用的-compose-组件)

**相关文档：**
- Compose 注解说明内容已整合到本文 [Compose 注解说明](#compose-注解说明) 小节中

---

## Compose 注解说明

## @Composable 注解

### 功能说明

`@Composable` 是 Jetpack Compose 的核心注解，用于标记**可组合函数（Composable Function）**。

**作用：**
- 告诉 Compose 编译器这是一个可组合函数
- 允许函数调用其他 `@Composable` 函数
- 启用 Compose 的重组（Recomposition）机制
- 允许函数使用 Compose 的状态和副作用 API

### 基本用法

```kotlin
import androidx.compose.runtime.Composable

@Composable
fun MyComponent() {
    Text("Hello Compose")
}
```

### 使用场景

**✅ 必须使用 @Composable 的情况：**

1. **UI 组件函数**
   ```kotlin
   @Composable
   fun TopBar() {
       Row {
           Text("标题")
           Button(onClick = { }) {
               Text("按钮")
           }
       }
   }
   ```

2. **调用其他 Composable 函数**
   ```kotlin
   @Composable
   fun ParentComponent() {
       ChildComponent()  // 调用其他 @Composable 函数
   }
   ```

3. **使用 Compose API**
   ```kotlin
   @Composable
   fun ComponentWithState() {
       val state = remember { mutableStateOf(0) }  // 使用 remember
       Text("Count: ${state.value}")
   }
   ```

**❌ 不需要 @Composable 的情况：**

1. **普通函数（不涉及 UI）**
   ```kotlin
   // 不需要 @Composable
   fun calculateTotal(items: List<Item>): Int {
       return items.sumOf { it.price }
   }
   ```

2. **数据类、普通类**
   ```kotlin
   // 不需要 @Composable
   data class User(val name: String, val age: Int)
   ```

### 项目中的使用

**代码位置：**
- `ui/LabDashboardScreen.kt` 第 21 行
- `ui/TopBar.kt` 第 35 行
- `ui/LeftColumn.kt` 第 29 行
- `ui/MiddleColumn.kt` 第 141 行
- `ui/RightColumn.kt` 第 31 行
- 以及所有其他 UI 组件函数

**示例：**
```kotlin
@Composable
fun MiddleColumn(
    courses: List<CourseScheduleDto> = emptyList(),
    currentWeek: Int = CourseScheduleConstants.DEFAULT_CURRENT_WEEK,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        // UI 内容
    }
}
```

---

## @UiComposable 注解

### 功能说明

`@UiComposable` 是 `@Composable` 的一个**特殊变体**，用于标记**UI 相关的 Composable 函数**。

**特点：**
- `@UiComposable` 是 `@Composable` 的子类型
- 主要用于 Compose UI 库内部的函数
- 普通开发者**很少直接使用**

### 基本用法

```kotlin
import androidx.compose.ui.UiComposable

@UiComposable
fun UiComponent() {
    // UI 内容
}
```

### 与 @Composable 的关系

**继承关系：**
```
@Composable (父类型)
    └── @UiComposable (子类型，专门用于 UI)
```

**关键区别：**

| 特性 | @Composable | @UiComposable |
|------|-------------|---------------|
| **用途** | 通用可组合函数 | 专门用于 UI 组件 |
| **使用频率** | 非常常用 | 很少直接使用 |
| **调用限制** | 可以调用 @Composable 和 @UiComposable | 只能调用 @UiComposable |
| **适用场景** | 所有 Compose 函数 | UI 库内部函数 |

### 实际应用

**在 Compose UI 库中的使用：**
```kotlin
// Compose UI 库内部（开发者通常不直接使用）
@UiComposable
fun Layout(
    modifier: Modifier = Modifier,
    content: @UiComposable () -> Unit
) {
    // 布局实现
}
```

**为什么普通开发者不需要使用：**
- Compose UI 库已经正确标记了所有 UI 函数
- 使用 `@Composable` 就足够了
- `@UiComposable` 主要用于类型安全和内部优化

---

## 注解的区别和关系

### 类型层次结构

```
@Composable (基础注解)
    ├── @UiComposable (UI 专用)
    └── 其他特殊变体（如果有）
```

### 调用规则

**规则 1：@Composable 可以调用 @Composable**
```kotlin
@Composable
fun Parent() {
    Child()  // ✅ 可以调用
}

@Composable
fun Child() {
    Text("Child")
}
```

**规则 2：@Composable 可以调用 @UiComposable**
```kotlin
@Composable
fun Parent() {
    UiChild()  // ✅ 可以调用
}

@UiComposable
fun UiChild() {
    Text("UI Child")
}
```

**规则 3：@UiComposable 只能调用 @UiComposable**
```kotlin
@UiComposable
fun UiParent() {
    UiChild()  // ✅ 可以调用
    // RegularChild()  // ❌ 错误！不能调用普通 @Composable
}

@Composable
fun RegularChild() {
    Text("Regular")
}
```

### 函数类型参数

**@Composable 函数类型：**
```kotlin
// 参数是 @Composable 函数
@Composable
fun Container(
    content: @Composable () -> Unit  // ✅ 正确
) {
    content()
}

// 错误示例
@Composable
fun Container(
    content: () -> Unit  // ❌ 错误！必须是 @Composable
) {
    content()  // 无法调用
}
```

**@UiComposable 函数类型：**
```kotlin
// UI 库内部使用
@UiComposable
fun Layout(
    content: @UiComposable () -> Unit  // ✅ UI 专用
) {
    content()
}
```

---

## 常见错误和解决方案

### 错误 1：缺少 @Composable 注解

**错误信息：**
```
@Composable invocations can only happen from the context of a @Composable function
```

**原因：**
在非 `@Composable` 函数中调用了 `@Composable` 函数。

**错误代码：**
```kotlin
fun MyActivity() {
    setContent {
        MyScreen()  // ❌ 错误！setContent 是 @Composable 上下文
    }
}
```

**解决方案：**
```kotlin
// 方法 1：在 Activity 中使用 setContent（正确）
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {  // setContent 提供 @Composable 上下文
            MyScreen()  // ✅ 正确
        }
    }
}

// 方法 2：将函数标记为 @Composable
@Composable
fun MyFunction() {
    MyScreen()  // ✅ 正确
}
```

### 错误 2：在非 Composable 上下文中使用 Compose API

**错误信息：**
```
@Composable function 'remember' can only be called from within the context of a @Composable function
```

**原因：**
在普通函数中使用了 `remember`、`mutableStateOf` 等 Compose API。

**错误代码：**
```kotlin
fun calculateState() {
    val state = remember { mutableStateOf(0) }  // ❌ 错误！
}
```

**解决方案：**
```kotlin
@Composable
fun calculateState() {
    val state = remember { mutableStateOf(0) }  // ✅ 正确
}
```

### 错误 3：@Composable 函数类型不匹配

**错误信息：**
```
Type mismatch: inferred type is () -> Unit but @Composable () -> Unit was expected
```

**原因：**
函数参数类型应该是 `@Composable () -> Unit`，但传入了普通函数。

**错误代码：**
```kotlin
@Composable
fun Container(content: @Composable () -> Unit) {
    content()
}

fun MyFunction() {
    Container {
        // 内容
    }  // ❌ 可能在某些上下文中出错
}
```

**解决方案：**
```kotlin
@Composable
fun MyFunction() {
    Container {
        // 内容
    }  // ✅ 正确
}
```

### 错误 4：@UiComposable vs @Composable 混淆

**错误信息：**
```
Calling a androidx.compose.ui.UiComposable composable function where a UI Composable composable was expected
```

**原因：**
在某些 UI 库函数中，参数类型是 `@UiComposable`，但传入了 `@Composable`。

**错误代码：**
```kotlin
// 假设某个 UI 库函数需要 @UiComposable
@UiComposable
fun SomeUiFunction(content: @UiComposable () -> Unit) {
    content()
}

@Composable
fun MyComponent() {
    SomeUiFunction {
        Text("Hello")  // ❌ 可能在某些情况下出错
    }
}
```

**解决方案：**
```kotlin
// 通常不需要手动处理，Compose 会自动转换
// 如果遇到问题，检查导入是否正确
import androidx.compose.runtime.Composable
import androidx.compose.ui.UiComposable  // 确保导入正确
```

### 错误 5：Preview 函数缺少 @Composable

**错误信息：**
```
@Preview functions must be @Composable
```

**原因：**
`@Preview` 注解的函数必须是 `@Composable`。

**错误代码：**
```kotlin
@Preview
fun MyPreview() {  // ❌ 错误！缺少 @Composable
    MyScreen()
}
```

**解决方案：**
```kotlin
@Preview
@Composable
fun MyPreview() {  // ✅ 正确
    MyScreen()
}
```

---

## 最佳实践

### 1. 何时使用 @Composable

**✅ 使用 @Composable：**
- 所有 UI 组件函数
- 调用其他 Composable 的函数
- 使用 Compose API（remember、mutableStateOf 等）的函数
- Preview 函数

**❌ 不使用 @Composable：**
- 纯数据计算函数
- 工具函数（不涉及 UI）
- 数据类、普通类
- 业务逻辑函数

### 2. 函数命名规范

**Composable 函数：**
- 使用 PascalCase（首字母大写）
- 名词形式（如 `TopBar`、`UserCard`）
- 不使用动词（如 `createTopBar` ❌）

```kotlin
// ✅ 正确
@Composable
fun TopBar() { }

@Composable
fun UserCard(user: User) { }

// ❌ 错误
@Composable
fun createTopBar() { }  // 应该是 TopBar
```

### 3. 函数参数规范

**Modifier 参数：**
- 总是作为第一个参数（可选）
- 使用默认值 `Modifier`

```kotlin
@Composable
fun MyComponent(
    modifier: Modifier = Modifier,  // ✅ 第一个参数
    title: String,
    content: String
) {
    // ...
}
```

**@Composable 函数类型参数：**
- 使用 `@Composable () -> Unit` 类型
- 放在参数列表最后

```kotlin
@Composable
fun Container(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit  // ✅ 最后
) {
    Column(modifier = modifier) {
        Text(title)
        content()
    }
}
```

### 4. 避免在 @Composable 中执行耗时操作

**❌ 错误：**
```kotlin
@Composable
fun MyComponent() {
    val data = loadDataFromDatabase()  // ❌ 阻塞操作！
    Text(data)
}
```

**✅ 正确：**
```kotlin
@Composable
fun MyComponent() {
    val data by remember {
        derivedStateOf { loadDataFromDatabase() }
    }.collectAsState()  // 使用协程或状态管理
    
    Text(data)
}
```

### 5. Preview 函数规范

**标准 Preview：**
```kotlin
@Preview(showBackground = true)
@Composable
fun MyComponentPreview() {
    MaterialTheme {
        MyComponent()
    }
}
```

**多个 Preview：**
```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun MyComponentLightPreview() {
    MaterialTheme {
        MyComponent()
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyComponentDarkPreview() {
    MaterialTheme {
        MyComponent()
    }
}
```

---

## 总结

### 核心要点

1. **@Composable**：
   - 最常用的注解
   - 标记所有 UI 组件函数
   - 允许使用 Compose API

2. **@UiComposable**：
   - 很少直接使用
   - 主要用于 UI 库内部
   - 是 @Composable 的子类型

3. **调用规则**：
   - `@Composable` 可以调用 `@Composable` 和 `@UiComposable`
   - `@UiComposable` 只能调用 `@UiComposable`
   - 普通函数不能调用 `@Composable`

4. **常见错误**：
   - 缺少 `@Composable` 注解
   - 在非 Composable 上下文中使用 Compose API
   - Preview 函数缺少 `@Composable`

### 快速参考

```kotlin
// ✅ 标准 UI 组件
@Composable
fun MyComponent() {
    Text("Hello")
}

// ✅ 带参数的组件
@Composable
fun MyComponent(
    modifier: Modifier = Modifier,
    title: String
) {
    Text(title)
}

// ✅ 带子组件的组件
@Composable
fun Container(
    content: @Composable () -> Unit
) {
    Column {
        content()
    }
}

// ✅ Preview
@Preview
@Composable
fun MyComponentPreview() {
    MyComponent(title = "Preview")
}
```

---

## 参考资料

- [Jetpack Compose 官方文档 - Composable 函数](https://developer.android.com/jetpack/compose/mental-model)
- [Compose 编译器文档](https://developer.android.com/jetpack/compose/compiler)
- [Compose 最佳实践](https://developer.android.com/jetpack/compose/performance)

---

## 布局组件

### 1. Column（垂直布局容器）

**功能说明：**
- `Column` 是一个垂直排列子组件的布局容器
- 子组件按照从上到下的顺序排列
- 支持通过 `verticalArrangement` 控制子组件之间的间距和对齐方式

**使用场景：**
- 垂直排列多个组件
- 创建表单布局
- 构建列表式界面

**代码位置：**
- **主界面布局**：`ui/LabDashboardScreen.kt` 第 25-35 行
  ```25:35:ui/LabDashboardScreen.kt
  Column(
      modifier = modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
  ) {
      // 顶部栏区域
      TopBar(...)
      // 主内容区域：三列布局
      Row(...) { ... }
  }
  ```
- **左侧列布局**：`ui/LeftColumn.kt` 第 32-45 行
- **右侧列布局**：`ui/RightColumn.kt` 第 37-50 行
- **顶部栏右侧信息**：`ui/TopBar.kt` 第 109-123 行
- **课表内容**：`ui/MiddleColumn.kt` 第 40-78 行、第 89-99 行
- **卡片内容**：`ui/LeftColumn.kt` 第 67-87 行、第 105-125 行、第 142-162 行

**页面位置：**
- **主界面**：整个屏幕的垂直布局，包含顶部栏和主内容区域
- **左侧列**：页面左侧，从上到下排列实验室信息、简介、规章制度三个卡片
- **右侧列**：页面右侧，从上到下排列通知公告、校历、开门方式三个面板
- **顶部栏右侧**：顶部栏最右侧，垂直排列日期时间和天气信息
- **课表区域**：中间列的主要内容区域，垂直排列周一到周五的课程

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`fillMaxSize()` 填充父容器、`background()` 设置背景色、`verticalScroll()` 启用滚动
   - **示例**：`modifier = modifier.fillMaxSize().background(...)`

2. **verticalArrangement: Arrangement.Vertical**
   - **效果**：控制子组件在垂直方向上的排列方式
   - **本项目使用**：
     - `Arrangement.spacedBy(16.dp)`：子组件之间固定 16dp 间距（左侧列、右侧列）
     - `Arrangement.spacedBy(4.dp)`：子组件之间固定 4dp 间距（顶部栏右侧信息）
     - `Arrangement.spacedBy(12.dp)`：子组件之间固定 12dp 间距（课表内容）
   - **视觉效果**：子组件垂直排列，间距均匀

#### 其他可用属性

1. **horizontalAlignment: Alignment.Horizontal**
   - **效果**：控制子组件在水平方向上的对齐方式
   - **可选值**：
     - `Alignment.Start`：左对齐（默认）
     - `Alignment.CenterHorizontally`：水平居中
     - `Alignment.End`：右对齐
   - **使用示例**：
     ```kotlin
     Column(
         horizontalAlignment = Alignment.CenterHorizontally
     ) { /* 子组件水平居中 */ }
     ```

2. **verticalArrangement: Arrangement.Vertical**（其他选项）
   - **可选值**：
     - `Arrangement.Top`：顶部对齐（默认）
     - `Arrangement.Center`：垂直居中
     - `Arrangement.Bottom`：底部对齐
     - `Arrangement.SpaceEvenly`：均匀分布，包括两端
     - `Arrangement.SpaceBetween`：两端对齐，中间均匀分布
     - `Arrangement.SpaceAround`：均匀分布，两端有间距
     - `Arrangement.spacedBy(density)`：固定间距（已使用）

#### 保持固定间距的同时对齐容器底部

**需求**：子组件之间保持固定间距（如 16dp），但整个内容区域对齐容器底部。

**解决方案：使用 `Spacer` + `weight()` 填充顶部空间**

```kotlin
Column(
    modifier = Modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.spacedBy(16.dp)  // 子组件之间固定间距
) {
    Spacer(modifier = Modifier.weight(1f))  // 填充顶部空间，推动内容到底部
    
    Text("第一个组件")
    Text("第二个组件")
    Text("第三个组件")
}
```

**完整示例：**
```kotlin
@Composable
fun BottomAlignedWithSpacing() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)  // 固定间距 16dp
    ) {
        // 使用 weight(1f) 填充顶部空间，让内容对齐底部
        Spacer(modifier = Modifier.weight(1f))
        
        Card {
            Text("卡片 1")
        }
        
        Card {
            Text("卡片 2")
        }
        
        Card {
            Text("卡片 3")
        }
    }
}
```

**说明：**
- `Spacer(modifier = Modifier.weight(1f))` 会填充所有可用的垂直空间
- 由于它位于最顶部，会将后续内容推到底部
- `Arrangement.spacedBy(16.dp)` 确保子组件之间保持 16dp 的固定间距
- 即使容器高度变化，内容也会始终对齐底部

**其他方法对比：**

**方法 1：使用 `Arrangement.Bottom` + 手动 `Spacer`（不推荐）**
```kotlin
Column(
    modifier = Modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.Bottom
) {
    Text("组件 1")
    Spacer(modifier = Modifier.height(16.dp))  // 手动设置间距
    Text("组件 2")
    Spacer(modifier = Modifier.height(16.dp))
    Text("组件 3")
}
```
**缺点**：需要手动为每个组件之间添加 Spacer，不够优雅。

**方法 2：使用 `Arrangement.SpaceBetween` + 顶部 Spacer（推荐）**
```kotlin
Column(
    modifier = Modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.SpaceBetween
) {
    Spacer(modifier = Modifier.weight(1f))  // 填充顶部空间
    
    Text("组件 1")
    Text("组件 2")
    Text("组件 3")
}
```
**说明**：`SpaceBetween` 会在子组件之间均匀分布空间，配合顶部的 `Spacer(weight(1f))` 可以实现底部对齐，但间距不固定。

**方法 3：使用 `Arrangement.spacedBy()` + 顶部 Spacer（最推荐）**
```kotlin
Column(
    modifier = Modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Spacer(modifier = Modifier.weight(1f))  // 填充顶部空间
    
    Text("组件 1")
    Text("组件 2")
    Text("组件 3")
}
```
**优点**：
- ✅ 子组件之间保持固定的 16dp 间距
- ✅ 内容对齐容器底部
- ✅ 代码简洁，易于维护
- ✅ 响应式，自动适应容器高度变化

**实际应用场景：**
```kotlin
// 底部操作栏：按钮固定在底部，按钮之间有固定间距
Column(
    modifier = Modifier
        .fillMaxHeight()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    Spacer(modifier = Modifier.weight(1f))  // 填充上方空间
    
    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("主要操作")
    }
    
    OutlinedButton(
        onClick = { },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("次要操作")
    }
}
```

3. **contentPadding: PaddingValues**
   - **效果**：为 Column 内容添加内边距
   - **使用示例**：
     ```kotlin
     Column(
         contentPadding = PaddingValues(16.dp)
     ) { /* 内容 */ }
     ```

#### 为不同子组件设置不同的对齐方式

**问题**：`Column` 的 `horizontalAlignment` 作用于整个容器，所有子组件共享同一对齐方式。如何让不同的子组件有不同的水平对齐？

**解决方案：使用 `Modifier.align()` 为单个子组件设置对齐**

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start  // 默认对齐方式（左对齐）
) {
    Text("左对齐文本")  // 使用默认对齐
    
    Text(
        text = "居中文本",
        modifier = Modifier.align(Alignment.CenterHorizontally)  // 单独设置居中
    )
    
    Text(
        text = "右对齐文本",
        modifier = Modifier.align(Alignment.End)  // 单独设置右对齐
    )
}
```

**说明：**
- `Column` 的 `horizontalAlignment` 设置所有子组件的默认水平对齐
- 使用 `Modifier.align(Alignment.Horizontal)` 可以为单个子组件覆盖默认对齐
- `align()` 修饰符只能在 `ColumnScope` 或 `RowScope` 中使用

**完整示例：**
```kotlin
@Composable
fun MixedAlignmentColumn() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start  // 默认左对齐
    ) {
        // 使用默认对齐（左对齐）
        Text("默认左对齐")
        
        // 单独设置居中
        Text(
            text = "单独居中",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // 单独设置右对齐
        Text(
            text = "单独右对齐",
            modifier = Modifier.align(Alignment.End)
        )
        
        // 再次使用默认对齐（左对齐）
        Text("又是左对齐")
    }
}
```

---

### 2. Row（水平布局容器）

**功能说明：**
- `Row` 是一个水平排列子组件的布局容器
- 子组件按照从左到右的顺序排列
- 支持通过 `horizontalArrangement` 控制子组件之间的间距和对齐方式

**使用场景：**
- 水平排列多个组件
- 创建工具栏
- 构建标签栏

**代码位置：**
- **主内容区三列布局**：`ui/LabDashboardScreen.kt` 第 38-64 行
  ```38:64:ui/LabDashboardScreen.kt
  Row(
      modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
      LeftColumn(...)
      MiddleColumn(...)
      RightColumn(...)
  }
  ```
- **顶部栏主布局**：`ui/TopBar.kt` 第 48-125 行
- **顶部栏左侧按钮**：`ui/TopBar.kt` 第 54-75 行
- **顶部栏中间标题**：`ui/TopBar.kt` 第 78-106 行
- **课表标题行**：`ui/MiddleColumn.kt` 第 47-70 行
- **信息行**：`ui/LeftColumn.kt` 第 176-191 行
- **开门方式按钮行**：`ui/RightColumn.kt` 第 164-178 行、第 181-195 行

**页面位置：**
- **主内容区**：页面中间部分，从左到右排列左侧列、中间列、右侧列（1:2:1 比例）
- **顶部栏**：页面最顶部，从左到右排列操作按钮、标题Logo、时间天气信息
- **课表标题**：课表卡片顶部，左侧显示"实验室课表"标题，右侧显示"当前周次: 第3周"标签
- **实验室信息行**：实验室信息卡片内，每行左侧显示标签，右侧显示值
- **开门方式按钮**：开门方式面板内，2x2 网格布局，每行两个按钮水平排列

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`fillMaxSize()` 填充父容器、`fillMaxWidth()` 填充宽度、`padding()` 设置内边距
   - **示例**：`modifier = Modifier.fillMaxSize().padding(16.dp)`

2. **horizontalArrangement: Arrangement.Horizontal**
   - **效果**：控制子组件在水平方向上的排列方式
   - **本项目使用**：
     - `Arrangement.SpaceBetween`：两端对齐，中间均匀分布（顶部栏主布局、课表标题行）
     - `Arrangement.spacedBy(16.dp)`：子组件之间固定 16dp 间距（主内容区三列）
     - `Arrangement.spacedBy(12.dp)`：子组件之间固定 12dp 间距（按钮行）
   - **视觉效果**：子组件水平排列，间距均匀或两端对齐

3. **verticalAlignment: Alignment.Vertical**
   - **效果**：控制子组件在垂直方向上的对齐方式
   - **本项目使用**：
     - `Alignment.CenterVertically`：垂直居中（顶部栏、课表标题行）
   - **视觉效果**：子组件在垂直方向上居中对齐

#### 其他可用属性

1. **horizontalArrangement: Arrangement.Horizontal**（其他选项）
   - **可选值**：
     - `Arrangement.Start`：左对齐（默认）
     - `Arrangement.Center`：水平居中
     - `Arrangement.End`：右对齐
     - `Arrangement.SpaceEvenly`：均匀分布，包括两端
     - `Arrangement.SpaceBetween`：两端对齐，中间均匀分布（已使用）
     - `Arrangement.SpaceAround`：均匀分布，两端有间距
     - `Arrangement.spacedBy(density)`：固定间距（已使用）

2. **verticalAlignment: Alignment.Vertical**（其他选项）
   - **可选值**：
     - `Alignment.Top`：顶部对齐（默认）
     - `Alignment.CenterVertically`：垂直居中（已使用）
     - `Alignment.Bottom`：底部对齐
   - **使用示例**：
     ```kotlin
     Row(
         verticalAlignment = Alignment.Bottom
     ) { /* 子组件底部对齐 */ }
     ```

3. **contentPadding: PaddingValues**
   - **效果**：为 Row 内容添加内边距
   - **使用示例**：
     ```kotlin
     Row(
         contentPadding = PaddingValues(horizontal = 16.dp)
     ) { /* 内容 */ }
     ```

#### 为不同子组件设置不同的对齐方式

**问题**：`Row` 的 `verticalAlignment` 作用于整个容器，所有子组件共享同一对齐方式。如何让不同的子组件有不同的垂直对齐？

**解决方案：使用 `Modifier.align()` 为单个子组件设置对齐**

```kotlin
Row(
    modifier = Modifier.fillMaxHeight(),
    verticalAlignment = Alignment.Top  // 默认对齐方式（顶部对齐）
) {
    Text("顶部对齐")  // 使用默认对齐
    
    Text(
        text = "居中文本",
        modifier = Modifier.align(Alignment.CenterVertically)  // 单独设置垂直居中
    )
    
    Text(
        text = "底部对齐",
        modifier = Modifier.align(Alignment.Bottom)  // 单独设置底部对齐
    )
}
```

**说明：**
- `Row` 的 `verticalAlignment` 设置所有子组件的默认垂直对齐
- 使用 `Modifier.align(Alignment.Vertical)` 可以为单个子组件覆盖默认对齐
- `align()` 修饰符只能在 `ColumnScope` 或 `RowScope` 中使用

**完整示例：**
```kotlin
@Composable
fun MixedAlignmentRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.Top  // 默认顶部对齐
    ) {
        // 使用默认对齐（顶部对齐）
        Text("顶部对齐")
        
        // 单独设置垂直居中
        Text(
            text = "垂直居中",
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        
        // 单独设置底部对齐
        Text(
            text = "底部对齐",
            modifier = Modifier.align(Alignment.Bottom)
        )
        
        // 再次使用默认对齐（顶部对齐）
        Text("又是顶部对齐")
    }
}
```

**实际应用场景：**
```kotlin
// 工具栏：不同按钮有不同的垂直对齐
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "菜单",
        modifier = Modifier.align(Alignment.Top)  // 图标顶部对齐
    )
    
    Text(
        text = "标题",
        modifier = Modifier.align(Alignment.CenterVertically)  // 文字居中
    )
    
    Button(
        onClick = { },
        modifier = Modifier.align(Alignment.Bottom)  // 按钮底部对齐
    ) {
        Text("操作")
    }
}
```

---

### 3. Box（通用布局容器）

**功能说明：**
- `Box` 是一个通用的布局容器，可以叠加子组件
- 支持通过 `contentAlignment` 控制子组件的对齐方式
- 常用于定位和叠加元素

**使用场景：**
- 叠加多个组件（如图标+文字）
- 绝对定位元素
- 创建自定义布局

**代码位置：**
- **顶部栏容器**：`ui/TopBar.kt` 第 43-47 行
  ```43:47:ui/TopBar.kt
  Box(
      modifier = modifier
          .background(MaterialTheme.colorScheme.primaryContainer)
          .padding(16.dp)
  ) {
  ```
- **Logo 容器**：`ui/TopBar.kt` 第 83-98 行
  ```83:98:ui/TopBar.kt
  Box(
      modifier = Modifier
          .size(48.dp)
          .background(
              Color(0xFF2196F3),
              shape = MaterialTheme.shapes.medium
          ),
      contentAlignment = Alignment.Center
  ) {
      Text(
          text = "C",
          color = Color.White,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold
      )
  }
  ```

**页面位置：**
- **顶部栏背景容器**：整个顶部栏区域，提供主色容器背景和统一的内边距
- **Logo 图标**：顶部栏中间区域，标题左侧，48dp×48dp 的蓝色圆角方块，内部居中显示字母"C"

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：
     - `background()` 设置背景色（顶部栏容器）
     - `padding()` 设置内边距（顶部栏容器）
     - `size(48.dp)` 设置固定尺寸（Logo 容器）
   - **示例**：`modifier = modifier.background(...).padding(16.dp)`

2. **contentAlignment: Alignment**
   - **效果**：控制子组件在 Box 内的对齐方式
   - **本项目使用**：
     - `Alignment.Center`：居中对齐（Logo 容器中的文字）
   - **视觉效果**：子组件在 Box 中心位置显示

#### 其他可用属性

1. **contentAlignment: Alignment**（其他选项）
   - **可选值**：
     - `Alignment.TopStart`：左上角
     - `Alignment.TopCenter`：顶部居中
     - `Alignment.TopEnd`：右上角
     - `Alignment.CenterStart`：左侧居中
     - `Alignment.Center`：居中（已使用）
     - `Alignment.CenterEnd`：右侧居中
     - `Alignment.BottomStart`：左下角
     - `Alignment.BottomCenter`：底部居中
     - `Alignment.BottomEnd`：右下角
   - **使用示例**：
     ```kotlin
     Box(
         contentAlignment = Alignment.TopEnd
     ) { /* 子组件在右上角 */ }
     ```

2. **propagateMinConstraints: Boolean**
   - **效果**：是否将最小约束传播给子组件
   - **默认值**：`false`
   - **使用场景**：需要子组件遵守最小尺寸约束时

3. **content: @Composable BoxScope.() -> Unit**
   - **效果**：Box 的内容区域
   - **说明**：可以使用 `BoxScope` 中的 `align()` 修饰符来单独定位子组件

**BoxScope 修饰符：**

- **Modifier.align(alignment: Alignment)**
  - **效果**：在 Box 内对齐单个子组件
  - **使用示例**：
    ```kotlin
    Box {
        Text("左上角", modifier = Modifier.align(Alignment.TopStart))
        Text("右下角", modifier = Modifier.align(Alignment.BottomEnd))
    }
    ```

---

### 4. BoxWithConstraints（响应式布局容器）

**功能说明：**
- `BoxWithConstraints` 是一个特殊的 `Box`，可以在其作用域内访问父容器的约束信息（宽度、高度）
- 通过 `maxWidth`、`maxHeight`、`minWidth`、`minHeight` 等属性获取容器尺寸
- 常用于需要根据容器大小动态调整布局的场景

**使用场景：**
- 响应式布局设计
- 根据容器宽度动态计算子组件尺寸
- 确保多个组件使用相同的宽度约束

**代码位置：**
- **课程表表头和内容宽度同步**：`ui/MiddleColumn.kt` 第 207-217 行、第 223-235 行
  ```207:235:ui/MiddleColumn.kt
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val sectionColumnWidth = CourseScheduleConstants.SECTION_COLUMN_WIDTH
      val weekColumnWidth = (maxWidth - sectionColumnWidth) / CourseScheduleConstants.WEEKDAY_COUNT

      // 表头（使用相同的列宽）
      TableHeader(
          sectionColumnWidth = sectionColumnWidth,
          weekColumnWidth = weekColumnWidth
      )
  }
  
  // 表头和表格内容之间的间距
  Spacer(modifier = Modifier.height(CourseScheduleConstants.HEADER_TABLE_SPACING))
  
  // 使用自定义布局实现单元格合并
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val sectionColumnWidth = CourseScheduleConstants.SECTION_COLUMN_WIDTH
      val weekColumnWidth = (maxWidth - sectionColumnWidth) / CourseScheduleConstants.WEEKDAY_COUNT
      
      MergedTableLayout(
          cellData = cellData,
          sectionColumnWidth = sectionColumnWidth,
          weekColumnWidth = weekColumnWidth,
          modifier = Modifier
              .fillMaxWidth()
              .border(CourseScheduleConstants.TABLE_BORDER_WIDTH, Color.Gray)
      )
  }
  ```

**页面位置：**
- **课程表布局**：中间列课程表区域，用于确保表头和表格内容使用相同的列宽计算逻辑，避免列错位问题

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`fillMaxWidth()` 填充宽度
   - **示例**：`modifier = Modifier.fillMaxWidth()`

2. **BoxWithConstraintsScope 属性**
   - **maxWidth: Dp**：父容器的最大宽度
   - **maxHeight: Dp**：父容器的最大高度
   - **minWidth: Dp**：父容器的最小宽度
   - **minHeight: Dp**：父容器的最小高度
   - **本项目使用**：`maxWidth` 用于计算星期列的宽度
   - **示例**：`val weekColumnWidth = (maxWidth - sectionColumnWidth) / 7`

#### 其他可用属性

1. **BoxWithConstraintsScope 的其他属性**
   - **constraints: Constraints**：完整的约束对象
   - **使用示例**：
     ```kotlin
     BoxWithConstraints {
         val isWideScreen = maxWidth > 600.dp
         if (isWideScreen) {
             // 宽屏布局
         } else {
             // 窄屏布局
         }
     }
     ```

2. **响应式设计示例**
   ```kotlin
   @Composable
   fun ResponsiveLayout() {
       BoxWithConstraints {
           val columnCount = when {
               maxWidth > 1200.dp -> 4
               maxWidth > 800.dp -> 3
               maxWidth > 600.dp -> 2
               else -> 1
           }
           
           LazyVerticalGrid(
               columns = GridCells.Fixed(columnCount)
           ) { /* 内容 */ }
       }
   }
   ```

**BoxWithConstraints vs Box：**
- **Box**：简单的叠加容器，无法访问父容器尺寸
- **BoxWithConstraints**：可以访问父容器约束，适合响应式布局
- **选择建议**：需要根据容器大小动态调整布局时使用 `BoxWithConstraints`

---

### 5. Spacer（间距组件）

**功能说明：**
- `Spacer` 用于在布局中添加空白间距
- 通过 `Modifier` 设置宽度或高度
- 比使用 `padding` 更灵活

**使用场景：**
- 组件之间的间距
- 调整布局空白
- 响应式间距

**代码位置：**
- **实验室简介标题和内容之间**：`ui/LeftColumn.kt` 第 116 行
  ```116:116:ui/LeftColumn.kt
  Spacer(modifier = Modifier.height(8.dp))
  ```
- **实验室规章制度标题和内容之间**：`ui/LeftColumn.kt` 第 153 行
- **课表标题和内容之间**：`ui/MiddleColumn.kt` 第 72 行
- **通知公告标题和内容之间**：`ui/RightColumn.kt` 第 78 行
- **校历标题和内容之间**：`ui/RightColumn.kt` 第 115 行
- **开门方式标题和按钮之间**：`ui/RightColumn.kt` 第 157 行

**页面位置：**
- **卡片内部间距**：各个信息卡片内，标题文字和内容文字之间的 8dp 垂直间距
- **课表区域间距**：课表卡片内，标题区域和课程列表之间的 16dp 垂直间距

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：设置 Spacer 的尺寸
   - **本项目使用**：
     - `Modifier.height(8.dp)`：8dp 高度的垂直间距（标题和内容之间）
     - `Modifier.height(16.dp)`：16dp 高度的垂直间距（课表标题和内容之间）
   - **视觉效果**：在垂直布局中创建空白间距

#### 其他可用属性

1. **modifier: Modifier**（其他选项）
   - **水平间距**：`Modifier.width(density)` - 在水平布局中创建空白间距
   - **固定尺寸**：`Modifier.size(width, height)` - 创建固定宽高的间距
   - **填充剩余空间**：`Modifier.weight(1f)` - 在 Row/Column 中填充剩余空间
   - **使用示例**：
     ```kotlin
     Row {
         Text("左侧")
         Spacer(modifier = Modifier.width(20.dp))  // 20dp 水平间距
         Text("右侧")
     }
     
     Column {
         Text("上方")
         Spacer(modifier = Modifier.height(10.dp))  // 10dp 垂直间距
         Text("下方")
     }
     
     Row {
         Text("左侧")
         Spacer(modifier = Modifier.weight(1f))  // 填充剩余空间
         Text("右侧")
     }
     ```

**与 Arrangement.spacedBy() 的区别：**
- `Spacer`：手动控制单个间距，更灵活
- `Arrangement.spacedBy()`：自动在所有子组件之间添加间距，更简洁
- **选择建议**：需要统一间距时用 `spacedBy()`，需要特殊间距时用 `Spacer`

---

## 图像组件

### 6. Image（图像组件）

**功能说明：**
- `Image` 用于显示图片资源
- 支持从资源文件、网络、本地文件等加载图片
- 可以设置内容描述（用于无障碍访问）

**使用场景：**
- Logo 显示
- 图标显示
- 图片展示

**代码位置：**
- **顶部栏 Logo**：`ui/TopBar.kt` 第 94-97 行
  ```94:97:ui/TopBar.kt
  Image(
      painter = painterResource(id = R.drawable.logo),
      contentDescription = "Logo"
  )
  ```

**页面位置：**
- **顶部栏 Logo**：页面顶部中间区域，标题左侧，显示应用 Logo

**参数属性说明：**

#### 已使用的属性

1. **painter: Painter**
   - **效果**：图片绘制对象
   - **本项目使用**：`painterResource(id = R.drawable.logo)` 从资源文件加载
   - **示例**：`painter = painterResource(id = R.drawable.logo)`

2. **contentDescription: String?**
   - **效果**：图片的内容描述（用于无障碍访问）
   - **本项目使用**：`"Logo"`
   - **说明**：屏幕阅读器会读取此描述

#### 其他可用属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **常用修饰符**：
     - `size(width, height)`：设置图片尺寸
     - `fillMaxSize()`：填充父容器
     - `clip(shape)`：裁剪图片形状
     - `alpha(alpha)`：设置透明度
   - **使用示例**：
     ```kotlin
     Image(
         painter = painterResource(id = R.drawable.logo),
         contentDescription = "Logo",
         modifier = Modifier
             .size(88.dp)
             .clip(CircleShape)
     )
     ```

2. **alignment: Alignment**
   - **效果**：图片在容器内的对齐方式
   - **默认值**：`Alignment.Center`
   - **使用示例**：
     ```kotlin
     Image(
         painter = painterResource(id = R.drawable.logo),
         contentDescription = "Logo",
         alignment = Alignment.TopStart
     )
     ```

3. **contentScale: ContentScale**
   - **效果**：图片缩放方式
   - **可选值**：
     - `ContentScale.Fit`：保持宽高比，完整显示
     - `ContentScale.Crop`：保持宽高比，裁剪多余部分
     - `ContentScale.FillBounds`：填充整个区域（可能变形）
     - `ContentScale.FillWidth`：填充宽度
     - `ContentScale.FillHeight`：填充高度
   - **使用示例**：
     ```kotlin
     Image(
         painter = painterResource(id = R.drawable.logo),
         contentDescription = "Logo",
         contentScale = ContentScale.Crop
     )
     ```

4. **从网络加载图片**
   ```kotlin
     // 需要添加 Coil 依赖
     AsyncImage(
         model = "https://example.com/image.jpg",
         contentDescription = "网络图片",
         modifier = Modifier.size(200.dp)
     )
     ```

**图片加载库推荐：**
- **Coil**：推荐的图片加载库，支持网络、本地、资源文件
- **Glide**：传统的图片加载库（需要适配 Compose）

---

## Material3 组件

### 7. Card（卡片组件）

**功能说明：**
- `Card` 是 Material3 设计规范中的卡片组件
- 提供阴影、圆角、背景色等视觉效果
- 用于组织相关信息内容

**使用场景：**
- 信息面板
- 内容容器
- 列表项

**代码位置：**
- **实验室信息卡片**：`ui/LeftColumn.kt` 第 60-88 行
  ```60:88:ui/LeftColumn.kt
  Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
      Column(...) { /* 实验室信息内容 */ }
  }
  ```
- **实验室简介卡片**：`ui/LeftColumn.kt` 第 98-125 行
- **实验室规章制度卡片**：`ui/LeftColumn.kt` 第 135-162 行
- **课表卡片**：`ui/MiddleColumn.kt` 第 33-79 行
- **通知公告卡片**：`ui/RightColumn.kt` 第 60-87 行
- **校历卡片**：`ui/RightColumn.kt` 第 97-124 行
- **开门方式卡片**：`ui/RightColumn.kt` 第 138-198 行

**页面位置：**
- **左侧列卡片**：页面左侧，从上到下依次为实验室信息、简介、规章制度三个卡片
- **中间列卡片**：页面中间，占据最大空间，显示完整的实验室课表
- **右侧列卡片**：页面右侧，从上到下依次为通知公告、校历、开门方式三个卡片

**视觉效果：**
- 浅蓝色背景（surfaceVariant）
- 4dp 阴影效果
- 圆角边框
- 内容内边距 16dp

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`fillMaxWidth()` 填充宽度
   - **示例**：`modifier = Modifier.fillMaxWidth()`

2. **colors: CardColors**
   - **效果**：设置卡片的颜色方案
   - **本项目使用**：
     - `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)`
   - **视觉效果**：浅蓝色背景，与主题一致

3. **elevation: CardElevation**
   - **效果**：设置卡片的阴影高度（Elevation）
   - **本项目使用**：
     - `CardDefaults.cardElevation(defaultElevation = 4.dp)`
   - **视觉效果**：4dp 的阴影，产生浮起效果

#### 其他可用属性

1. **shape: Shape**
   - **效果**：设置卡片的形状（圆角大小）
   - **可选值**：
     - `MaterialTheme.shapes.small`：小圆角
     - `MaterialTheme.shapes.medium`：中等圆角（默认）
     - `MaterialTheme.shapes.large`：大圆角
     - `RoundedCornerShape(8.dp)`：自定义圆角
   - **使用示例**：
     ```kotlin
     Card(
         shape = MaterialTheme.shapes.large
     ) { /* 内容 */ }
     ```

2. **border: BorderStroke**
   - **效果**：设置卡片边框
   - **使用示例**：
     ```kotlin
     Card(
         border = BorderStroke(1.dp, Color.Gray)
     ) { /* 内容 */ }
     ```

3. **contentPadding: PaddingValues**
   - **效果**：设置卡片内容的内边距
   - **使用示例**：
     ```kotlin
     Card(
         contentPadding = PaddingValues(24.dp)
     ) { /* 内容 */ }
     ```

4. **enabled: Boolean**
   - **效果**：是否启用卡片（影响交互）
   - **默认值**：`true`

5. **onClick: (() -> Unit)?**
   - **效果**：点击事件回调（使卡片可点击）
   - **使用示例**：
     ```kotlin
     Card(
         onClick = { /* 处理点击 */ }
     ) { /* 内容 */ }
     ```

6. **interactionSource: MutableInteractionSource**
   - **效果**：自定义交互状态（如按下、悬停等）
   - **使用场景**：需要自定义交互反馈时

7. **content: @Composable RowScope.() -> Unit**
   - **效果**：卡片的内容区域
   - **说明**：Card 内部使用 Row 布局，适合水平排列内容

---

### 8. Button（按钮组件）

**功能说明：**
- `Button` 是 Material3 的标准按钮组件
- 支持点击事件处理
- 可自定义颜色、形状、大小

**使用场景：**
- 主要操作按钮
- 表单提交
- 导航按钮

**代码位置：**
- **切换界面按钮**：`ui/TopBar.kt` 第 58-65 行
  ```58:65:ui/TopBar.kt
  Button(
      onClick = onSwitchInterface,
      colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary
      )
  ) {
      Text("切换界面")
  }
  ```
- **退出系统按钮**：`ui/TopBar.kt` 第 67-74 行
- **开门方式按钮**：`ui/RightColumn.kt` 第 212-228 行
  ```212:228:ui/RightColumn.kt
  Button(
      onClick = onClick,
      modifier = modifier
          .height(80.dp)
          .fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
      ),
      shape = RoundedCornerShape(8.dp)
  ) {
      Text(text = text, ...)
  }
  ```
- **开门按钮调用**：`ui/RightColumn.kt` 第 168-177 行、第 185-194 行

**页面位置：**
- **顶部栏按钮**：页面顶部左侧，两个按钮水平排列
  - "切换界面"按钮：使用次要色（secondary），位于左侧
  - "退出系统"按钮：使用错误色（error），位于"切换界面"右侧
- **开门方式按钮**：页面右侧底部"开门方式"面板内，2x2 网格布局
  - 第一行：左侧"人脸识别开门"，右侧"密码开门"
  - 第二行：左侧"二维码开门"，右侧"刷卡开门"
  - 每个按钮高度 80dp，使用主色（primary），圆角 8dp

**按钮类型：**
- 主要按钮：使用 primary 颜色（开门方式按钮）
- 次要按钮：使用 secondary 颜色（切换界面按钮）
- 错误按钮：使用 error 颜色（退出系统按钮）

**参数属性说明：**

#### 已使用的属性

1. **onClick: () -> Unit**
   - **效果**：按钮点击时的回调函数
   - **本项目使用**：处理切换界面、退出系统、开门等操作
   - **示例**：`onClick = onSwitchInterface`

2. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：
     - `height(80.dp)` 设置按钮高度（开门方式按钮）
     - `fillMaxWidth()` 填充宽度（开门方式按钮）
   - **示例**：`modifier = modifier.height(80.dp).fillMaxWidth()`

3. **colors: ButtonColors**
   - **效果**：设置按钮的颜色方案
   - **本项目使用**：
     - `ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)` - 次要色
     - `ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)` - 错误色
     - `ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)` - 主色
   - **视觉效果**：不同颜色的按钮表示不同的操作重要性

4. **shape: Shape**
   - **效果**：设置按钮的形状（圆角）
   - **本项目使用**：`RoundedCornerShape(8.dp)` - 8dp 圆角（开门方式按钮）
   - **视觉效果**：圆角矩形按钮

#### 其他可用属性

1. **enabled: Boolean**
   - **效果**：是否启用按钮
   - **默认值**：`true`
   - **使用示例**：
     ```kotlin
     Button(
         enabled = isFormValid,
         onClick = { /* 提交 */ }
     ) { Text("提交") }
     ```

2. **contentPadding: PaddingValues**
   - **效果**：设置按钮内容的内边距
   - **默认值**：`ButtonDefaults.ContentPadding`
   - **使用示例**：
     ```kotlin
     Button(
         contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
     ) { Text("按钮") }
     ```

3. **elevation: ButtonElevation**
   - **效果**：设置按钮的阴影高度
   - **使用示例**：
     ```kotlin
     Button(
         elevation = ButtonDefaults.buttonElevation(
             defaultElevation = 8.dp,
             pressedElevation = 4.dp
         )
     ) { Text("按钮") }
     ```

4. **border: BorderStroke?**
   - **效果**：设置按钮边框
   - **使用示例**：
     ```kotlin
     Button(
         border = BorderStroke(2.dp, Color.Blue)
     ) { Text("按钮") }
     ```

5. **content: @Composable RowScope.() -> Unit**
   - **效果**：按钮的内容区域
   - **说明**：Button 内部使用 Row 布局，适合水平排列图标和文字
   - **使用示例**：
     ```kotlin
     Button(onClick = {}) {
         Icon(Icons.Default.Add, "添加")
         Spacer(Modifier.width(8.dp))
         Text("添加")
     }
     ```

6. **interactionSource: MutableInteractionSource**
   - **效果**：自定义交互状态
   - **使用场景**：需要监听按钮的按下、悬停等状态时

**Button 变体：**
- **TextButton**：文本按钮，无背景，适合次要操作
- **OutlinedButton**：轮廓按钮，有边框无填充
- **IconButton**：图标按钮，圆形，适合图标操作

---

### 9. Surface（表面组件）

**功能说明：**
- `Surface` 是一个可自定义的表面容器
- 提供背景色、阴影、形状等属性
- 常用于创建不同层次的视觉元素

**使用场景：**
- 标签背景
- 浮动元素
- 内容容器

**代码位置：**
- **周次标签**：`ui/MiddleColumn.kt` 第 59-69 行
  ```59:69:ui/MiddleColumn.kt
  Surface(
      color = MaterialTheme.colorScheme.primary,
      shape = MaterialTheme.shapes.small
  ) {
      Text(
          text = "当前周次: 第3周",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimary
      )
  }
  ```
- **课程项卡片**：`ui/MiddleColumn.kt` 第 130-142 行
  ```130:142:ui/MiddleColumn.kt
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      shape = MaterialTheme.shapes.small,
      shadowElevation = 2.dp
  ) {
      Text(
          text = course,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
      )
  }
  ```

**页面位置：**
- **周次标签**：课表卡片顶部右侧，显示"当前周次: 第3周"，蓝色背景（primary），小圆角，白色文字
- **课程项**：课表内容区域，每个课程名称显示在一个白色背景的小卡片中，带有 2dp 阴影，12dp 内边距

**视觉效果：**
- 周次标签：主色背景，小圆角，水平 12dp、垂直 6dp 内边距
- 课程项：表面色背景，小圆角，2dp 阴影，12dp 内边距

**参数属性说明：**

#### 已使用的属性

1. **color: Color**
   - **效果**：设置 Surface 的背景颜色
   - **本项目使用**：
     - `MaterialTheme.colorScheme.primary` - 主色（周次标签）
     - `MaterialTheme.colorScheme.surface` - 表面色（课程项）
   - **视觉效果**：不同颜色区分不同用途的元素

2. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：
     - `fillMaxWidth()` 填充宽度（课程项）
     - `padding()` 设置内边距（周次标签内的文字）
   - **示例**：`modifier = Modifier.fillMaxWidth()`

3. **shape: Shape**
   - **效果**：设置 Surface 的形状（圆角）
   - **本项目使用**：
     - `MaterialTheme.shapes.small` - 小圆角（周次标签、课程项）
   - **视觉效果**：圆角矩形

4. **shadowElevation: Dp**
   - **效果**：设置阴影高度（仅用于课程项）
   - **本项目使用**：`shadowElevation = 2.dp`
   - **视觉效果**：轻微的浮起效果

#### 其他可用属性

1. **tonalElevation: Dp**
   - **效果**：设置色调高度（影响背景色深浅）
   - **使用示例**：
     ```kotlin
     Surface(
         tonalElevation = 2.dp
     ) { /* 内容 */ }
     ```

2. **contentColor: Color**
   - **效果**：设置内容颜色（文字、图标等）
   - **默认值**：根据背景色自动计算
   - **使用示例**：
     ```kotlin
     Surface(
         contentColor = Color.White
     ) { Text("白色文字") }
     ```

3. **border: BorderStroke?**
   - **效果**：设置边框
   - **使用示例**：
     ```kotlin
     Surface(
         border = BorderStroke(1.dp, Color.Gray)
     ) { /* 内容 */ }
     ```

4. **content: @Composable () -> Unit**
   - **效果**：Surface 的内容区域
   - **说明**：Surface 内部使用 Box 布局，适合叠加内容

**Surface vs Card：**
- **Surface**：更轻量，适合简单容器、标签、浮动元素
- **Card**：更完整，有默认的内边距和交互效果，适合信息面板

---

### 10. Divider（分割线组件）

**功能说明：**
- `Divider` 用于在布局中创建视觉分割线
- 可以设置颜色、粗细、透明度
- 帮助区分不同的内容区域

**使用场景：**
- 列表项之间的分割
- 内容区域分隔
- 视觉层次划分

**代码位置：**
- **课程日期分割线**：`ui/MiddleColumn.kt` 第 147-150 行
  ```147:150:ui/MiddleColumn.kt
  Divider(
      modifier = Modifier.padding(vertical = 8.dp),
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
  )
  ```

**页面位置：**
- **课表分割线**：课表内容区域，每个日期（周一、周二等）的课程列表下方，用于分隔不同日期的课程
- 位于每个 `DayScheduleRow` 组件的底部，垂直方向 8dp 的内边距

**视觉效果：**
- 半透明灰色线条（outline 颜色，30% 透明度）
- 垂直方向 8dp 的内边距，使分割线上下有适当间距

**参数属性说明：**

#### 已使用的属性

1. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`padding(vertical = 8.dp)` 设置垂直内边距
   - **视觉效果**：分割线上下有 8dp 的间距

2. **color: Color**
   - **效果**：设置分割线的颜色
   - **本项目使用**：`MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)`
   - **视觉效果**：半透明灰色线条

#### 其他可用属性

1. **thickness: Dp**
   - **效果**：设置分割线的粗细
   - **默认值**：`1.dp`
   - **使用示例**：
     ```kotlin
     Divider(thickness = 2.dp)
     ```

2. **modifier: Modifier**（其他选项）
   - **水平分割线**：在 Row 中使用 `Modifier.width(density)` 或 `fillMaxWidth()`
   - **垂直分割线**：在 Column 中使用 `Modifier.height(density)` 或 `fillMaxHeight()`
   - **使用示例**：
     ```kotlin
     // 水平分割线
     Column {
         Text("上方")
         Divider(modifier = Modifier.fillMaxWidth())
         Text("下方")
     }
     
     // 垂直分割线
     Row {
         Text("左侧")
         Divider(
             modifier = Modifier
                 .fillMaxHeight()
                 .width(1.dp)
         )
         Text("右侧")
     }
     ```

**Divider 使用场景：**
- **列表项分隔**：在列表项之间添加分割线
- **内容区域分隔**：分隔不同的内容区域（如本项目中的日期分隔）
- **视觉层次**：帮助用户理解内容结构

---

## 文本和显示组件

### 11. Text（文本组件）

**功能说明：**
- `Text` 用于显示文本内容
- 支持 Material3 的 Typography 系统
- 可设置字体大小、颜色、粗细等

**使用场景：**
- 标题文本
- 正文内容
- 标签文本

**代码位置：**
- **主标题**：`ui/TopBar.kt` 第 100-105 行（顶部栏标题）
  ```100:105:ui/TopBar.kt
  Text(
      text = "本科生院 | 计算机基础实验教学中心",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onPrimaryContainer
  )
  ```
- **课表标题**：`ui/MiddleColumn.kt` 第 52-56 行
- **卡片标题**：`ui/LeftColumn.kt` 第 110-114 行、第 147-151 行；`ui/RightColumn.kt` 第 72-76 行、第 109-113 行、第 151-155 行
- **日期时间**：`ui/TopBar.kt` 第 113-117 行
- **天气信息**：`ui/TopBar.kt` 第 118-122 行
- **实验室信息值**：`ui/LeftColumn.kt` 第 180-190 行
- **课程名称**：`ui/MiddleColumn.kt` 第 117-122 行、第 136-141 行
- **按钮文字**：`ui/TopBar.kt` 第 64、73 行；`ui/RightColumn.kt` 第 222-227 行

**页面位置：**
- **顶部栏标题**：页面顶部中间，Logo 右侧，显示"本科生院 | 计算机基础实验教学中心"
- **日期时间**：页面顶部右侧，显示当前日期、星期和时间
- **天气信息**：页面顶部右侧，日期时间下方，显示"武汉洪山区:小雨 24℃ 91%RH"
- **课表标题**：中间列卡片顶部左侧，显示"实验室课表"
- **卡片标题**：各个信息卡片的顶部，如"实验室简介"、"通知公告"等
- **实验室信息**：左侧列第一个卡片内，显示实验室号、名称、安全等级等信息
- **课程名称**：课表内容区域，显示具体的课程名称
- **按钮文字**：所有按钮内部显示的文字

**Typography 样式使用：**
- `headlineSmall`：顶部栏标题、课表标题
- `titleMedium`：卡片标题（实验室简介、通知公告等）
- `bodyLarge`：课程名称
- `bodyMedium`：正文内容、按钮文字、实验室信息
- `bodySmall`：天气信息、次要文本
- `labelLarge`：周次标签

**参数属性说明：**

#### 已使用的属性

1. **text: String**
   - **效果**：显示的文本内容
   - **本项目使用**：各种文本内容，如标题、按钮文字、信息等

2. **modifier: Modifier**
   - **效果**：修改组件的外观和行为
   - **本项目使用**：`padding()` 设置内边距（课程名称、周次标签）

3. **style: TextStyle**
   - **效果**：设置文本样式（字体大小、行高、字间距等）
   - **本项目使用**：
     - `MaterialTheme.typography.headlineSmall` - 小标题样式
     - `MaterialTheme.typography.titleMedium` - 中等标题样式
     - `MaterialTheme.typography.bodyLarge` - 大号正文样式
     - `MaterialTheme.typography.bodyMedium` - 中等正文样式
     - `MaterialTheme.typography.bodySmall` - 小号正文样式
     - `MaterialTheme.typography.labelLarge` - 大号标签样式

4. **color: Color**
   - **效果**：设置文本颜色
   - **本项目使用**：
     - `MaterialTheme.colorScheme.onPrimaryContainer` - 主色容器上的文字
     - `MaterialTheme.colorScheme.onSurfaceVariant` - 表面变体上的文字
     - `MaterialTheme.colorScheme.onPrimary` - 主色上的文字
     - `Color.White` - 白色文字（Logo）
     - `MaterialTheme.colorScheme.primary` - 主色文字（日期标签）

5. **fontWeight: FontWeight?**
   - **效果**：设置字体粗细
   - **本项目使用**：
     - `FontWeight.Bold` - 粗体（顶部栏标题）
     - `FontWeight.Medium` - 中等粗细（按钮文字）

6. **fontSize: TextUnit**
   - **效果**：设置字体大小（覆盖 TextStyle 中的大小）
   - **本项目使用**：`24.sp` - Logo 文字大小

#### 其他可用属性

1. **textAlign: TextAlign?**
   - **效果**：设置文本对齐方式
   - **可选值**：
     - `TextAlign.Left`：左对齐（默认）
     - `TextAlign.Center`：居中
     - `TextAlign.Right`：右对齐
     - `TextAlign.Justify`：两端对齐
     - `TextAlign.Start`：开始对齐（根据语言方向）
     - `TextAlign.End`：结束对齐（根据语言方向）
   - **使用示例**：
     ```kotlin
     Text(
         text = "居中文本",
         textAlign = TextAlign.Center
     )
     ```

2. **maxLines: Int**
   - **效果**：设置最大行数
   - **默认值**：`Int.MAX_VALUE`（无限制）
   - **使用示例**：
     ```kotlin
     Text(
         text = "长文本...",
         maxLines = 2,
         overflow = TextOverflow.Ellipsis
     )
     ```

3. **overflow: TextOverflow**
   - **效果**：文本溢出时的处理方式
   - **可选值**：
     - `TextOverflow.Clip`：裁剪（默认）
     - `TextOverflow.Ellipsis`：显示省略号
     - `TextOverflow.Visible`：显示溢出内容
   - **使用示例**：
     ```kotlin
     Text(
         text = "长文本...",
         maxLines = 1,
         overflow = TextOverflow.Ellipsis
     )
     ```

4. **lineHeight: TextUnit**
   - **效果**：设置行高
   - **使用示例**：
     ```kotlin
     Text(
         text = "多行文本",
         lineHeight = 24.sp
     )
     ```

5. **letterSpacing: TextUnit**
   - **效果**：设置字符间距
   - **使用示例**：
     ```kotlin
     Text(
         text = "文字",
         letterSpacing = 2.sp
     )
     ```

6. **textDecoration: TextDecoration?**
   - **效果**：设置文本装饰（下划线、删除线等）
   - **可选值**：
     - `TextDecoration.Underline`：下划线
     - `TextDecoration.LineThrough`：删除线
     - `TextDecoration.None`：无装饰
   - **使用示例**：
     ```kotlin
     Text(
         text = "下划线文本",
         textDecoration = TextDecoration.Underline
     )
     ```

7. **fontFamily: FontFamily?**
   - **效果**：设置字体族
   - **使用示例**：
     ```kotlin
     Text(
         text = "文本",
         fontFamily = FontFamily.Serif
     )
     ```

8. **onTextLayout: ((TextLayoutResult) -> Unit)?**
   - **效果**：文本布局完成后的回调
   - **使用场景**：需要获取文本布局信息时

**Typography 样式完整列表：**
- `displayLarge`、`displayMedium`、`displaySmall`：超大标题
- `headlineLarge`、`headlineMedium`、`headlineSmall`：大标题
- `titleLarge`、`titleMedium`、`titleSmall`：标题
- `bodyLarge`、`bodyMedium`、`bodySmall`：正文
- `labelLarge`、`labelMedium`、`labelSmall`：标签

---

## 交互组件

### 12. 滚动修饰符（Scroll Modifiers）

**功能说明：**
- `verticalScroll` 和 `horizontalScroll` 使内容可滚动
- `rememberScrollState` 用于记住滚动状态
- 适用于内容超出屏幕的情况

**使用场景：**
- 长列表
- 可滚动内容区域
- 表单内容

**代码位置：**
- **左侧列滚动**：`ui/LeftColumn.kt` 第 32-34 行
  ```32:34:ui/LeftColumn.kt
  Column(
      modifier = modifier
          .verticalScroll(rememberScrollState()),
  ```
- **中间列滚动**：`ui/MiddleColumn.kt` 第 40-44 行
  ```40:44:ui/MiddleColumn.kt
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
  ```
- **右侧列滚动**：`ui/RightColumn.kt` 第 37-39 行
  ```37:39:ui/RightColumn.kt
  Column(
      modifier = modifier
          .verticalScroll(rememberScrollState()),
  ```

**页面位置：**
- **左侧列**：页面左侧，当三个卡片（实验室信息、简介、规章制度）的总高度超过可用空间时，可以垂直滚动查看所有内容
- **中间列**：页面中间，当课表内容（周一到周五的课程）超过卡片高度时，可以垂直滚动查看所有课程
- **右侧列**：页面右侧，当三个面板（通知公告、校历、开门方式）的总高度超过可用空间时，可以垂直滚动查看所有内容

**交互效果：**
- 当内容超出容器高度时，用户可以通过触摸滑动来滚动查看隐藏的内容
- 滚动状态会被记住，保持滚动位置

**参数属性说明：**

#### 已使用的属性

1. **state: ScrollState**
   - **效果**：滚动状态对象，用于控制和监听滚动位置
   - **本项目使用**：`rememberScrollState()` 创建并记住滚动状态
   - **示例**：`modifier = modifier.verticalScroll(rememberScrollState())`

2. **modifier: Modifier**
   - **效果**：与 `verticalScroll()` 或 `horizontalScroll()` 组合使用
   - **本项目使用**：`verticalScroll()` 启用垂直滚动

#### 其他可用属性

1. **verticalScroll(state: ScrollState, enabled: Boolean = true, flingBehavior: FlingBehavior? = null, reverseScrolling: Boolean = false)**
   - **state**：滚动状态（必需）
   - **enabled**：是否启用滚动（默认 `true`）
   - **flingBehavior**：惯性滚动行为（默认使用系统行为）
   - **reverseScrolling**：是否反向滚动（默认 `false`）
   - **使用示例**：
     ```kotlin
     Column(
         modifier = Modifier.verticalScroll(
             state = scrollState,
             enabled = isScrollEnabled,
             reverseScrolling = false
         )
     ) { /* 内容 */ }
     ```

2. **horizontalScroll(state: ScrollState, enabled: Boolean = true, flingBehavior: FlingBehavior? = null, reverseScrolling: Boolean = false)**
   - **效果**：启用水平滚动
   - **参数**：与 `verticalScroll` 相同
   - **使用示例**：
     ```kotlin
     Row(
         modifier = Modifier.horizontalScroll(scrollState)
     ) { /* 内容 */ }
     ```

3. **ScrollState 的属性和方法：**
   - **value: Int**：当前滚动位置（像素）
   - **maxValue: Int**：最大滚动位置
   - **isScrollInProgress: Boolean**：是否正在滚动
   - **animateScrollTo(value: Int)**：动画滚动到指定位置
   - **scrollTo(value: Int)**：立即滚动到指定位置
   - **使用示例**：
     ```kotlin
     val scrollState = rememberScrollState()
     
     // 滚动到底部
     LaunchedEffect(Unit) {
         scrollState.animateScrollTo(scrollState.maxValue)
     }
     
     Column(
         modifier = Modifier.verticalScroll(scrollState)
     ) { /* 内容 */ }
     ```

**滚动修饰符组合：**
- 可以同时使用 `verticalScroll` 和 `horizontalScroll` 实现双向滚动
- 可以与 `fillMaxSize()`、`fillMaxWidth()` 等修饰符组合使用

---

## 修饰符和效果

### 13. Modifier（修饰符系统）

**功能说明：**
- `Modifier` 是 Compose 中用于修改组件外观和行为的系统
- 可以链式调用多个修饰符
- 顺序很重要，后面的修饰符会覆盖前面的效果

**常用修饰符：**

#### 尺寸修饰符
```kotlin
Modifier
    .fillMaxSize()      // 填充父容器
    .fillMaxWidth()     // 填充宽度
    .fillMaxHeight()    // 填充高度
    .size(48.dp)        // 固定尺寸
    .width(100.dp)      // 固定宽度
    .height(80.dp)      // 固定高度
```

#### 内边距修饰符
```kotlin
Modifier
    .padding(16.dp)                    // 统一内边距
    .padding(horizontal = 16.dp)       // 水平内边距
    .padding(vertical = 8.dp)          // 垂直内边距
    .padding(start = 12.dp, end = 12.dp) // 指定方向内边距
```

#### 背景修饰符
```kotlin
Modifier
    .background(
        MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    )
```

#### 间距修饰符
```kotlin
Arrangement.spacedBy(16.dp)  // 组件之间间距
```

**在本项目中的应用：**
```kotlin
// 组合使用多个修饰符
Card(
    modifier = Modifier
        .fillMaxWidth()              // 填充宽度
        .padding(16.dp)              // 外边距
        .background(...)             // 背景色
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)          // 内边距
            .verticalScroll(...)     // 可滚动
    ) { /* 内容 */ }
}
```

**参数属性详细说明：**

#### 尺寸修饰符（Size Modifiers）

1. **fillMaxSize(fraction: Float = 1f)**
   - **效果**：填充父容器的全部尺寸
   - **参数**：`fraction` - 填充比例（0.0 到 1.0，默认 1.0）
   - **本项目使用**：主界面 Column、主内容区 Row
   - **示例**：`Modifier.fillMaxSize()` 或 `Modifier.fillMaxSize(0.8f)` 填充 80%

2. **fillMaxWidth(fraction: Float = 1f)**
   - **效果**：填充父容器的全部宽度
   - **参数**：`fraction` - 填充比例
   - **本项目使用**：所有 Card、顶部栏 Row
   - **示例**：`Modifier.fillMaxWidth()` 或 `Modifier.fillMaxWidth(0.5f)` 填充 50%

3. **fillMaxHeight(fraction: Float = 1f)**
   - **效果**：填充父容器的全部高度
   - **参数**：`fraction` - 填充比例
   - **本项目使用**：三列布局中的列
   - **示例**：`Modifier.fillMaxHeight()`

4. **size(width: Dp, height: Dp)** 或 **size(size: Dp)**
   - **效果**：设置固定尺寸
   - **本项目使用**：Logo 容器 `size(48.dp)`
   - **示例**：`Modifier.size(48.dp)` 或 `Modifier.size(width = 100.dp, height = 50.dp)`

5. **width(width: Dp)**
   - **效果**：设置固定宽度
   - **示例**：`Modifier.width(100.dp)`

6. **height(height: Dp)**
   - **效果**：设置固定高度
   - **本项目使用**：开门方式按钮 `height(80.dp)`、Spacer `height(8.dp)`
   - **示例**：`Modifier.height(80.dp)`

7. **weight(weight: Float, fill: Boolean = true)**
   - **效果**：在 Row/Column 中按比例分配空间
   - **参数**：
     - `weight` - 权重比例
     - `fill` - 是否填充剩余空间（默认 true）
   - **本项目使用**：三列布局 `weight(1f)` 和 `weight(2f)`
   - **示例**：`Modifier.weight(1f)` 或 `Modifier.weight(2f, fill = false)`

8. **defaultMinSize(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified)**
   - **效果**：设置最小尺寸
   - **示例**：`Modifier.defaultMinSize(minWidth = 100.dp)`

#### 内边距修饰符（Padding Modifiers）

1. **padding(all: Dp)**
   - **效果**：设置统一的内边距
   - **本项目使用**：`padding(16.dp)` - 所有方向 16dp
   - **示例**：`Modifier.padding(16.dp)`

2. **padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp)**
   - **效果**：分别设置水平和垂直内边距
   - **本项目使用**：`padding(horizontal = 12.dp, vertical = 6.dp)` - 周次标签
   - **示例**：`Modifier.padding(horizontal = 16.dp, vertical = 8.dp)`

3. **padding(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp)**
   - **效果**：分别设置四个方向的内边距
   - **示例**：`Modifier.padding(start = 12.dp, end = 12.dp)`

4. **padding(paddingValues: PaddingValues)**
   - **效果**：使用 PaddingValues 对象设置内边距
   - **示例**：`Modifier.padding(PaddingValues(16.dp))`

#### 背景修饰符（Background Modifiers）

1. **background(color: Color, shape: Shape = RectangleShape)**
   - **效果**：设置背景颜色和形状
   - **本项目使用**：
     - 顶部栏：`background(MaterialTheme.colorScheme.primaryContainer)`
     - Logo：`background(Color(0xFF2196F3), shape = MaterialTheme.shapes.medium)`
   - **示例**：`Modifier.background(Color.Blue, RoundedCornerShape(8.dp))`

2. **background(brush: Brush, shape: Shape = RectangleShape, alpha: Float = 1.0f)**
   - **效果**：使用渐变画笔作为背景
   - **示例**：`Modifier.background(Brush.horizontalGradient(listOf(Color.Blue, Color.Red)))`

#### 边框修饰符（Border Modifiers）

1. **border(width: Dp, color: Color, shape: Shape = RectangleShape)**
   - **效果**：设置边框
   - **示例**：`Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))`

2. **border(width: Dp, brush: Brush, shape: Shape = RectangleShape)**
   - **效果**：使用渐变画笔作为边框
   - **示例**：`Modifier.border(2.dp, Brush.linearGradient(...))`

#### 对齐修饰符（Alignment Modifiers）

1. **align(alignment: Alignment)**（在 Box 中使用）
   - **效果**：在 Box 内对齐单个子组件
   - **可选值**：`Alignment.TopStart`、`Alignment.Center`、`Alignment.BottomEnd` 等
   - **示例**：`Modifier.align(Alignment.Center)`

2. **wrapContentSize(align: Alignment = Alignment.Center, unbounded: Boolean = false)**
   - **效果**：使组件只占用内容所需的空间
   - **示例**：`Modifier.wrapContentSize(Alignment.Center)`

#### 滚动修饰符（Scroll Modifiers）

1. **verticalScroll(state: ScrollState, enabled: Boolean = true, ...)**
   - **效果**：启用垂直滚动
   - **本项目使用**：左侧列、中间列、右侧列
   - **示例**：`Modifier.verticalScroll(rememberScrollState())`

2. **horizontalScroll(state: ScrollState, enabled: Boolean = true, ...)**
   - **效果**：启用水平滚动
   - **示例**：`Modifier.horizontalScroll(rememberScrollState())`

#### 点击修饰符（Click Modifiers）

1. **clickable(onClick: () -> Unit, ...)**
   - **效果**：使组件可点击
   - **示例**：`Modifier.clickable { /* 处理点击 */ }`

2. **combinedClickable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, ...)**
   - **效果**：支持点击和长按
   - **示例**：`Modifier.combinedClickable(onClick = {}, onLongClick = {})`

#### 其他常用修饰符

1. **alpha(alpha: Float)**
   - **效果**：设置透明度（0.0 到 1.0）
   - **示例**：`Modifier.alpha(0.5f)` - 50% 透明度

2. **rotate(degrees: Float)**
   - **效果**：旋转组件
   - **示例**：`Modifier.rotate(45f)` - 旋转 45 度

3. **scale(scale: Float)**
   - **效果**：缩放组件
   - **示例**：`Modifier.scale(1.2f)` - 放大 20%

4. **offset(x: Dp = 0.dp, y: Dp = 0.dp)**
   - **效果**：偏移组件位置
   - **示例**：`Modifier.offset(x = 10.dp, y = 5.dp)`

5. **clip(shape: Shape)**
   - **效果**：裁剪组件形状
   - **示例**：`Modifier.clip(RoundedCornerShape(8.dp))`

**修饰符链式调用顺序：**
修饰符的顺序很重要，建议顺序：
1. 尺寸修饰符（size、fillMaxSize 等）
2. 布局修饰符（weight、padding 等）
3. 视觉修饰符（background、border 等）
4. 交互修饰符（clickable、scroll 等）

---

### 14. MaterialTheme（主题系统）

**功能说明：**
- `MaterialTheme` 提供统一的颜色、字体、形状主题
- 支持浅色和深色主题
- 确保整个应用的设计一致性

**颜色方案：**
```kotlin
MaterialTheme.colorScheme.primary           // 主色
MaterialTheme.colorScheme.secondary         // 次要色
MaterialTheme.colorScheme.error             // 错误色
MaterialTheme.colorScheme.background        // 背景色
MaterialTheme.colorScheme.surface           // 表面色
MaterialTheme.colorScheme.surfaceVariant    // 表面变体色
MaterialTheme.colorScheme.onPrimary         // 主色上的文字颜色
MaterialTheme.colorScheme.onSurface         // 表面上的文字颜色
```

**Typography 系统：**
```kotlin
MaterialTheme.typography.headlineSmall
MaterialTheme.typography.titleMedium
MaterialTheme.typography.bodyLarge
MaterialTheme.typography.bodyMedium
```

**形状系统：**
```kotlin
MaterialTheme.shapes.small      // 小圆角
MaterialTheme.shapes.medium     // 中等圆角
MaterialTheme.shapes.large      // 大圆角
```

**在本项目中的应用：**
- 所有组件都使用 MaterialTheme 的颜色和样式
- 确保视觉一致性
- 支持主题切换（浅色/深色）

**参数属性详细说明：**

#### MaterialTheme 颜色方案（ColorScheme）

**主要颜色：**
1. **primary: Color**
   - **效果**：应用的主色调
   - **本项目使用**：Logo 背景、周次标签、开门方式按钮
   - **用途**：重要元素、主要操作按钮

2. **onPrimary: Color**
   - **效果**：主色上的文字/图标颜色
   - **本项目使用**：周次标签文字、开门方式按钮文字
   - **用途**：确保在主色背景上的文字可读性

3. **primaryContainer: Color**
   - **效果**：主色容器背景
   - **本项目使用**：顶部栏背景
   - **用途**：主色的浅色变体，用于背景

4. **onPrimaryContainer: Color**
   - **效果**：主色容器上的文字颜色
   - **本项目使用**：顶部栏标题、日期时间文字
   - **用途**：确保在主色容器上的文字可读性

**次要颜色：**
5. **secondary: Color**
   - **效果**：应用的次要色调
   - **本项目使用**：切换界面按钮
   - **用途**：次要操作、强调元素

6. **onSecondary: Color**
   - **效果**：次要色上的文字颜色
   - **用途**：确保在次要色背景上的文字可读性

**错误颜色：**
7. **error: Color**
   - **效果**：错误/危险操作的色调
   - **本项目使用**：退出系统按钮
   - **用途**：错误提示、危险操作按钮

8. **onError: Color**
   - **效果**：错误色上的文字颜色
   - **用途**：确保在错误色背景上的文字可读性

**表面颜色：**
9. **surface: Color**
   - **效果**：表面/卡片背景色
   - **本项目使用**：课程项背景
   - **用途**：卡片、对话框、菜单背景

10. **onSurface: Color**
    - **效果**：表面上的文字颜色
    - **本项目使用**：课程名称文字
    - **用途**：确保在表面上的文字可读性

11. **surfaceVariant: Color**
    - **效果**：表面变体背景色
    - **本项目使用**：所有 Card 的背景色
    - **用途**：信息面板、次要卡片背景

12. **onSurfaceVariant: Color**
    - **效果**：表面变体上的文字颜色
    - **本项目使用**：卡片标题、卡片内容文字
    - **用途**：确保在表面变体上的文字可读性

**背景颜色：**
13. **background: Color**
    - **效果**：应用背景色
    - **本项目使用**：主界面背景
    - **用途**：整个应用的背景

14. **onBackground: Color**
    - **效果**：背景上的文字颜色
    - **用途**：确保在背景上的文字可读性

**其他颜色：**
15. **outline: Color**
    - **效果**：轮廓/边框颜色
    - **本项目使用**：Divider 分割线颜色
    - **用途**：边框、分割线、输入框轮廓

16. **outlineVariant: Color**
    - **效果**：轮廓变体颜色
    - **用途**：次要边框、分割线

#### MaterialTheme Typography（字体系统）

**显示样式（Display）：**
- `displayLarge`、`displayMedium`、`displaySmall`
- **用途**：超大标题，用于欢迎页面、启动页面

**标题样式（Headline）：**
- `headlineLarge`、`headlineMedium`、`headlineSmall`
- **本项目使用**：`headlineSmall` - 顶部栏标题、课表标题
- **用途**：页面标题、主要标题

**标题样式（Title）：**
- `titleLarge`、`titleMedium`、`titleSmall`
- **本项目使用**：`titleMedium` - 卡片标题
- **用途**：卡片标题、章节标题

**正文样式（Body）：**
- `bodyLarge`、`bodyMedium`、`bodySmall`
- **本项目使用**：
  - `bodyLarge` - 课程名称
  - `bodyMedium` - 正文内容、按钮文字、实验室信息
  - `bodySmall` - 天气信息、次要文本
- **用途**：正文内容、描述文字

**标签样式（Label）：**
- `labelLarge`、`labelMedium`、`labelSmall`
- **本项目使用**：`labelLarge` - 周次标签
- **用途**：标签、按钮文字、输入框标签

**Typography 属性：**
每个 Typography 样式包含：
- `fontSize: TextUnit` - 字体大小
- `fontWeight: FontWeight?` - 字体粗细
- `lineHeight: TextUnit` - 行高
- `letterSpacing: TextUnit` - 字符间距
- `fontFamily: FontFamily?` - 字体族

#### MaterialTheme Shapes（形状系统）

1. **small: Shape**
   - **效果**：小圆角（4dp）
   - **本项目使用**：周次标签、课程项、按钮
   - **用途**：小元素、标签、按钮

2. **medium: Shape**
   - **效果**：中等圆角（12dp，默认）
   - **本项目使用**：Logo 容器
   - **用途**：卡片、对话框、中等元素

3. **large: Shape**
   - **效果**：大圆角（16dp）
   - **用途**：大卡片、底部表单、大元素

**自定义形状：**
- `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)` - 自定义圆角
- `CircleShape` - 圆形
- `RectangleShape` - 矩形（无圆角）
- `CutCornerShape` - 切角形状

**使用示例：**
```kotlin
// 使用主题形状
Card(shape = MaterialTheme.shapes.medium) { /* 内容 */ }

// 自定义形状
Surface(shape = RoundedCornerShape(8.dp)) { /* 内容 */ }
```

#### MaterialTheme 使用最佳实践

1. **颜色使用原则：**
   - 主要操作使用 `primary` 和 `onPrimary`
   - 次要操作使用 `secondary` 和 `onSecondary`
   - 错误操作使用 `error` 和 `onError`
   - 背景使用 `background` 和 `onBackground`
   - 卡片使用 `surface`/`surfaceVariant` 和 `onSurface`/`onSurfaceVariant`

2. **Typography 使用原则：**
   - 页面标题使用 `headlineSmall` 或 `headlineMedium`
   - 卡片标题使用 `titleMedium`
   - 正文使用 `bodyMedium` 或 `bodyLarge`
   - 标签使用 `labelLarge` 或 `labelMedium`

3. **Shape 使用原则：**
   - 小元素（按钮、标签）使用 `small`
   - 卡片、对话框使用 `medium`（默认）
   - 大元素使用 `large`

4. **主题切换：**
   - MaterialTheme 自动支持浅色和深色主题
   - 使用 `isSystemInDarkTheme()` 检测系统主题
   - 颜色方案会根据主题自动调整

---

## 布局组合模式

### 15. 响应式布局

**三列布局模式：**
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    LeftColumn(modifier = Modifier.weight(1f))    // 左侧：1份
    MiddleColumn(modifier = Modifier.weight(2f))  // 中间：2份（主要区域）
    RightColumn(modifier = Modifier.weight(1f))   // 右侧：1份
}
```

**代码位置：**
- **主内容区三列布局**：`ui/LabDashboardScreen.kt` 第 38-64 行
  ```38:64:ui/LabDashboardScreen.kt
  Row(
      modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
      // 左侧列：实验室信息相关
      LeftColumn(
          modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
      )
      
      // 中间列：实验室课表（主要功能）
      MiddleColumn(
          modifier = Modifier
              .weight(2f)
              .fillMaxHeight()
      )
      
      // 右侧列：通知公告、校历、开门方式
      RightColumn(
          modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
      )
  }
  ```

**页面位置：**
- **主内容区域**：页面中间部分（顶部栏下方），占据整个屏幕的剩余空间
- **左侧列**：占据主内容区域宽度的 1/4（weight=1）
- **中间列**：占据主内容区域宽度的 1/2（weight=2），是主要功能区域
- **右侧列**：占据主内容区域宽度的 1/4（weight=1）
- 三列之间各有 16dp 的水平间距

**效果：**
- 使用 `weight` 实现响应式布局
- 中间列占据更多空间（2:1:1 比例）
- 自动适应不同屏幕尺寸
- 当屏幕宽度变化时，三列按比例自动调整宽度

---

### 16. 网格布局

**2x2 按钮网格：**
```kotlin
Column {
    Row {
        Button(...)  // 第一行第一个
        Button(...)  // 第一行第二个
    }
    Row {
        Button(...)  // 第二行第一个
        Button(...)  // 第二行第二个
    }
}
```

**代码位置：**
- **开门方式按钮网格**：`ui/RightColumn.kt` 第 160-196 行
  ```160:196:ui/RightColumn.kt
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
      // 第一行：人脸识别和密码开门
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
          DoorOpeningButton(
              text = "人脸识别开门",
              onClick = { ... },
              modifier = Modifier.weight(1f)
          )
          DoorOpeningButton(
              text = "密码开门",
              onClick = { ... },
              modifier = Modifier.weight(1f)
          )
      }
      
      // 第二行：二维码和刷卡开门
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
          DoorOpeningButton(
              text = "二维码开门",
              onClick = { ... },
              modifier = Modifier.weight(1f)
          )
          DoorOpeningButton(
              text = "刷卡开门",
              onClick = { ... },
              modifier = Modifier.weight(1f)
          )
      }
  }
  ```

**页面位置：**
- **开门方式面板**：页面右侧底部，"开门方式"卡片内
- **第一行**：上方，左侧"人脸识别开门"按钮，右侧"密码开门"按钮
- **第二行**：下方，左侧"二维码开门"按钮，右侧"刷卡开门"按钮
- 每行两个按钮平均分配宽度（weight=1f），按钮之间 12dp 水平间距
- 两行之间 12dp 垂直间距

**效果：**
- 使用 Row + Column 组合实现 2x2 网格布局
- 每个按钮使用 `weight(1f)` 平均分配空间
- 按钮之间使用 `spacedBy(12.dp)` 添加间距
- 响应式设计，按钮宽度自动适应容器宽度

---

## 实际效果展示

### 界面布局结构

```
┌─────────────────────────────────────────────────────────┐
│  TopBar（顶部栏）                                        │
│  [切换界面] [退出系统]  Logo + 标题    时间 + 天气      │
├──────────────┬──────────────────────┬───────────────────┤
│              │                      │                   │
│ LeftColumn   │   MiddleColumn      │   RightColumn     │
│              │                      │                   │
│ 实验室信息   │   实验室课表         │   通知公告        │
│ 实验室简介   │   (当前周次:第3周)   │   校历            │
│ 规章制度     │                      │   开门方式        │
│              │   周一课程...        │   [人脸识别]      │
│              │   周二课程...        │   [密码开门]      │
│              │   ...                │   [二维码]        │
│              │                      │   [刷卡开门]      │
└──────────────┴──────────────────────┴───────────────────┘
```

### 颜色方案

- **主色（Primary）**：用于重要元素（Logo、周次标签、开门按钮）
- **次要色（Secondary）**：用于次要操作（切换界面按钮）
- **错误色（Error）**：用于危险操作（退出系统按钮）
- **表面变体色（SurfaceVariant）**：用于卡片背景
- **背景色（Background）**：主界面背景

### 间距系统

- **大间距（16dp）**：主要区域之间的间距
- **中间距（12dp）**：相关组件之间的间距
- **小间距（8dp）**：紧密相关元素之间的间距
- **内边距（16dp）**：卡片内部内容的内边距

---

## 最佳实践

### 1. 组件拆分
- 将界面拆分为独立的功能组件
- 每个组件负责单一职责
- 便于维护和复用

### 2. 注释规范
- 为每个组件添加功能说明注释
- 说明使用的 Compose 组件
- 标注数据来源和交互逻辑

### 3. 响应式设计
- 使用 `weight` 实现响应式布局
- 使用 `fillMaxWidth/Height` 适应父容器
- 考虑不同屏幕尺寸

### 4. 主题一致性
- 统一使用 MaterialTheme
- 保持颜色、字体、形状的一致性
- 支持主题切换

### 5. 性能优化
- 使用 `remember` 缓存计算结果
- 使用 `LazyColumn` 处理长列表（如需要）
- 避免不必要的重组

---

## 组件快速索引

### 按文件位置索引

#### ui/LabDashboardScreen.kt
- **Column**（第 25-35 行）：主界面垂直布局
- **Row**（第 38-64 行）：三列水平布局（左中右）

#### ui/TopBar.kt
- **Box**（第 48-52 行）：顶部栏容器
- **Row**（第 53-129 行）：顶部栏主布局
- **Row**（第 59-81 行）：左侧按钮区域
- **Row**（第 84-108 行）：中间标题Logo区域
- **Box**（第 89-98 行）：Logo 图标容器
- **Image**（第 94-97 行）：Logo 图片
- **Column**（第 111-127 行）：右侧时间天气区域
- **Button**（第 64-71 行）：切换界面按钮
- **Button**（第 73-80 行）：退出系统按钮
- **Text**（第 100-107 行）：主标题
- **Text**（第 115-120、121-126 行）：日期时间和天气

#### ui/LeftColumn.kt
- **Column**（第 32-45 行）：左侧列主布局
- **Card**（第 60-88 行）：实验室信息卡片
- **Card**（第 98-125 行）：实验室简介卡片
- **Card**（第 135-162 行）：实验室规章制度卡片
- **Column**（第 67-87 行）：实验室信息内容
- **Row**（第 176-191 行）：信息行（标签+值）
- **Spacer**（第 116、153 行）：标题和内容间距
- **Text**（第 110-114、147-151 行）：卡片标题
- **verticalScroll**（第 34 行）：左侧列滚动

#### ui/MiddleColumn.kt
- **Card**（第 147-183 行）：课表卡片
- **Column**（第 154-181 行）：课表内容区域
- **Row**（第 161-172 行）：课表标题行
- **BoxWithConstraints**（第 208-217、223-235 行）：响应式布局容器（表头和表格宽度同步）
- **Column**（第 207-236 行）：课程表网格容器
- **MergedTableLayout**（第 375-442 行）：自定义合并单元格布局
- **TableHeader**（第 448-477 行）：表头组件
- **SectionCell**（第 483-506 行）：节次单元格
- **CourseCell**（第 512-579 行）：课程单元格
- **EmptyCell**（第 585-591 行）：空单元格
- **TableCell**（第 597-619 行）：表头单元格
- **Text**（第 166-171 行）：课表标题
- **verticalScroll**（第 158 行）：课表滚动
- **border**（第 233、456、490、519、589、604 行）：边框修饰符
- **offset**（第 415 行）：偏移修饰符

#### ui/RightColumn.kt
- **Column**（第 37-50 行）：右侧列主布局
- **Card**（第 60-87 行）：通知公告卡片
- **Card**（第 97-124 行）：校历卡片
- **Card**（第 138-198 行）：开门方式卡片
- **Column**（第 160-196 行）：按钮网格布局
- **Row**（第 164-178 行）：第一行按钮
- **Row**（第 181-195 行）：第二行按钮
- **Button**（第 212-228 行）：开门方式按钮组件
- **Spacer**（第 78、115、157 行）：标题和内容间距
- **Text**（第 72-76、109-113、151-155 行）：卡片标题
- **verticalScroll**（第 39 行）：右侧列滚动

---

## 总结

本项目使用了以下主要的 Jetpack Compose 组件：

1. **布局组件**：Column、Row、Box、BoxWithConstraints、Spacer
2. **图像组件**：Image
3. **Material3 组件**：Card、Button、Surface、Divider
4. **文本组件**：Text（配合 Typography）
5. **修饰符**：padding、background、fillMaxSize、weight、verticalScroll、border、offset 等
6. **主题系统**：MaterialTheme（颜色、字体、形状）

通过这些组件的组合使用，实现了一个功能完整、视觉美观、响应式的实验室管理系统界面。所有组件都遵循 Material3 设计规范，确保了良好的用户体验和视觉一致性。

### 界面布局总结

**顶部栏（TopBar）**
- 位置：页面最顶部
- 组件：Box（容器）、Row（主布局）、Button（操作按钮）、Text（标题和时间）
- 文件：`ui/TopBar.kt`

**左侧列（LeftColumn）**
- 位置：页面左侧（占 1/4 宽度）
- 组件：Column（主布局）、Card（3个信息卡片）、Text（标题和内容）
- 文件：`ui/LeftColumn.kt`

**中间列（MiddleColumn）**
- 位置：页面中间（占 1/2 宽度）
- 组件：Card（课表卡片）、Column（课程列表）、Surface（周次标签和课程项）、Divider（分割线）
- 文件：`ui/MiddleColumn.kt`

**右侧列（RightColumn）**
- 位置：页面右侧（占 1/4 宽度）
- 组件：Column（主布局）、Card（3个功能面板）、Button（开门方式按钮，2x2网格）
- 文件：`ui/RightColumn.kt`

所有组件都通过 `ui/LabDashboardScreen.kt` 中的主界面进行组合和布局。

---

## 未使用但常用的 Compose 组件

以下组件在当前项目中未使用，但在实际开发中非常常用。本节提供这些组件的说明和应用示例。

### 17. LazyColumn / LazyRow（懒加载列表）

**功能说明：**
- `LazyColumn` 和 `LazyRow` 是用于显示大量数据的懒加载列表组件
- 只渲染可见区域内的项目，性能优异
- 支持滚动、项目动画、粘性头部等功能

**使用场景：**
- 长列表数据展示
- 可滚动内容列表
- 聊天消息列表
- 商品列表

**代码示例：**
```kotlin
@Composable
fun CourseList(courses: List<CourseScheduleDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(courses) { course ->
            CourseItem(course = course)
        }
    }
}

@Composable
fun CourseItem(course: CourseScheduleDto) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = course.courseName ?: "",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = course.teacherName ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

**LazyRow 示例（水平滚动）：**
```kotlin
@Composable
fun HorizontalCourseList(courses: List<CourseScheduleDto>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(courses) { course ->
            CourseCard(course = course)
        }
    }
}
```

**高级功能：**
- **粘性头部**：`stickyHeader { }`
- **项目动画**：`item { }` 配合 `AnimatedVisibility`
- **索引访问**：`itemsIndexed(items) { index, item -> }`

---

### 18. LazyVerticalGrid / LazyHorizontalGrid（懒加载网格）

**功能说明：**
- `LazyVerticalGrid` 和 `LazyHorizontalGrid` 用于显示网格布局的懒加载列表
- 支持固定列数、自适应列数、跨列项目等

**使用场景：**
- 图片网格
- 卡片网格
- 商品网格
- 仪表板布局

**代码示例：**
```kotlin
@Composable
fun CourseGrid(courses: List<CourseScheduleDto>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 固定2列
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(courses) { course ->
            CourseCard(course = course)
        }
    }
}

// 自适应列数（根据屏幕宽度）
@Composable
fun ResponsiveCourseGrid(courses: List<CourseScheduleDto>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp), // 最小200dp，自动计算列数
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(courses) { course ->
            CourseCard(course = course)
        }
    }
}
```

**GridCells 选项：**
- `GridCells.Fixed(count)`：固定列数
- `GridCells.Adaptive(minSize)`：自适应列数（根据最小尺寸）
- `GridCells.FixedSize(size)`：固定列宽

---

### 19. Icon / IconButton（图标组件）

**功能说明：**
- `Icon` 用于显示 Material Icons 或其他图标
- `IconButton` 是可点击的图标按钮
- 支持自定义图标、颜色、大小

**使用场景：**
- 工具栏图标
- 操作按钮
- 状态指示
- 导航图标

**代码示例：**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun IconExample() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 基础图标
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "首页",
            tint = MaterialTheme.colorScheme.primary
        )
        
        // 图标按钮
        IconButton(onClick = { /* 处理点击 */ }) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "收藏",
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        // 自定义大小
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "设置",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

// 在顶部栏中使用
@Composable
fun TopBarWithIcons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* 菜单 */ }) {
            Icon(Icons.Default.Menu, "菜单")
        }
        
        Text("标题")
        
        IconButton(onClick = { /* 搜索 */ }) {
            Icon(Icons.Default.Search, "搜索")
        }
    }
}
```

**常用 Material Icons：**
- `Icons.Default.Home`、`Icons.Default.Settings`、`Icons.Default.Search`
- `Icons.Default.Favorite`、`Icons.Default.Share`、`Icons.Default.Delete`
- `Icons.Default.Add`、`Icons.Default.Edit`、`Icons.Default.Close`

---

### 20. TextField / OutlinedTextField（输入框组件）

**功能说明：**
- `TextField` 和 `OutlinedTextField` 用于文本输入
- 支持占位符、标签、错误提示、密码输入等
- `OutlinedTextField` 有边框样式

**使用场景：**
- 表单输入
- 搜索框
- 密码输入
- 多行文本输入

**代码示例：**
```kotlin
@Composable
fun LoginForm() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 基础输入框
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            placeholder = { Text("请输入用户名") },
            leadingIcon = {
                Icon(Icons.Default.Person, "用户")
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            placeholder = { Text("请输入密码") },
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(Icons.Default.Lock, "密码")
            },
            trailingIcon = {
                IconButton(onClick = { /* 显示/隐藏密码 */ }) {
                    Icon(Icons.Default.Visibility, "显示密码")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 带错误提示的输入框
        var email by remember { mutableStateOf("") }
        val isError = email.isNotEmpty() && !email.contains("@")
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("邮箱") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("请输入有效的邮箱地址", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

**TextField 变体：**
- `TextField`：填充样式（无边框）
- `OutlinedTextField`：轮廓样式（有边框）
- `BasicTextField`：基础样式（无 Material 样式）

---

### 21. Switch / Checkbox / RadioButton（选择控件）

**功能说明：**
- `Switch`：开关控件（开/关两种状态）
- `Checkbox`：复选框（可多选）
- `RadioButton`：单选按钮（只能选一个）

**使用场景：**
- 设置开关
- 多选列表
- 单选列表
- 表单选项

**代码示例：**
```kotlin
@Composable
fun SelectionControls() {
    var isEnabled by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Switch 开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用通知")
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it }
            )
        }
        
        // Checkbox 复选框
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it }
            )
            Text("同意用户协议")
        }
        
        // RadioButton 单选按钮
        Column {
            Text("选择开门方式：")
            RadioButton(
                selected = selectedOption == "face",
                onClick = { selectedOption = "face" }
            )
            Text("人脸识别")
            
            RadioButton(
                selected = selectedOption == "password",
                onClick = { selectedOption = "password" }
            )
            Text("密码开门")
        }
    }
}
```

---

### 22. Dialog / AlertDialog（对话框组件）

**功能说明：**
- `Dialog` 是通用的对话框容器
- `AlertDialog` 是 Material3 的标准警告对话框
- 用于确认操作、显示重要信息

**使用场景：**
- 确认对话框
- 警告提示
- 信息展示
- 自定义弹窗

**代码示例：**
```kotlin
@Composable
fun DialogExample() {
    var showDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { showDialog = true }) {
            Text("显示对话框")
        }
        
        Button(onClick = { showAlertDialog = true }) {
            Text("显示警告对话框")
        }
    }
    
    // 自定义对话框
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("自定义对话框", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("这是对话框内容")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = false }) {
                        Text("确定")
                    }
                }
            }
        }
    }
    
    // 警告对话框
    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出系统吗？") },
            confirmButton = {
                TextButton(onClick = { showAlertDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
```

---

### 23. Scaffold（脚手架布局）

**功能说明：**
- `Scaffold` 是 Material3 的标准页面布局容器
- 提供 TopBar、BottomBar、FloatingActionButton、Drawer 等插槽
- 自动处理系统栏（状态栏、导航栏）的适配

**使用场景：**
- 标准应用页面布局
- 带导航栏的页面
- 带浮动按钮的页面

**代码示例：**
```kotlin
@Composable
fun ScaffoldExample() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("实验室管理系统") },
                actions = {
                    IconButton(onClick = { /* 搜索 */ }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, "设置") },
                    label = { Text("设置") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 添加 */ }) {
                Icon(Icons.Default.Add, "添加")
            }
        }
    ) { paddingValues ->
        // 主内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text("主内容")
        }
    }
}
```

---

### 24. FloatingActionButton（浮动操作按钮）

**功能说明：**
- `FloatingActionButton` 是 Material3 的浮动操作按钮
- 通常位于屏幕右下角
- 用于主要操作

**使用场景：**
- 添加操作
- 主要操作按钮
- 快速操作入口

**代码示例：**
```kotlin
@Composable
fun FloatingActionButtonExample() {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* 添加课程 */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        // 主内容
    }
}

// 扩展的 FAB（带标签）
@Composable
fun ExtendedFABExample() {
    ExtendedFloatingActionButton(
        onClick = { /* 操作 */ },
        icon = { Icon(Icons.Default.Add, "添加") },
        text = { Text("添加课程") }
    )
}
```

---

### 25. Chip / FilterChip / AssistChip（芯片组件）

**功能说明：**
- `Chip` 系列组件用于显示紧凑的标签或选择项
- `FilterChip`：可过滤的芯片（可选中）
- `AssistChip`：辅助芯片（带操作）
- `SuggestionChip`：建议芯片

**使用场景：**
- 标签显示
- 过滤器
- 快速选择
- 分类标签

**代码示例：**
```kotlin
@Composable
fun ChipExample() {
    var selectedChip by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // FilterChip - 可过滤的芯片
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedChip == "all",
                onClick = { selectedChip = "all" },
                label = { Text("全部") }
            )
            FilterChip(
                selected = selectedChip == "today",
                onClick = { selectedChip = "today" },
                label = { Text("今天") }
            )
            FilterChip(
                selected = selectedChip == "week",
                onClick = { selectedChip = "week" },
                label = { Text("本周") }
            )
        }
        
        // AssistChip - 辅助芯片（带图标）
        AssistChip(
            onClick = { /* 操作 */ },
            label = { Text("人脸识别开门") },
            leadingIcon = {
                Icon(Icons.Default.Face, "人脸")
            }
        )
        
        // SuggestionChip - 建议芯片
        SuggestionChip(
            onClick = { },
            label = { Text("推荐课程") }
        )
    }
}
```

---

### 26. ProgressIndicator（进度指示器）

**功能说明：**
- `CircularProgressIndicator`：圆形进度指示器
- `LinearProgressIndicator`：线性进度指示器
- 用于显示加载状态或进度

**使用场景：**
- 加载状态
- 进度显示
- 等待提示

**代码示例：**
```kotlin
@Composable
fun ProgressIndicatorExample() {
    var progress by remember { mutableStateOf(0.3f) }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 圆形进度指示器（不确定进度）
        CircularProgressIndicator()
        
        // 圆形进度指示器（确定进度）
        CircularProgressIndicator(progress = progress)
        
        // 线性进度指示器（不确定进度）
        LinearProgressIndicator()
        
        // 线性进度指示器（确定进度）
        LinearProgressIndicator(progress = progress)
        
        // 自定义颜色
        CircularProgressIndicator(
            progress = progress,
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        
        // 按钮控制进度
        Button(onClick = { progress = (progress + 0.1f).coerceAtMost(1f) }) {
            Text("增加进度")
        }
    }
}
```

---

### 27. Snackbar（提示消息）

**功能说明：**
- `Snackbar` 用于显示简短的消息提示
- 通常出现在屏幕底部
- 自动消失或可手动关闭

**使用场景：**
- 操作反馈
- 错误提示
- 成功提示
- 简短通知

**代码示例：**
```kotlin
@Composable
fun SnackbarExample() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "操作成功",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            ) {
                Text("显示成功提示")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "操作失败",
                            duration = SnackbarDuration.Long,
                            actionLabel = "重试"
                        ) { /* 重试操作 */ }
                    }
                }
            ) {
                Text("显示错误提示（带操作）")
            }
        }
    }
}
```

---

### 28. DropdownMenu（下拉菜单）

**功能说明：**
- `DropdownMenu` 用于显示下拉菜单
- 通常与按钮或图标配合使用
- 支持多级菜单

**使用场景：**
- 操作菜单
- 选项选择
- 上下文菜单
- 更多操作

**代码示例：**
```kotlin
@Composable
fun DropdownMenuExample() {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("选项1") }
    
    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedOption)
            Icon(Icons.Default.ArrowDropDown, "下拉")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("选项1") },
                onClick = {
                    selectedOption = "选项1"
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("选项2") },
                onClick = {
                    selectedOption = "选项2"
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("选项3") },
                onClick = {
                    selectedOption = "选项3"
                    expanded = false
                }
            )
        }
    }
}

// 带图标的菜单项
@Composable
fun DropdownMenuWithIcons() {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, "更多")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, "编辑") },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Default.Delete, "删除") },
                onClick = { expanded = false }
            )
        }
    }
}
```

---

### 29. TabRow / Tab（标签页组件）

**功能说明：**
- `TabRow` 和 `Tab` 用于创建标签页界面
- 支持滚动标签、固定标签等模式
- 通常与 `HorizontalPager` 配合使用

**使用场景：**
- 分类切换
- 页面切换
- 内容分组

**代码示例：**
```kotlin
@Composable
fun TabRowExample() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("周一", "周二", "周三", "周四", "周五")
    
    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        // 根据选中的标签显示内容
        when (selectedTabIndex) {
            0 -> Text("周一课程")
            1 -> Text("周二课程")
            2 -> Text("周三课程")
            3 -> Text("周四课程")
            4 -> Text("周五课程")
        }
    }
}

// 带图标的标签
@Composable
fun TabRowWithIcons() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    TabRow(selectedTabIndex = selectedTabIndex) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { selectedTabIndex = 0 },
            icon = { Icon(Icons.Default.Home, "首页") },
            text = { Text("首页") }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { selectedTabIndex = 1 },
            icon = { Icon(Icons.Default.Settings, "设置") },
            text = { Text("设置") }
        )
    }
}
```

---

### 30. Slider（滑块组件）

**功能说明：**
- `Slider` 用于在范围内选择数值
- 支持单值滑块和范围滑块
- 可自定义颜色、步长等

**使用场景：**
- 音量控制
- 亮度调节
- 范围选择
- 数值输入

**代码示例：**
```kotlin
@Composable
fun SliderExample() {
    var sliderValue by remember { mutableStateOf(0f) }
    var volumeValue by remember { mutableStateOf(50f) }
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 基础滑块
        Text("值: ${sliderValue.toInt()}")
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 0f..100f
        )
        
        // 音量滑块（带图标）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VolumeDown, "音量")
            Slider(
                value = volumeValue,
                onValueChange = { volumeValue = it },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.VolumeUp, "音量")
        }
        
        // 带步长的滑块
        var stepValue by remember { mutableStateOf(0f) }
        Text("步长值: ${stepValue.toInt()}")
        Slider(
            value = stepValue,
            onValueChange = { stepValue = it },
            valueRange = 0f..100f,
            steps = 9 // 10个值（0, 10, 20, ..., 100）
        )
    }
}
```

---

### 31. Badge（徽章组件）

**功能说明：**
- `Badge` 用于显示数字徽章或状态指示
- 通常叠加在其他组件上（如图标、按钮）
- 支持自定义颜色、大小

**使用场景：**
- 未读消息数
- 通知数量
- 状态指示
- 新内容标记

**代码示例：**
```kotlin
@Composable
fun BadgeExample() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 图标上的徽章
        BadgedBox(
            badge = {
                Badge {
                    Text("5")
                }
            }
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, "通知")
            }
        }
        
        // 按钮上的徽章
        BadgedBox(
            badge = {
                Badge {
                    Text("99+")
                }
            }
        ) {
            Button(onClick = { }) {
                Text("消息")
            }
        }
        
        // 自定义颜色的徽章
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text("3", color = Color.White)
                }
            }
        ) {
            Icon(Icons.Default.Mail, "邮件")
        }
    }
}
```

---

## 组件使用总结

### 已使用的组件

**布局组件：**
- ✅ Column、Row、Box、BoxWithConstraints、Spacer

**Material3 组件：**
- ✅ Card、Button、Surface、Divider、Text

**图像组件：**
- ✅ Image

**修饰符：**
- ✅ Modifier（各种修饰符）、verticalScroll、MaterialTheme

### 未使用但推荐的组件

**列表和网格：**
- ⭐ LazyColumn / LazyRow（长列表）
- ⭐ LazyVerticalGrid / LazyHorizontalGrid（网格布局）

**交互组件：**
- ⭐ Icon / IconButton（图标）
- ⭐ TextField / OutlinedTextField（输入框）
- ⭐ Switch / Checkbox / RadioButton（选择控件）
- ⭐ Slider（滑块）

**导航和布局：**
- ⭐ Scaffold（脚手架布局）
- ⭐ TabRow / Tab（标签页）
- ⭐ NavigationBar（导航栏）

**反馈组件：**
- ⭐ Dialog / AlertDialog（对话框）
- ⭐ Snackbar（提示消息）
- ⭐ ProgressIndicator（进度指示器）

**其他组件：**
- ⭐ FloatingActionButton（浮动按钮）
- ⭐ Chip / FilterChip（芯片）
- ⭐ DropdownMenu（下拉菜单）
- ⭐ Badge（徽章）

---

## 组件选择指南

### 何时使用 LazyColumn vs Column
- **Column**：少量固定项目（< 50），不需要滚动
- **LazyColumn**：大量项目或需要滚动，性能更好

### 何时使用 Card vs Surface
- **Card**：需要默认内边距、阴影、交互效果的信息面板
- **Surface**：简单的容器、标签、浮动元素

### 何时使用 TextField vs OutlinedTextField
- **TextField**：填充样式，适合对话框、表单内部
- **OutlinedTextField**：轮廓样式，适合独立表单、搜索框

### 何时使用 Dialog vs AlertDialog
- **Dialog**：自定义样式的弹窗
- **AlertDialog**：标准的确认/警告对话框

---

## 参考资料

- [Jetpack Compose 官方文档](https://developer.android.com/jetpack/compose)
- [Material3 组件文档](https://m3.material.io/components)
- [Compose 组件 API 参考](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)