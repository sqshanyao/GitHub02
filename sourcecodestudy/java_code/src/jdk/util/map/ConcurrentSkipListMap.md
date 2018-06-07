# ConcurrentSkipListMap
* 介绍
TreeMap集合是基于红黑树（Red-Black tree）的 NavigableMap实现。该集合最重要的特点就是可排序，该映射根据其键的自然顺序进行排序，或者根据创建映射时提供的 Comparator 进行排序，具体取决于使用的构造方法。这句话是什么意思呢？就是说TreeMap可以对添加进来的元素进行排序，可以按照默认的排序方式，也可以自己指定排序方式。

* 根据上一条，我们要想使用TreeMap存储并排序我们自定义的类（如User类），那么必须自己定义比较机制：一种方式是User类去实现java.lang.Comparable接口，并实现其compareTo()方法。另一种方式是写一个类（如MyCompatator）去实现java.util.Comparator接口，并实现compare()方法，然后将MyCompatator类实例对象作为TreeMap的构造方法参数进行传参（当然也可以使用匿名内部类），这些比较方法是怎么被调用的将在源码中讲解。

## 成员变量

比较器对象，用于定义比较规则
* private final Comparator<? super K> comparator;

## 构造函数
	默认比较规则
	public TreeMap() {
        comparator = null;
    }
	自定义比较规则
	public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }
	
	public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }
	
	public TreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }