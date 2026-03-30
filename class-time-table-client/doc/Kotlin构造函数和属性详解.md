# Kotlin 构造函数和属性详解

本文档详细解释 Kotlin 的构造函数、成员变量声明以及 getter/setter 的自动生成机制。

## 目录

1. [主构造函数](#主构造函数)
2. [次构造函数](#次构造函数)
3. [成员变量声明](#成员变量声明)
4. [Getter/Setter 自动生成](#gettersetter-自动生成)
5. [属性访问器自定义](#属性访问器自定义)
6. [实际代码示例分析](#实际代码示例分析)

---

## 主构造函数

### 基本语法

Kotlin 的主构造函数（Primary Constructor）写在类名后面，使用 `()` 包裹参数。

**语法格式：**
```kotlin
class ClassName(参数1: 类型1, 参数2: 类型2) {
    // 类体
}
```

### 主构造函数的三种写法

#### 1. 仅参数（不声明为属性）

```kotlin
class Person(name: String, age: Int) {
    // name 和 age 只是构造函数参数，不是类的属性
    // 在类体内无法直接访问
}
```

**等价于 Java：**
```java
public class Person {
    public Person(String name, int age) {
        // name 和 age 只是局部变量
    }
}
```

#### 2. 声明为属性（使用 `val` 或 `var`）

```kotlin
class Person(val name: String, var age: Int) {
    // name 是只读属性（val），自动生成 getter
    // age 是可读写属性（var），自动生成 getter 和 setter
}
```

**等价于 Java：**
```java
public class Person {
    private final String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
}
```

#### 3. 使用可见性修饰符

```kotlin
class Person(
    val name: String,           // public getter
    private var age: Int,       // private getter 和 setter
    internal val id: Long       // internal getter
) {
    // ...
}
```

---

## 次构造函数

### 基本语法

次构造函数（Secondary Constructor）写在类体内，使用 `constructor` 关键字。

**语法格式：**
```kotlin
class Person(val name: String) {
    constructor(name: String, age: Int) : this(name) {
        // 必须调用主构造函数或其他次构造函数
        // 使用 : this(...) 调用主构造函数
    }
}
```

### 次构造函数规则

1. **必须调用主构造函数**：使用 `: this(...)` 调用主构造函数
2. **可以调用其他次构造函数**：使用 `: this(...)` 调用其他次构造函数
3. **不能声明属性**：次构造函数参数不能使用 `val` 或 `var`

**示例：**
```kotlin
class Person(val name: String, val age: Int) {
    // 次构造函数 1：只提供 name，age 默认为 0
    constructor(name: String) : this(name, 0)
    
    // 次构造函数 2：从 Map 创建
    constructor(map: Map<String, Any>) : this(
        map["name"] as String,
        map["age"] as Int
    )
}
```

**等价于 Java：**
```java
public class Person {
    private final String name;
    private final int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public Person(String name) {
        this(name, 0);
    }
    
    public Person(Map<String, Object> map) {
        this(
            (String) map.get("name"),
            (Integer) map.get("age")
        );
    }
}
```

---

## 成员变量声明

### 在主构造函数中声明

**方式 1：使用 `val`（只读属性）**
```kotlin
class Person(val name: String) {
    // name 是只读属性
    // 自动生成：public String getName()
}
```

**方式 2：使用 `var`（可读写属性）**
```kotlin
class Person(var name: String) {
    // name 是可读写属性
    // 自动生成：public String getName() 和 public void setName(String)
}
```

**方式 3：使用可见性修饰符**
```kotlin
class Person(
    val name: String,              // public getter
    private var age: Int,          // private getter 和 setter
    protected val id: Long,        // protected getter
    internal var email: String     // internal getter 和 setter
)
```

### 在类体中声明

```kotlin
class Person {
    val name: String = "Unknown"   // 只读属性，带初始值
    var age: Int = 0               // 可读写属性，带初始值
    
    // 延迟初始化
    lateinit var address: String
    
    // 可空属性
    var phone: String? = null
}
```

---

## Getter/Setter 自动生成

### 自动生成规则

Kotlin 会根据属性的声明方式自动生成对应的访问器：

| 声明方式 | Getter | Setter | 说明 |
|---------|--------|--------|------|
| `val name: String` | ✅ 自动生成 | ❌ 不生成 | 只读属性 |
| `var name: String` | ✅ 自动生成 | ✅ 自动生成 | 可读写属性 |
| `private val name: String` | ✅ 私有 | ❌ 不生成 | 私有只读 |
| `private var name: String` | ✅ 私有 | ✅ 私有 | 私有可读写 |

### 自动生成的代码示例

**Kotlin 代码：**
```kotlin
class Person(
    val name: String,      // 只读
    var age: Int          // 可读写
)
```

**Kotlin 编译器实际生成的代码（简化版）：**
```kotlin
class Person {
    private val _name: String
    private var _age: Int
    
    constructor(name: String, age: Int) {
        this._name = name
        this._age = age
    }
    
    // 自动生成的 getter
    fun getName(): String {
        return _name
    }
    
    // 自动生成的 getter
    fun getAge(): Int {
        return _age
    }
    
    // 自动生成的 setter
    fun setAge(value: Int) {
        _age = value
    }
}
```

**Java 等价代码：**
```java
public class Person {
    private final String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
}
```

### 幕后字段（Backing Field）

当你需要自定义 getter/setter 时，Kotlin 提供 `field` 关键字来访问幕后字段。

```kotlin
class Person {
    var name: String = ""
        get() = field.uppercase()  // field 代表幕后字段
        set(value) {
            if (value.isNotEmpty()) {
                field = value
            }
        }
}
```

**说明：**
- `field` 是 Kotlin 提供的特殊标识符
- 只能在属性的 getter/setter 中使用
- 代表属性的实际存储值

---

## 属性访问器自定义

### 自定义 Getter

```kotlin
class Rectangle(val width: Int, val height: Int) {
    // 计算属性：没有幕后字段，只有 getter
    val area: Int
        get() = width * height
    
    // 自定义 getter，带幕后字段
    var description: String = ""
        get() = "Rectangle: ${width}x${height}, Area: $area"
}
```

### 自定义 Setter

```kotlin
class Person {
    var age: Int = 0
        set(value) {
            if (value >= 0 && value <= 150) {
                field = value  // 使用 field 设置值
            } else {
                throw IllegalArgumentException("Invalid age: $value")
            }
        }
    
    var name: String = ""
        set(value) {
            field = value.trim().uppercase()
        }
}
```

### Getter/Setter 可见性

```kotlin
class Person {
    var name: String = ""
        private set  // setter 是私有的，只能在类内修改
    
    var age: Int = 0
        internal get  // getter 是 internal 的
        private set   // setter 是私有的
}
```

---

## 实际代码示例分析

### 你的代码分析

让我们分析你选中的代码：

```kotlin
abstract class DoorOpeningUIProvider(
    private var title : String,
    private var description : String) {
    
    @Composable
    abstract fun BuildContent(
        type: DoorOpeningType,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    )
}
```

### 详细解释

#### 1. 主构造函数参数

```kotlin
private var title : String
private var description : String
```

**含义：**
- `private`：这两个属性是私有的
- `var`：可读写属性，自动生成 getter 和 setter
- 但由于是 `private`，所以 getter 和 setter 也是私有的

**等价于 Java：**
```java
public abstract class DoorOpeningUIProvider {
    private String title;
    private String description;
    
    public DoorOpeningUIProvider(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    // 私有 getter（实际上在 Java 中不会生成）
    private String getTitle() {
        return title;
    }
    
    // 私有 setter
    private void setTitle(String title) {
        this.title = title;
    }
    
    // description 同理
}
```

#### 2. 问题：无法从外部访问

由于 `title` 和 `description` 是 `private` 的，外部无法访问。如果你需要在子类或外部访问，应该改为：

**方案 1：改为 `protected`（推荐）**
```kotlin
abstract class DoorOpeningUIProvider(
    protected var title: String,        // 子类可访问
    protected var description: String    // 子类可访问
) {
    // ...
}
```

**方案 2：改为 `val`（如果不需要修改）**
```kotlin
abstract class DoorOpeningUIProvider(
    val title: String,        // public getter
    val description: String   // public getter
) {
    // ...
}
```

**方案 3：提供公共访问方法**
```kotlin
abstract class DoorOpeningUIProvider(
    private var title: String,
    private var description: String
) {
    // 提供公共访问方法
    fun getTitle(): String = title
    fun getDescription(): String = description
    
    fun setTitle(title: String) {
        this.title = title
    }
    
    fun setDescription(description: String) {
        this.description = description
    }
}
```

### 修复建议

根据你的代码第 201 行 `uiProvider.get`，看起来你想访问 title。建议修改为：

```kotlin
abstract class DoorOpeningUIProvider(
    val title: String,           // 改为 val，public getter
    val description: String      // 改为 val，public getter
) {
    @Composable
    abstract fun BuildContent(
        type: DoorOpeningType,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    )
}
```

然后在使用时：
```kotlin
Text(
    text = uiProvider.title,  // 直接访问属性
    // ...
)
```

---

## 完整示例对比

### Java 版本

```java
public abstract class DoorOpeningUIProvider {
    private String title;
    private String description;
    
    public DoorOpeningUIProvider(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public abstract void buildContent(
        DoorOpeningType type,
        Runnable onConfirm,
        Runnable onCancel
    );
}
```

### Kotlin 版本（自动生成 getter/setter）

```kotlin
abstract class DoorOpeningUIProvider(
    var title: String,           // 自动生成 getTitle() 和 setTitle()
    var description: String      // 自动生成 getDescription() 和 setDescription()
) {
    @Composable
    abstract fun BuildContent(
        type: DoorOpeningType,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    )
}
```

### Kotlin 版本（只读属性）

```kotlin
abstract class DoorOpeningUIProvider(
    val title: String,           // 只生成 getTitle()
    val description: String      // 只生成 getDescription()
) {
    @Composable
    abstract fun BuildContent(
        type: DoorOpeningType,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    )
}
```

---

## 总结

### 关键要点

1. **主构造函数**：写在类名后面，可以声明属性
2. **`val` vs `var`**：
   - `val`：只读属性，只生成 getter
   - `var`：可读写属性，生成 getter 和 setter
3. **可见性修饰符**：影响 getter/setter 的可见性
4. **自动生成**：Kotlin 自动生成访问器，无需手动编写
5. **幕后字段**：使用 `field` 访问属性的实际存储值

### 最佳实践

1. **优先使用 `val`**：除非确实需要修改，否则使用只读属性
2. **合理使用可见性**：根据需求选择合适的可见性修饰符
3. **利用自动生成**：让 Kotlin 自动生成 getter/setter，减少样板代码
4. **需要验证时自定义**：只有在需要验证或转换时才自定义 setter

---

**文档版本：** 1.0  
**最后更新：** 2024年



