package persistent;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static persistent.Bits.bitClear;
import static persistent.Bits.bitSet;
import static persistent.Bits.clearBit;
import static persistent.Bits.setBit;


public final class PersistentMap<K, V> {
    private static final int SUBHASH_MASK = 31;
    private static final PersistentMap EMPTY_MAP = new PersistentMap();

    private final SubMap root;
    private final int elements;


    /**
     * Creates an empty persistent map
     */
    public static <K, V> PersistentMap<K, V> create() {
        return (PersistentMap<K, V>) EMPTY_MAP;
    }

    private PersistentMap() {
        this(null, 0);
    }

    private PersistentMap(SubMap root, int elements) {
        this.root = root;
        this.elements = elements;
    }

    @VisibleForTesting
    static <K, V> SubMap insertCollidingKeys(int levelFrom, KeyEntry<K, V> oldKeyEntry, K key, V value) {
        int oldHashCode = oldKeyEntry.key().hashCode();
        int newHashCode = key.hashCode();
        int levelTo;
        for (levelTo = levelFrom + 1; levelTo <= 6; levelTo++) {
            int oldBucket = subhashForLevel(oldHashCode, levelTo);
            int newBucket = subhashForLevel(newHashCode, levelTo);
            if (oldBucket != newBucket) {
                break;
            }
        }

        SubMap subMap = null;
        if (levelTo == 7) {
            // Key collision occurred:
            levelTo--;
            KeyEntry<K, V> oldKv = new KeyEntry<>(oldKeyEntry.key(), oldKeyEntry.value());
            KeyEntry<K, V> newKv = new KeyEntry<>(key, value);
            newKv.next(oldKv);
            subMap = new SubMap(subhashForLevel(oldHashCode, levelTo--), newKv);
        } else {
            KeyEntry<K, V> newKv = new KeyEntry<>(key, value);
            subMap = new SubMap(subhashForLevel(newHashCode, levelTo), newKv, subhashForLevel(oldHashCode, levelTo--), oldKeyEntry);
        }

        while (levelTo > levelFrom) {
            subMap = SubMap.create().set(subhashForLevel(newHashCode, levelTo--), subMap);
        }

        return subMap;
    }

    private SubMap insert(SubMap root, int level, K key, V value, int hashCode) {
        int bucket = subhashForLevel(hashCode, level);

        Object entry = root.get(bucket);
        if (isVacant(entry)) {
            return root.set(bucket, new KeyEntry<>(key, value));
        } else if (isKeyValue(entry)) {
            KeyEntry<K, V> oldKeyEntry = (KeyEntry<K, V>) entry;
            SubMap newSubMap = insertCollidingKeys(level, oldKeyEntry, key, value);
            return root.replace(bucket, newSubMap);
        } else {
            SubMap newSubmap = insert((SubMap) entry, level + 1, key, value, hashCode);
            return root.replace(bucket, newSubmap);
        }
    }

    public PersistentMap<K, V> put(K key, V value) {
        SubMap mapRoot = root != null ? root : SubMap.create();
        SubMap newRoot = insert(mapRoot, 0, key, value, key.hashCode());
        return new PersistentMap<>(newRoot, elements + 1);
    }

    private SubMap removeKey(SubMap root, int level, K key) {
        int hashCode = key.hashCode();
        int bucket = subhashForLevel(hashCode, level);

        Object entry = root.get(bucket);
        if (isKeyValue(entry)) {
            return root.removeEntry(bucket);
        } else if (isSubmap(entry)) {
            SubMap subMap = (SubMap) entry;
            SubMap copy = removeKey(subMap, level + 1, key);
            if (copy != null && copy.isEmpty()) {
                return root.removeEntry(bucket);
            } else {
                return root.replace(bucket, copy);
            }
        }

        return null;
    }

    public PersistentMap<K, V> remove(K key) {
        if (root == null) {
            return this;
        }

        SubMap newRoot = removeKey(root, 0, key);
        if (newRoot != null) {
            return new PersistentMap<>(newRoot, elements - 1);
        } else {
            return this;
        }
    }

    private V valueFromChain(KeyEntry<K, V> keyEntry, K key) {
        while (keyEntry != null && !keyEntry.key().equals(key)) {
            keyEntry = keyEntry.next();
        }
        return keyEntry != null ? keyEntry.value() : null;
    }

    private V lookup(SubMap root, int level, K key, int hashCode) {
        int bucket = subhashForLevel(hashCode, level);

        Object entry = root.get(bucket);
        if (isVacant(entry)) {
            return null;
        } else if (isKeyValue(entry)) {
            KeyEntry<K, V> keyEntry = ((KeyEntry<K, V>) entry);
            if (level < 6) {
                return hashCode == keyEntry.key().hashCode() ? keyEntry.value() : null;
            } else {
                return valueFromChain(keyEntry, key);
            }
        } else {
            SubMap subMap = (SubMap) entry;
            return lookup(subMap, level + 1, key, key.hashCode());
        }
    }

    public V get(K key) {
        if (root == null) {
            return null;
        }

        return lookup(root, 0, key, key.hashCode());
    }


    private String prefix(int level) {
        String prefix = " |";
        for (int i = 0; i < level; i++) {
            prefix += "       |";
        }
        return prefix;
    }


    public boolean isEmpty() {
        return elements == 0;
    }

    public int size() {
        return elements;
    }

    public String metrics() {
        // No-op
        return "";
    }

    public Object nodeAt(int level, int hashCode) {
        Object desired = root;
        for (int i = 0; i < level; i++) {
            assert (isSubmap(desired));
            SubMap subMap = (SubMap) desired;
            int bucket = subhashForLevel(hashCode, i);
            desired = subMap.get(bucket);
        }
        return desired;
    }

    public Set<K> keySet() {
        if (root == null) {
            return Collections.emptySet();
        }
        MapWalker<K, V, Set<K>> walker = new MapWalker<>(
                (keySet, level, bucket, kvEntry, subMapEntry) -> keySet.add(kvEntry.getKey()),
                MapWalker.noAction(),
                MapWalker.noAction());

        return walker.walk(new HashSet<>(), root);
    }

    public List<V> values() {
        if (root == null) {
            return Collections.emptyList();
        }
        MapWalker<K, V, List<V>> walker = new MapWalker<>(
                (values, level, bucket, kvEntry, subMapEntry) -> values.add(kvEntry.getValue()),
                MapWalker.noAction(),
                MapWalker.noAction());

        return walker.walk(new ArrayList<>(), root);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        if (root == null) {
            return Collections.emptySet();
        }
        MapWalker<K, V, Set<Map.Entry<K, V>>> walker = new MapWalker<>(
                (entries, level, bucket, kvEntry, subMapEntry) -> entries.add(kvEntry),
                MapWalker.noAction(),
                MapWalker.noAction());

        return walker.walk(new HashSet<>(), root);
    }

    public String dump() {
        if (root == null) {
            return "Root:\n<empty>";
        }

        MapWalker<K, V, StringBuilder> walker = new MapWalker<>(
                (builder, level, bucket, mapEntry, subMapEntry) -> {
                    builder.append(prefix(level));
                    builder.append(format("- %02d: %s", bucket, mapEntry.toString()));
                    builder.append('\n');
                },
                (builder, level, bucket, mapEntry, subMapEntry) -> {
                    builder.append(prefix(level));
                    builder.append(format("- %02d: SubMap", bucket));
                    builder.append('\n');
                },
                (builder, level, bucket, mapEntry, subMapEntry) -> {
                    builder.append(prefix(level));
                    builder.append(format("- %02d: %s", bucket, "ERROR - vacant entry"));
                    builder.append('\n');
                });

        return walker.walk(new StringBuilder("Root:\n"), root).toString();
    }

    @VisibleForTesting
    static class KeyEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;
        private KeyEntry<K, V> next;

        public KeyEntry(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }

        void next(KeyEntry<K, V> nextValue) {
            this.next = nextValue;
        }

        KeyEntry<K, V> next() {
            return next;
        }

        K key() {
            return getKey();
        }

        V value() {
            return getValue();
        }

        @Override
        public String toString() {
            String nextStr = next != null ? " -> " + next.toString() : "";
            int hashCode = key.hashCode();
            return format("KeyValue(%d [%s], %s)%s", hashCode, hashToDottedString(hashCode), value, nextStr);
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    @VisibleForTesting
    static class SubMap {
        public static SubMap EMPTY_SUBMAP = new SubMap();

        final private int mask;
        final private Object[] hashArray;

        public static SubMap create() {
            return EMPTY_SUBMAP;
        }

        private SubMap() {
            // Todo: is it necessary to create an array of zero objects?
            this.hashArray = new Object[0];
            this.mask = 0;
        }

        public SubMap(int mask, Object[] hashArray) {
            this.mask = mask;
            this.hashArray = hashArray;
        }

        public <K, V> SubMap(int bucket, KeyEntry<K, V> keyEntry) {
            this.hashArray = new Object[1];
            this.hashArray[0] = keyEntry;
            this.mask = setBit(0, bucket);
        }

        public <K, V> SubMap(int bucket1, KeyEntry<K, V> keyEntry1, int bucket2, KeyEntry<K, V> keyEntry2) {
            this.hashArray = new Object[2];
            if (bucket1 < bucket2) {
                int mask = 0;
                this.hashArray[0] = keyEntry1;
                mask = setBit(mask, bucket1);

                this.hashArray[1] = keyEntry2;
                mask = setBit(mask, bucket2);

                this.mask = mask;
            } else {
                int mask = 0;

                this.hashArray[0] = keyEntry2;
                mask = setBit(mask, bucket2);

                this.hashArray[1] = keyEntry1;
                mask = setBit(mask, bucket1);

                this.mask = mask;
            }
        }

        public boolean isEmpty() {
            return capacity() == 0 || mask == 0;
        }

        public SubMap removeEntry(int bucket) {
            assert (bitSet(mask, bucket));

            if (capacity() > 1) {
                Object[] newHashArray = Arrays.copyOf(hashArray, capacity() - 1);
                int newMask = clearBit(mask, bucket);
                int entryIndex = populationCountAt(newMask, bucket);

                System.arraycopy(hashArray, entryIndex + 1, newHashArray, entryIndex, capacity() - (entryIndex + 1));
                return new SubMap(newMask, newHashArray);
            }

            return EMPTY_SUBMAP;
        }

        public Object get(int bucket) {
            if (bitClear(mask, bucket)) {
                return null;
            } else {
                int count = populationCountAt(mask, bucket);
                return hashArray[count];
            }
        }

        public int capacity() {
            return hashArray.length;
        }

        public SubMap set(int bucket, Object entry) {
            assert (bitClear(mask, bucket));

            Object[] newHashArray = Arrays.copyOf(hashArray, capacity() + 1);
            int newMask = setBit(mask, bucket);
            int entryIndex = populationCountAt(newMask, bucket);
            newHashArray[entryIndex] = entry;
            System.arraycopy(hashArray, entryIndex, newHashArray, entryIndex + 1, capacity() - entryIndex);

            return new SubMap(newMask, newHashArray);
        }

        public SubMap replace(int bucket, Object entry) {
            assert (bitSet(mask, bucket));

            int newMask = setBit(mask, bucket);
            Object[] newHashArray = Arrays.copyOf(hashArray, capacity());

            int entryIndex = populationCountAt(mask, bucket);
            newHashArray[entryIndex] = entry;

            return new SubMap(newMask, newHashArray);
        }

        public boolean isPresent(int bucket) {
            return bitSet(mask, bucket);
        }
    }


    private static boolean isSubmap(Object entry) {
        return entry instanceof SubMap;
    }

    private static boolean isVacant(Object entry) {
        return entry == null;
    }

    private static boolean isKeyValue(Object entry) {
        return entry instanceof KeyEntry;
    }

    public static int populationCountAt(int mask, int bucket) {
        int lsb = (1 << bucket) - 1;
        return Integer.bitCount(mask & lsb);
    }

    public static int subhashForLevel(int hash, int level) {
//        assert (level <= 6);

        if (level < 6) {
            int rshift = 32 - (5 + level * 5);
            return (hash >>> rshift) & SUBHASH_MASK;
        } else {
            return (hash & 3);
        }
    }

    private static String hashToDottedString(int hashCode) {
        return String.valueOf(subhashForLevel(hashCode, 0)) +
                "." + subhashForLevel(hashCode, 1) +
                "." + subhashForLevel(hashCode, 2) +
                "." + subhashForLevel(hashCode, 3) +
                "." + subhashForLevel(hashCode, 4) +
                "." + subhashForLevel(hashCode, 5) +
                "." + subhashForLevel(hashCode, 6);
    }

    interface MapWalkerEventAction<K, V, C> {
        void walkerEvent(C context, int level, int bucket, KeyEntry<K, V> keyValue, SubMap subMap);
    }

    private static class MapWalker<K, V, C> {
        private final MapWalkerEventAction<K, V, C> kvAction;
        private final MapWalkerEventAction<K, V, C> subMapAction;
        private final MapWalkerEventAction<K, V, C> vacantAction;

        public MapWalker(MapWalkerEventAction<K, V, C> kvAction,
                         MapWalkerEventAction<K, V, C> subMapAction,
                         MapWalkerEventAction<K, V, C> vacantAction) {
            this.kvAction = kvAction;
            this.subMapAction = subMapAction;
            this.vacantAction = vacantAction;
        }

        public static <K, V, C> MapWalkerEventAction<K, V, C> noAction() {
            return (keySet, level, bucket, kvEntry, subMapEntry) -> {
            };
        }

        public C walk(C context, SubMap root) {
            return walk(context, root, 0);
        }

        private C walk(C context, SubMap root, int level) {
            for (int i = 0; i < 32; i++) {
                if (!root.isPresent(i)) {
                    continue;
                }

                Object entry = root.get(i);
                if (isKeyValue(entry)) {
                    kvAction.walkerEvent(context, level, i, (KeyEntry<K, V>) entry, null);
                } else if (isSubmap(entry)) {
                    subMapAction.walkerEvent(context, level, i, null, (SubMap) entry);
                    walk(context, (SubMap) entry, level + 1);
                } else {
                    vacantAction.walkerEvent(context, level, i, null, (SubMap) root);
                }
            }
            return context;
        }
    }
}
