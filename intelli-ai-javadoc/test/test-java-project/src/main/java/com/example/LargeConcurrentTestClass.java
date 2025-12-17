package com.example;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LargeConcurrentTestClass {

    // ========== 字段 ==========
    private String name;
    private int id;
    private double value;
    private boolean active;
    private List<String> items;
    private Map<String, Object> metadata;
    /** 用于存储整数集合, 通常用于需要唯一整数标识的场景 */
    private Set<Integer> numbers;
    private Optional<String> optionalValue;
    private Future<String> futureResult;
    private Date createdAt;
    private Long timestamp;

    // ========== 静态字段 ==========
    public static final String DEFAULT_NAME = "Default";
    public static final int MAX_SIZE = 1000;
    private static int instanceCount = 0;
    private static final Map<String, LargeConcurrentTestClass> registry = new HashMap<>();

    // ========== 构造方法 ==========

    public LargeConcurrentTestClass() {
        this.name = DEFAULT_NAME;
        this.id = ++instanceCount;
        this.items = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.numbers = new HashSet<>();
    }

    public LargeConcurrentTestClass(String name) {
        this.name = name;
        this.id = ++instanceCount;
        this.items = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.numbers = new HashSet<>();
    }

    public LargeConcurrentTestClass(String name, int id) {
        this.name = name;
        this.id = id;
        this.items = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.numbers = new HashSet<>();
    }

    // ========== Getter 和 Setter 方法 ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Set<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(Set<Integer> numbers) {
        this.numbers = numbers;
    }

    // ========== 业务方法 ==========

    public void processItems() {
        items.forEach(item -> System.out.println("Processing: " + item));
    }

    public void processItemsWithPredicate(Predicate<String> filter) {
        items.stream()
            .filter(filter)
            .forEach(System.out::println);
    }

    public <T> T transformItem(Function<String, T> transformer) {
        if (items.isEmpty()) {
            return null;
        }
        return transformer.apply(items.get(0));
    }

    public void addItem(String item) {
        items.add(item);
    }

    public boolean removeItem(String item) {
        return items.remove(item);
    }

    public boolean containsItem(String item) {
        return items.contains(item);
    }

    public int getItemCount() {
        return items.size();
    }

    public void clearItems() {
        items.clear();
    }

    public void addNumber(int number) {
        numbers.add(number);
    }

    public boolean removeNumber(int number) {
        return numbers.remove(number);
    }

    public int calculateSum() {
        return numbers.stream().mapToInt(Integer::intValue).sum();
    }

    public double calculateAverage() {
        return numbers.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    // ========== 静态方法 ==========

    public static LargeConcurrentTestClass create(String name) {
        LargeConcurrentTestClass instance = new LargeConcurrentTestClass(name);
        registry.put(name, instance);
        return instance;
    }

    public static LargeConcurrentTestClass getInstance(String name) {
        return registry.get(name);
    }

    public static int getInstanceCount() {
        return instanceCount;
    }

    public static void clearRegistry() {
        registry.clear();
    }

    // ========== 私有方法 ==========

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    private void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
        this.createdAt = new Date(timestamp);
    }

    // ========== 内部接口 ==========

    public interface ItemProcessor {
        void process(String item);

        boolean validate(String item);

        String transform(String item);
    }

    public interface DataTransformer<T, R> {
        R transform(T data);

        List<R> transformBatch(List<T> dataList);

        Map<String, R> transformMap(Map<String, T> dataMap);
    }

    // ========== 静态内部类 ==========

    public static class Builder {
        private String name;
        private int id;
        private double value;
        private boolean active;
        private List<String> items;

        public Builder() {
            this.items = new ArrayList<>();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder addItem(String item) {
            this.items.add(item);
            return this;
        }

        public LargeConcurrentTestClass build() {
            LargeConcurrentTestClass instance = new LargeConcurrentTestClass(name, id);
            instance.setValue(value);
            instance.setActive(active);
            instance.setItems(new ArrayList<>(items));
            return instance;
        }
    }

    public static class Validator {
        public static boolean validateName(String name) {
            return name != null && !name.trim().isEmpty() && name.length() <= 100;
        }

        public static boolean validateId(int id) {
            return id > 0;
        }

        public static boolean validateValue(double value) {
            return value >= 0 && value <= 1000000;
        }
    }

    // ========== 非静态内部类 ==========

    public class ItemHandler {
        private String handlerName;

        public ItemHandler(String handlerName) {
            this.handlerName = handlerName;
        }

        public void handle(String item) {
            System.out.println(handlerName + " handling: " + item);
            LargeConcurrentTestClass.this.addItem(item);
        }

        public void processAll() {
            LargeConcurrentTestClass.this.processItems();
        }

        public String getHandlerName() {
            return handlerName;
        }

        public void setHandlerName(String handlerName) {
            this.handlerName = handlerName;
        }
    }

    public class MetadataManager {
        private Map<String, String> customMetadata;

        public MetadataManager() {
            this.customMetadata = new HashMap<>();
        }

        public void addCustomMetadata(String key, String value) {
            customMetadata.put(key, value);
            LargeConcurrentTestClass.this.putMetadata(key, value);
        }

        public String getCustomMetadata(String key) {
            return customMetadata.get(key);
        }

        public void mergeMetadata() {
            LargeConcurrentTestClass.this.metadata.putAll(customMetadata);
        }

        public void clearAll() {
            customMetadata.clear();
            LargeConcurrentTestClass.this.metadata.clear();
        }
    }

    // ========== 枚举 ==========

    public enum Status {
        ACTIVE,
        INACTIVE,
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum Priority {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        URGENT(4);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isHigherThan(Priority other) {
            return this.level > other.level;
        }
    }

    // ========== 使用匿名类的示例方法 ==========

    public void useAnonymousClass() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("Running anonymous task for: " + name);
                processItems();
            }
        };
        task.run();
    }

    public void useAnonymousComparator() {
        Collections.sort(items, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
    }

    public Callable<String> createCallable() {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Result from " + name;
            }
        };
    }

    public Supplier<String> createSupplier() {
        return new Supplier<String>() {
            @Override
            public String get() {
                return "Supplied from " + name + " with " + items.size() + " items";
            }
        };
    }

    public Consumer<String> createConsumer() {
        return new Consumer<String>() {
            @Override
            public void accept(String s) {
                addItem(s);
                System.out.println("Consumed: " + s);
            }
        };
    }

    // ========== 使用 Lambda 的方法 ==========

    public void processWithLambda() {
        items.forEach(item -> {
            System.out.println("Lambda processing: " + item);
            if (item.length() > 5) {
                metadata.put("long_" + item, item.length());
            }
        });
    }

    public List<String> filterItems(Predicate<String> predicate) {
        List<String> result = new ArrayList<>();
        items.stream()
            .filter(predicate)
            .forEach(result::add);
        return result;
    }

    // ========== 嵌套更深的类 ==========
/**
 * 嵌套类 {@code NestedClass}
 * <p>
 * 该类封装了一个名称 {@code nestedName} 和一个数值 {@code nestedValue}, 并提供 {@link #process()} 方法用于演示处理逻辑.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */

/**
 * 更深层嵌套类 {@code DeeplyNestedClass}
 * <p>
 * 该类包含一个名称 {@code deepName}, 并提供 {@link #deepProcess()} 方法用于演示更深层的处理逻辑.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */

/**
 * NestedClass
 * <p>
 * 作为外部类的静态内部类, 封装了一个名称和数值属性, 并提供了基本的处理逻辑.
 * 该类可用于演示或测试内部类的使用场景.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */

/**
 * DeeplyNestedClass
 * <p>
 * 进一步嵌套在 NestedClass 内部的静态类, 持有一个深层名称属性, 并提供深层处理方法.
 * 该类主要用于展示多层内部类的结构与功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */

    public static class NestedClass {
        private String nestedName;
        private int nestedValue;

        public NestedClass(String nestedName) {
            this.nestedName = nestedName;
            this.nestedValue = 0;
        }

        public void process() {
            System.out.println("Nested class processing: " + nestedName);
        }

        public static class DeeplyNestedClass {
            private String deepName;

            public DeeplyNestedClass(String deepName) {
                this.deepName = deepName;
            }

            public void deepProcess() {
                System.out.println("Deeply nested processing: " + deepName);
            }

            public interface DeepInterface {
                void deepMethod();

                String deepTransform(String input);
            }
        }
    }

    // ========== 泛型方法 ==========

    public <T extends Number> T getMaxValue(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
            .max((a, b) -> Double.compare(a.doubleValue(), b.doubleValue()))
            .orElse(null);
    }

    public <T, R> List<R> mapList(List<T> source, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : source) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public <K, V> Map<K, V> filterMap(Map<K, V> source, Predicate<Map.Entry<K, V>> predicate) {
        Map<K, V> result = new HashMap<>();
        source.entrySet().stream()
            .filter(predicate)
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    // ========== 更多方法用于增加代码量 ==========

    public void method1() {
        System.out.println("Method 1");
    }

    public void method2() {
        System.out.println("Method 2");
    }

    public void method3() {
        System.out.println("Method 3");
    }

    public void method4() {
        System.out.println("Method 4");
    }

    public void method5() {
        System.out.println("Method 5");
    }

    public int calculate1() {
        return 1;
    }

    public int calculate2() {
        return 2;
    }

    public int calculate3() {
        return 3;
    }

    public int calculate4() {
        return 4;
    }

    public int calculate5() {
        return 5;
    }

    public String format1() {
        return "Format 1";
    }

    public String format2() {
        return "Format 2";
    }

    public String format3() {
        return "Format 3";
    }

    public boolean check1() {
        return true;
    }

    public boolean check2() {
        return false;
    }

    public boolean check3() {
        return active;
    }

    // ========== 重写 Object 方法 ==========

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LargeConcurrentTestClass that = (LargeConcurrentTestClass) obj;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    @Override
    public String toString() {
        return "LargeConcurrentTestClass{" +
            "name='" + name + '\'' +
            ", id=" + id +
            ", value=" + value +
            ", active=" + active +
            ", itemCount=" + (items != null ? items.size() : 0) +
            '}';
    }
}

