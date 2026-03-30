# Java 到 Kotlin 语法转换指南

本文档提供从 Java 到 Kotlin 的语法精简转换指南，涵盖继承、多态、泛型、构造函数、接口实现等核心概念。

## 目录

1. [基础语法转换](#基础语法转换)
2. [类与对象](#类与对象)
3. [继承](#继承)
4. [接口实现](#接口实现)
5. [多态](#多态)
6. [泛型](#泛型)
7. [构造函数](#构造函数)
8. [属性与访问器](#属性与访问器)
9. [函数与方法](#函数与方法)
10. [空安全](#空安全)
11. [扩展函数](#扩展函数)
12. [数据类](#数据类)
13. [对象与单例](#对象与单例)
14. [集合操作](#集合操作)
15. [Lambda 表达式](#lambda-表达式)

---

## 基础语法转换

### 变量声明

**Java:**
```java
String name = "Kotlin";
final String constant = "Immutable";
int count = 10;
```

**Kotlin:**
```kotlin
var name: String = "Kotlin"
val constant: String = "Immutable"  // val 相当于 final
var count: Int = 10

// 类型推断
var name = "Kotlin"  // 自动推断为 String
val count = 10       // 自动推断为 Int
```

### 空值处理

**Java:**
```java
String nullable = null;
String nonNull = "Hello";
```

**Kotlin:**
```kotlin
var nullable: String? = null  // 可空类型
var nonNull: String = "Hello"  // 非空类型
```

### 字符串模板

**Java:**
```java
String message = "Hello, " + name + "!";
```

**Kotlin:**
```kotlin
val message = "Hello, $name!"
val message2 = "Sum: ${a + b}"
```

---

## 类与对象

### 类声明

**Java:**
```java
public class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
```

**Kotlin:**
```kotlin
class Person(
    var name: String,
    var age: Int
)

// 或者使用完整形式
class Person {
    var name: String
    var age: Int
    
    constructor(name: String, age: Int) {
        this.name = name
        this.age = age
    }
}
```

### 可见性修饰符

**Java:**
```java
public class PublicClass { }
private class PrivateClass { }
protected class ProtectedClass { }  // 仅内部类
```

**Kotlin:**
```kotlin
class PublicClass        // 默认 public
public class PublicClass  // 显式 public
private class PrivateClass
protected class ProtectedClass  // 仅内部类
internal class InternalClass    // 模块内可见
```

---

## 继承

### 类继承

**Java:**
```java
public class Animal {
    protected String name;
    
    public Animal(String name) {
        this.name = name;
    }
    
    public void makeSound() {
        System.out.println("Some sound");
    }
}

public class Dog extends Animal {
    public Dog(String name) {
        super(name);
    }
    
    @Override
    public void makeSound() {
        System.out.println("Woof!");
    }
}
```

**Kotlin:**
```kotlin
open class Animal(val name: String) {  // open 关键字允许继承
    open fun makeSound() {              // open 关键字允许重写
        println("Some sound")
    }
}

class Dog(name: String) : Animal(name) {  // 使用 : 继承
    override fun makeSound() {             // override 关键字
        println("Woof!")
    }
}
```

### 抽象类

**Java:**
```java
public abstract class Shape {
    protected String color;
    
    public Shape(String color) {
        this.color = color;
    }
    
    public abstract double area();
    
    public void display() {
        System.out.println("Color: " + color);
    }
}

public class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}
```

**Kotlin:**
```kotlin
abstract class Shape(val color: String) {
    abstract fun area(): Double
    
    fun display() {
        println("Color: $color")
    }
}

class Circle(color: String, private val radius: Double) : Shape(color) {
    override fun area(): Double {
        return Math.PI * radius * radius
    }
}
```

### 继承规则

**Kotlin 继承特点：**
- 所有类默认 `final`，需要 `open` 才能被继承
- 所有方法默认 `final`，需要 `open` 才能被重写
- 使用 `:` 代替 `extends` 和 `implements`
- 必须调用父类构造函数

---

## 接口实现

### 接口定义

**Java:**
```java
public interface Drawable {
    void draw();
    
    default void display() {
        System.out.println("Displaying...");
    }
}

public interface Clickable {
    void onClick();
}
```

**Kotlin:**
```kotlin
interface Drawable {
    fun draw()
    
    fun display() {  // 默认实现
        println("Displaying...")
    }
}

interface Clickable {
    fun onClick()
}
```

### 实现接口

**Java:**
```java
public class Button implements Drawable, Clickable {
    @Override
    public void draw() {
        System.out.println("Drawing button");
    }
    
    @Override
    public void onClick() {
        System.out.println("Button clicked");
    }
}
```

**Kotlin:**
```kotlin
class Button : Drawable, Clickable {
    override fun draw() {
        println("Drawing button")
    }
    
    override fun onClick() {
        println("Button clicked")
    }
}
```

### 接口属性

**Java:**
```java
public interface Config {
    String getApiUrl();
    void setApiUrl(String url);
}
```

**Kotlin:**
```kotlin
interface Config {
    val apiUrl: String  // 抽象属性
    var timeout: Int   // 抽象可变属性
}

// 实现
class AppConfig : Config {
    override val apiUrl: String = "https://api.example.com"
    override var timeout: Int = 5000
}
```

---

## 多态

### 多态示例

**Java:**
```java
public class Animal {
    public void makeSound() {
        System.out.println("Animal sound");
    }
}

public class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Woof!");
    }
}

public class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow!");
    }
}

// 使用
Animal animal1 = new Dog();
Animal animal2 = new Cat();
animal1.makeSound();  // Woof!
animal2.makeSound();  // Meow!
```

**Kotlin:**
```kotlin
open class Animal {
    open fun makeSound() {
        println("Animal sound")
    }
}

class Dog : Animal() {
    override fun makeSound() {
        println("Woof!")
    }
}

class Cat : Animal() {
    override fun makeSound() {
        println("Meow!")
    }
}

// 使用
val animal1: Animal = Dog()
val animal2: Animal = Cat()
animal1.makeSound()  // Woof!
animal2.makeSound()  // Meow!
```

### 类型检查与转换

**Java:**
```java
if (animal instanceof Dog) {
    Dog dog = (Dog) animal;
    dog.bark();
}
```

**Kotlin:**
```kotlin
if (animal is Dog) {
    animal.bark()  // 智能转换，无需显式转换
}

// 或者使用 as
val dog = animal as? Dog  // 安全转换，失败返回 null
dog?.bark()
```

---

## 泛型

### 泛型类

**Java:**
```java
public class Box<T> {
    private T item;
    
    public Box(T item) {
        this.item = item;
    }
    
    public T getItem() {
        return item;
    }
    
    public void setItem(T item) {
        this.item = item;
    }
}

// 使用
Box<String> stringBox = new Box<>("Hello");
Box<Integer> intBox = new Box<>(42);
```

**Kotlin:**
```kotlin
class Box<T>(var item: T)

// 使用
val stringBox: Box<String> = Box("Hello")
val intBox: Box<Int> = Box(42)

// 类型推断
val stringBox = Box("Hello")  // 自动推断为 Box<String>
```

### 泛型约束

**Java:**
```java
public class NumberBox<T extends Number> {
    private T number;
    
    public NumberBox(T number) {
        this.number = number;
    }
    
    public double getDoubleValue() {
        return number.doubleValue();
    }
}
```

**Kotlin:**
```kotlin
class NumberBox<T : Number>(var number: T) {
    fun getDoubleValue(): Double {
        return number.toDouble()
    }
}

// 多个约束
class Processor<T> where T : CharSequence, T : Appendable {
    fun process(item: T) {
        // ...
    }
}
```

### 泛型函数

**Java:**
```java
public static <T> T getFirst(List<T> list) {
    return list.get(0);
}

public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;
}
```

**Kotlin:**
```kotlin
fun <T> getFirst(list: List<T>): T {
    return list[0]
}

fun <T : Comparable<T>> max(a: T, b: T): T {
    return if (a > b) a else b
}
```

### 通配符与变型

**Java:**
```java
// 上界通配符
List<? extends Number> numbers;

// 下界通配符
List<? super Integer> integers;

// 无界通配符
List<?> objects;
```

**Kotlin:**
```kotlin
// 协变（out - 只读）
List<out Number> numbers

// 逆变（in - 只写）
List<in Int> integers

// 星投影
List<*> objects

// 实际使用
fun processNumbers(numbers: List<out Number>) { }
fun addNumbers(numbers: MutableList<in Int>) { }
```

---

## 构造函数

### 主构造函数

**Java:**
```java
public class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

**Kotlin:**
```kotlin
class Person(val name: String, val age: Int)

// 等价于
class Person(name: String, age: Int) {
    val name: String = name
    val age: Int = age
}
```

### 次构造函数

**Java:**
```java
public class Person {
    private String name;
    private int age;
    
    public Person(String name) {
        this(name, 0);
    }
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

**Kotlin:**
```kotlin
class Person(val name: String, val age: Int) {
    constructor(name: String) : this(name, 0)
}

// 或者使用默认参数
class Person(val name: String, val age: Int = 0)
```

### 初始化块

**Java:**
```java
public class Person {
    private String name;
    
    {
        // 初始化块
        System.out.println("Initializing...");
    }
    
    public Person(String name) {
        this.name = name;
    }
}
```

**Kotlin:**
```kotlin
class Person(val name: String) {
    init {
        // 初始化块
        println("Initializing...")
    }
}
```

---

## 属性与访问器

### Getter/Setter

**Java:**
```java
public class Person {
    private String name;
    private int age;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        if (age >= 0) {
            this.age = age;
        }
    }
}
```

**Kotlin:**
```kotlin
class Person {
    var name: String = ""
        set(value) {
            if (value.isNotEmpty()) {
                field = value  // field 是幕后字段
            }
        }
        get() = field
    
    var age: Int = 0
        set(value) {
            if (value >= 0) {
                field = value
            }
        }
}
```

### 只读属性

**Java:**
```java
public class Circle {
    private final double radius;
    private double area;
    
    public Circle(double radius) {
        this.radius = radius;
        this.area = Math.PI * radius * radius;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public double getArea() {
        return area;
    }
}
```

**Kotlin:**
```kotlin
class Circle(val radius: Double) {
    val area: Double = Math.PI * radius * radius
}

// 或者使用自定义 getter
class Circle(val radius: Double) {
    val area: Double
        get() = Math.PI * radius * radius
}
```

---

## 函数与方法

### 函数声明

**Java:**
```java
public int add(int a, int b) {
    return a + b;
}

public void printMessage(String message) {
    System.out.println(message);
}
```

**Kotlin:**
```kotlin
fun add(a: Int, b: Int): Int {
    return a + b
}

// 单表达式函数
fun add(a: Int, b: Int) = a + b

fun printMessage(message: String) {
    println(message)
}

// Unit 可以省略
fun printMessage(message: String): Unit {
    println(message)
}
```

### 默认参数

**Java:**
```java
public void greet(String name, String greeting) {
    System.out.println(greeting + ", " + name);
}

// 需要重载
public void greet(String name) {
    greet(name, "Hello");
}
```

**Kotlin:**
```kotlin
fun greet(name: String, greeting: String = "Hello") {
    println("$greeting, $name")
}

// 使用
greet("Alice")              // Hello, Alice
greet("Bob", "Hi")          // Hi, Bob
greet(name = "Charlie")     // 命名参数
```

### 可变参数

**Java:**
```java
public int sum(int... numbers) {
    int total = 0;
    for (int num : numbers) {
        total += num;
    }
    return total;
}
```

**Kotlin:**
```kotlin
fun sum(vararg numbers: Int): Int {
    var total = 0
    for (num in numbers) {
        total += num
    }
    return total
}

// 或者
fun sum(vararg numbers: Int) = numbers.sum()
```

---

## 空安全

### 可空类型

**Java:**
```java
String name = null;  // 可能 NPE
if (name != null) {
    System.out.println(name.length());
}
```

**Kotlin:**
```kotlin
var name: String? = null  // 可空类型
println(name?.length)     // 安全调用，返回 null 或 Int
println(name?.length ?: 0)  // Elvis 操作符
```

### 安全调用链

**Java:**
```java
if (person != null && person.address != null) {
    String city = person.address.city;
}
```

**Kotlin:**
```kotlin
val city = person?.address?.city
```

### 非空断言

**Java:**
```java
String name = getName();  // 假设不为 null
int length = name.length();
```

**Kotlin:**
```kotlin
val name = getName()!!
val length = name.length
```

---

## 扩展函数

**Java:**
```java
public class StringUtils {
    public static boolean isEmail(String str) {
        return str.contains("@");
    }
}

// 使用
StringUtils.isEmail("test@example.com");
```

**Kotlin:**
```kotlin
fun String.isEmail(): Boolean {
    return this.contains("@")
}

// 使用
"test@example.com".isEmail()
```

### 扩展属性

**Kotlin:**
```kotlin
val String.lastChar: Char
    get() = this[length - 1]

// 使用
val last = "Hello".lastChar  // 'o'
```

---

## 数据类

**Java:**
```java
public class Person {
    private String name;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && 
               Objects.equals(name, person.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}
```

**Kotlin:**
```kotlin
data class Person(val name: String, val age: Int)

// 自动生成：
// - equals()
// - hashCode()
// - toString()
// - copy()
// - componentN() (解构)
```

### 解构声明

**Kotlin:**
```kotlin
val person = Person("Alice", 30)
val (name, age) = person  // 解构
println(name)  // Alice
println(age)   // 30
```

---

## 对象与单例

### 单例模式

**Java:**
```java
public class Singleton {
    private static Singleton instance;
    
    private Singleton() {}
    
    public static synchronized Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

**Kotlin:**
```kotlin
object Singleton {
    fun doSomething() {
        // ...
    }
}

// 使用
Singleton.doSomething()
```

### 伴生对象

**Java:**
```java
public class MyClass {
    private static int count = 0;
    
    public static int getCount() {
        return count;
    }
}
```

**Kotlin:**
```kotlin
class MyClass {
    companion object {
        private var count = 0
        
        fun getCount(): Int = count
    }
}

// 使用
MyClass.getCount()
```

---

## 集合操作

### 集合创建

**Java:**
```java
List<String> list = new ArrayList<>();
list.add("A");
list.add("B");

Map<String, Integer> map = new HashMap<>();
map.put("one", 1);
map.put("two", 2);
```

**Kotlin:**
```kotlin
val list = mutableListOf("A", "B")
val immutableList = listOf("A", "B")

val map = mutableMapOf("one" to 1, "two" to 2)
val immutableMap = mapOf("one" to 1, "two" to 2)
```

### 集合操作

**Java:**
```java
List<String> filtered = new ArrayList<>();
for (String s : list) {
    if (s.startsWith("A")) {
        filtered.add(s);
    }
}
```

**Kotlin:**
```kotlin
val filtered = list.filter { it.startsWith("A") }
val mapped = list.map { it.length }
val sorted = list.sortedBy { it.length }
```

---

## Lambda 表达式

### Lambda 语法

**Java:**
```java
// Java 8+
list.forEach(item -> System.out.println(item));

// 方法引用
list.forEach(System.out::println);

// 匿名内部类
button.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        // ...
    }
});
```

**Kotlin:**
```kotlin
list.forEach { item -> println(item) }
list.forEach { println(it) }  // it 是单个参数的隐式名称

// 方法引用
list.forEach(::println)

// Lambda 作为参数
button.setOnClickListener { view ->
    // ...
}
```

### 高阶函数

**Java:**
```java
public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
    List<T> result = new ArrayList<>();
    for (T item : list) {
        if (predicate.test(item)) {
            result.add(item);
        }
    }
    return result;
}
```

**Kotlin:**
```kotlin
fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T> {
    return list.filter(predicate)
}

// 使用
val numbers = listOf(1, 2, 3, 4, 5)
val evens = filter(numbers) { it % 2 == 0 }
```

---

## 总结对比表

| 特性 | Java | Kotlin |
|------|------|--------|
| 变量声明 | `String name = "value"` | `var name: String = "value"` |
| 常量 | `final String name = "value"` | `val name: String = "value"` |
| 空安全 | 无内置支持 | `String?` 可空类型 |
| 继承 | `extends` | `:` |
| 接口实现 | `implements` | `:` |
| 类继承性 | 默认可继承 | 默认 `final`，需 `open` |
| 方法重写 | `@Override` | `override` |
| 构造函数 | 构造函数方法 | 主构造函数 + 次构造函数 |
| Getter/Setter | 手动编写 | 自动生成或自定义 |
| 数据类 | 手动实现 | `data class` |
| 单例 | 手动实现 | `object` |
| 扩展函数 | 工具类 | 扩展函数 |
| Lambda | Java 8+ | 一等公民 |
| 字符串模板 | 拼接 | `$variable` 或 `${expression}` |
| 类型检查 | `instanceof` | `is` |
| 类型转换 | `(Type) obj` | `obj as Type` |

---

## 最佳实践

### 1. 优先使用 `val` 而非 `var`
```kotlin
// ✅ 推荐
val name = "Kotlin"

// ❌ 避免
var name = "Kotlin"  // 除非确实需要可变
```

### 2. 使用数据类替代 POJO
```kotlin
// ✅ 推荐
data class User(val id: Int, val name: String)

// ❌ 避免
class User {
    var id: Int = 0
    var name: String = ""
    // ... equals, hashCode, toString
}
```

### 3. 使用空安全操作符
```kotlin
// ✅ 推荐
val length = name?.length ?: 0

// ❌ 避免
val length = if (name != null) name.length else 0
```

### 4. 使用扩展函数增强可读性
```kotlin
// ✅ 推荐
fun String.isValidEmail() = contains("@") && contains(".")

// ❌ 避免
fun isValidEmail(email: String) = email.contains("@") && email.contains(".")
```

### 5. 利用默认参数减少重载
```kotlin
// ✅ 推荐
fun greet(name: String, greeting: String = "Hello") { }

// ❌ 避免
fun greet(name: String) { }
fun greet(name: String, greeting: String) { }
```

---

## 参考资料

- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Kotlin for Java Developers](https://kotlinlang.org/docs/java-to-kotlin-idioms.html)
- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)

---

**文档版本：** 1.0  
**最后更新：** 2024年



