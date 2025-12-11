package com.example

import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * 大型并发测试类
 * <p>
 * 这个类包含了大量的成员，用于测试 Code Vision 和并发处理的性能。
 * 包含多个嵌套类、匿名类、接口、枚举等复杂的代码结构。
 */
class LargeConcurrentTestClass {

    // ========== 字段 ==========
    private var name: String = ""
    private var id: Int = 0
    private var value: Double = 0.0
    private var active: Boolean = false
    private var items: MutableList<String> = mutableListOf()
    private var metadata: MutableMap<String, Any> = mutableMapOf()
    private var numbers: MutableSet<Int> = mutableSetOf()
    private var optionalValue: Optional<String>? = null
    private var futureResult: Future<String>? = null
    private var createdAt: Date? = null
    private var timestamp: Long? = null

    // ========== 伴生对象（类似 Java 的静态成员）==========
    companion object {
        const val DEFAULT_NAME = "Default"
        const val MAX_SIZE = 1000
        private var instanceCount = 0
        private val registry = mutableMapOf<String, LargeConcurrentTestClass>()

        fun create(name: String): LargeConcurrentTestClass {
            val instance = LargeConcurrentTestClass(name)
            registry[name] = instance
            return instance
        }

        fun getInstance(name: String): LargeConcurrentTestClass? {
            return registry[name]
        }

        fun getInstanceCount(): Int {
            return instanceCount
        }

        fun clearRegistry() {
            registry.clear()
        }
    }

    // ========== 构造方法 ==========
    constructor() {
        this.name = DEFAULT_NAME
        this.id = ++instanceCount
        this.items = mutableListOf()
        this.metadata = mutableMapOf()
        this.numbers = mutableSetOf()
    }

    constructor(name: String) {
        this.name = name
        this.id = ++instanceCount
        this.items = mutableListOf()
        this.metadata = mutableMapOf()
        this.numbers = mutableSetOf()
    }

    constructor(name: String, id: Int) {
        this.name = name
        this.id = id
        this.items = mutableListOf()
        this.metadata = mutableMapOf()
        this.numbers = mutableSetOf()
    }

    // ========== Getter 和 Setter 方法 ==========
    fun getName(): String {
        return name
    }

    fun setName(name: String) {
        this.name = name
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getValue(): Double {
        return value
    }

    fun setValue(value: Double) {
        this.value = value
    }

    fun isActive(): Boolean {
        return active
    }

    fun setActive(active: Boolean) {
        this.active = active
    }

    fun getItems(): MutableList<String> {
        return items
    }

    fun setItems(items: MutableList<String>) {
        this.items = items
    }

    fun getMetadata(): MutableMap<String, Any> {
        return metadata
    }

    fun setMetadata(metadata: MutableMap<String, Any>) {
        this.metadata = metadata
    }

    fun getNumbers(): MutableSet<Int> {
        return numbers
    }

    fun setNumbers(numbers: MutableSet<Int>) {
        this.numbers = numbers
    }

    // ========== 业务方法 ==========
    fun processItems() {
        items.forEach { item ->
            println("Processing: $item")
        }
    }

    fun processItemsWithPredicate(filter: (String) -> Boolean) {
        items.filter(filter).forEach { println(it) }
    }

    fun <T> transformItem(transformer: (String) -> T): T? {
        return items.firstOrNull()?.let(transformer)
    }

    fun addItem(item: String) {
        items.add(item)
    }

    fun removeItem(item: String): Boolean {
        return items.remove(item)
    }

    fun containsItem(item: String): Boolean {
        return items.contains(item)
    }

    fun getItemCount(): Int {
        return items.size
    }

    fun clearItems() {
        items.clear()
    }

    fun addNumber(number: Int) {
        numbers.add(number)
    }

    fun removeNumber(number: Int): Boolean {
        return numbers.remove(number)
    }

    fun calculateSum(): Int {
        return numbers.sum()
    }

    fun calculateAverage(): Double {
        return numbers.average()
    }

    fun putMetadata(key: String, value: Any) {
        metadata[key] = value
    }

    fun getMetadata(key: String): Any? {
        return metadata[key]
    }

    fun hasMetadata(key: String): Boolean {
        return metadata.containsKey(key)
    }

    // ========== 私有方法 ==========
    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw IllegalArgumentException("Name cannot be null or empty")
        }
    }

    private fun updateTimestamp() {
        this.timestamp = System.currentTimeMillis()
        this.createdAt = Date(timestamp!!)
    }

    // ========== 内部接口 ==========
    interface ItemProcessor {
        fun process(item: String)
        fun validate(item: String): Boolean
        fun transform(item: String): String
    }

    interface DataTransformer<T, R> {
        fun transform(data: T): R
        fun transformBatch(dataList: List<T>): List<R>
        fun transformMap(dataMap: Map<String, T>): Map<String, R>
    }

    // ========== 嵌套类 ==========
    class Builder {
        private var name: String = ""
        private var id: Int = 0
        private var value: Double = 0.0
        private var active: Boolean = false
        private val items: MutableList<String> = mutableListOf()

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun id(id: Int): Builder {
            this.id = id
            return this
        }

        fun value(value: Double): Builder {
            this.value = value
            return this
        }

        fun active(active: Boolean): Builder {
            this.active = active
            return this
        }

        fun addItem(item: String): Builder {
            this.items.add(item)
            return this
        }

        fun build(): LargeConcurrentTestClass {
            val instance = LargeConcurrentTestClass(name, id)
            instance.setValue(value)
            instance.setActive(active)
            instance.setItems(ArrayList(items))
            return instance
        }
    }

    class Validator {
        companion object {
            fun validateName(name: String): Boolean {
                return name.isNotBlank() && name.length <= 100
            }

            fun validateId(id: Int): Boolean {
                return id > 0
            }

            fun validateValue(value: Double): Boolean {
                return value >= 0 && value <= 1000000
            }
        }
    }

    // ========== 内部类 ==========
    inner class ItemHandler(private var handlerName: String) {
        fun handle(item: String) {
            println("$handlerName handling: $item")
            this@LargeConcurrentTestClass.addItem(item)
        }

        fun processAll() {
            this@LargeConcurrentTestClass.processItems()
        }

        fun getHandlerName(): String {
            return handlerName
        }

        fun setHandlerName(handlerName: String) {
            this.handlerName = handlerName
        }
    }

    inner class MetadataManager {
        private val customMetadata = mutableMapOf<String, String>()

        fun addCustomMetadata(key: String, value: String) {
            customMetadata[key] = value
            this@LargeConcurrentTestClass.putMetadata(key, value)
        }

        fun getCustomMetadata(key: String): String? {
            return customMetadata[key]
        }

        fun mergeMetadata() {
            this@LargeConcurrentTestClass.metadata.putAll(customMetadata)
        }

        fun clearAll() {
            customMetadata.clear()
            this@LargeConcurrentTestClass.metadata.clear()
        }
    }

    // ========== 枚举 ==========
    enum class Status {
        ACTIVE,
        INACTIVE,
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    enum class Priority(val level: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        URGENT(4);

        fun isHigherThan(other: Priority): Boolean {
            return this.level > other.level
        }
    }

    // ========== 对象表达式（类似 Java 的匿名类）==========
    fun useAnonymousClass() {
        val task = object : Runnable {
            override fun run() {
                println("Running anonymous task for: $name")
                processItems()
            }
        }
        task.run()
    }

    fun useAnonymousComparator() {
        items.sortWith { o1, o2 ->
            o1.compareTo(o2, ignoreCase = true)
        }
    }

    fun createCallable(): Callable<String> {
        return object : Callable<String> {
            override fun call(): String {
                return "Result from $name"
            }
        }
    }

    fun createSupplier(): Supplier<String> {
        return object : Supplier<String> {
            override fun get(): String {
                return "Supplied from $name with ${items.size} items"
            }
        }
    }

    fun createConsumer(): Consumer<String> {
        return object : Consumer<String> {
            override fun accept(s: String) {
                addItem(s)
                println("Consumed: $s")
            }
        }
    }

    // ========== 使用 Lambda 的方法 ==========
    fun processWithLambda() {
        items.forEach { item ->
            println("Lambda processing: $item")
            if (item.length > 5) {
                metadata["long_$item"] = item.length
            }
        }
    }

    fun filterItems(predicate: (String) -> Boolean): List<String> {
        return items.filter(predicate)
    }

    // ========== 嵌套更深的类 ==========
    class NestedClass(private var nestedName: String, private var nestedValue: Int = 0) {
        fun process() {
            println("Nested class processing: $nestedName")
        }

        class DeeplyNestedClass(private val deepName: String) {
            fun deepProcess() {
                println("Deeply nested processing: $deepName")
            }

            interface DeepInterface {
                fun deepMethod()
                fun deepTransform(input: String): String
            }
        }
    }

    // ========== 泛型方法 ==========
    fun <T : Number> getMaxValue(values: List<T>): T? {
        return values.maxOrNull()
    }

    fun <T, R> mapList(source: List<T>, mapper: (T) -> R): List<R> {
        return source.map(mapper)
    }

    fun <K, V> filterMap(
        source: Map<K, V>,
        predicate: (Map.Entry<K, V>) -> Boolean
    ): Map<K, V> {
        return source.filter(predicate)
    }

    // ========== 更多方法用于增加代码量 ==========
    fun method1() {
        println("Method 1")
    }

    fun method2() {
        println("Method 2")
    }

    fun method3() {
        println("Method 3")
    }

    fun method4() {
        println("Method 4")
    }

    fun method5() {
        println("Method 5")
    }

    fun calculate1(): Int {
        return 1
    }

    fun calculate2(): Int {
        return 2
    }

    fun calculate3(): Int {
        return 3
    }

    fun calculate4(): Int {
        return 4
    }

    fun calculate5(): Int {
        return 5
    }

    fun format1(): String {
        return "Format 1"
    }

    fun format2(): String {
        return "Format 2"
    }

    fun format3(): String {
        return "Format 3"
    }

    fun check1(): Boolean {
        return true
    }

    fun check2(): Boolean {
        return false
    }

    fun check3(): Boolean {
        return active
    }

    // ========== 数据类风格的属性（Kotlin 特有）==========
    val displayName: String
        get() = "$name (ID: $id)"

    val itemCount: Int
        get() = items.size

    val isEmpty: Boolean
        get() = items.isEmpty()

    val isNotEmpty: Boolean
        get() = items.isNotEmpty()

    // ========== 扩展函数风格的辅助方法 ==========
    fun String.validateItem(): Boolean {
        return this.isNotBlank() && this.length >= 3
    }

    fun Int.isPositive(): Boolean {
        return this > 0
    }

    // ========== 重写 Object 方法 ==========
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LargeConcurrentTestClass) return false
        return id == other.id && name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id
        return result
    }

    override fun toString(): String {
        return "LargeConcurrentTestClass(name='$name', id=$id, value=$value, active=$active, itemCount=${items.size})"
    }
}

