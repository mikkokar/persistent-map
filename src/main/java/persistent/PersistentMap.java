package persistent;

import com.google.common.annotations.VisibleForTesting;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import static java.lang.String.format;


public class PersistentMap<K, V> {
    private static final int SUBHASH_MASK = 31;

    private final SubMap root;
    private final int elements;

    public PersistentMap() {
        this(null, 0);
    }

    private PersistentMap(SubMap root, int elements) {
        this.root = root;
        this.elements = elements;
    }

    @VisibleForTesting
    static <K, V> SubMap insertCollidingKeys(int levelFrom, KeyValue<K, V> oldKeyValue, K key, V value) {
        int oldHashCode = oldKeyValue.key().hashCode();
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
            KeyValue<K, V> oldKv = new KeyValue<>(oldKeyValue.key(), oldKeyValue.value());
            KeyValue<K, V> newKv = new KeyValue<>(key, value);
            newKv.next(oldKv);
            subMap = new SubMap(subhashForLevel(oldHashCode, levelTo--), newKv);
        } else {
            KeyValue<K, V> newKv = new KeyValue<>(key, value);
            subMap = new SubMap(subhashForLevel(newHashCode, levelTo), newKv, subhashForLevel(oldHashCode, levelTo--), oldKeyValue);
        }

        while (levelTo > levelFrom) {
            subMap = new SubMap<>().set(subhashForLevel(newHashCode, levelTo--), subMap);
        }

        return subMap;
    }

    private SubMap<K, V> insert(SubMap root, int level, K key, V value, int hashCode) {
        int bucket = subhashForLevel(hashCode, level);

        Object entry = root.get(bucket);
        if (isVacant(entry)) {
            return root.set(bucket, new KeyValue<>(key, value));
        } else if (isKeyValue(entry)) {
            KeyValue<K, V> oldKeyValue = (KeyValue<K, V>) entry;
            SubMap newSubMap = insertCollidingKeys(level, oldKeyValue, key, value);
            return root.replace(bucket, newSubMap);
        } else {
            SubMap<K, V> newSubmap = insert((SubMap) entry, level + 1, key, value, hashCode);
            return root.replace(bucket, newSubmap);
        }
    }

    public PersistentMap<K, V> put(K key, V value) {
        SubMap<K, V> mapRoot = root != null ? root : new SubMap<>();
        SubMap<K, V> newRoot = insert(mapRoot, 0, key, value, key.hashCode());
        return new PersistentMap<>(newRoot, elements + 1);
    }

    private V valueFromChain(KeyValue<K, V> keyValue, K key) {
        while (keyValue != null && !keyValue.key().equals(key)) {
            keyValue = keyValue.next();
        }
        return keyValue != null ? keyValue.value() : null;
    }

    private V lookup(SubMap root, int level, K key, int hashCode) {
        int bucket = subhashForLevel(hashCode, level);

        Object entry = root.get(bucket);
        if (isVacant(entry)) {
            return null;
        } else if (isKeyValue(entry)) {
            KeyValue<K, V> keyValue = ((KeyValue<K, V>) entry);
            if (level < 6) {
                return hashCode == keyValue.key().hashCode() ? keyValue.value() : null;
            } else {
                return valueFromChain(keyValue, key);
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

    private void dumpLevel(StringBuilder builder, SubMap root, int level) {
        for (int i = 0; i < 32; i++) {
            if (!root.isPresent(i)) {
                continue;
            }
            Object entry = root.get(i);
            if (isVacant(entry)) {
                builder.append(prefix(level));
                builder.append(format("- %02d: %s", i, "ERROR - vacant entry"));
                builder.append('\n');
                continue;
            }

            if (isKeyValue(entry)) {
                builder.append(prefix(level));
                KeyValue<K, V> keyValue = (KeyValue<K, V>) entry;
                builder.append(format("- %02d: %s", i, keyValue.toString()));
                builder.append('\n');
            } else {
                builder.append(prefix(level));
                builder.append(format("- %02d: SubMap", i));
                builder.append('\n');

                dumpLevel(builder, (SubMap) entry, level + 1);
            }
        }
    }

    public String dump() {
        StringBuilder builder = new StringBuilder("Root:\n");
        if (root == null) {
            builder.append("<empty>");
            return builder.toString();
        }

        dumpLevel(builder, root, 0);

        return builder.toString();
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


    public PersistentMap<K, V> remove(K key) {
        throw new NotImplementedException();
    }

    public Object nodeAt(int level, int hashCode) {
        throw new NotImplementedException();
    }

    @VisibleForTesting
    static class KeyValue<K, V> {
        private final K key;
        private final V value;
        private KeyValue<K, V> next;

        public KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }

        public void next(KeyValue<K, V> nextValue) {
            this.next = nextValue;
        }

        public KeyValue<K, V> next() {
            return next;
        }

        public K key() {
            return key;
        }

        public V value() {
            return value;
        }

        @Override
        public String toString() {
            String nextStr = next != null ? " -> " + next.toString() : "";
            int hashCode = key.hashCode();
            return format("KeyValue(%d [%s], %s)%s", hashCode, hashToDottedString(hashCode), value, nextStr);
        }
    }

    @VisibleForTesting
    static class SubMap<K, V> {
        final private Object[] hashArray;
        final private int mask;

        public SubMap() {
            // Todo: is it necessary to create an array of zero objects?
            this.hashArray = new Object[0];
            this.mask = 0;
        }

        public SubMap(int mask, Object[] hashArray) {
            this.mask = mask;
            this.hashArray = hashArray;
        }

        public <K, V> SubMap(int bucket, KeyValue<K, V> keyValue) {
            this.hashArray = new Object[1];
            this.hashArray[0] = keyValue;
            this.mask = setBit(0, bucket);;
        }

        public <K, V> SubMap(int bucket1, KeyValue<K, V> keyValue1, int bucket2, KeyValue<K, V> keyValue2) {
            this.hashArray = new Object[2];
            if (bucket1 < bucket2) {
                int mask = 0;
                this.hashArray[0] = keyValue1;
                mask = setBit(mask, bucket1);

                this.hashArray[1] = keyValue2;
                mask = setBit(mask, bucket2);

                this.mask = mask;
            } else {
                int mask = 0;

                this.hashArray[0] = keyValue2;
                mask = setBit(mask, bucket2);

                this.hashArray[1] = keyValue1;
                mask = setBit(mask, bucket1);

                this.mask = mask;
            }
        }

        public boolean isEmpty() {
            return capacity() == 0 || mask == 0;
        }

        public void removeEntry(int bucket) {
            hashArray[bucket] = null;
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

        public SubMap<K, V> set(int bucket, Object entry) {
//            assert (bitClear(mask, bucket));

            Object[] newHashArray = Arrays.copyOf(hashArray, capacity() + 1);
            int newMask = setBit(mask, bucket);
            int entryIndex = populationCountAt(newMask, bucket);
            newHashArray[entryIndex] = entry;
            System.arraycopy(hashArray, entryIndex, newHashArray, entryIndex + 1, capacity() - entryIndex);

            return new SubMap<>(newMask, newHashArray);
        }

        public SubMap<K, V> replace(int bucket, Object entry) {
//            assert (bitSet(mask, bucket));

            int newMask = setBit(mask, bucket);
            Object[] newHashArray = Arrays.copyOf(hashArray, capacity());

            int entryIndex = populationCountAt(mask, bucket);
            newHashArray[entryIndex] = entry;

            return new SubMap<>(newMask, newHashArray);
        }

        private boolean bitClear(int mask, int bucket) {
            return !bitSet(mask, bucket);
        }

        @VisibleForTesting
        static int setBit(int mask, int position) {
            return mask | (1 << position);
        }

        @VisibleForTesting
        static boolean bitSet(int mask, int position) {
            return (mask & (1 << position)) != 0;
        }

        public boolean isPresent(int bucket) {
            return bitSet(mask, bucket);
        }
    }

    private boolean isSubmap(Object entry) {
        return entry instanceof SubMap;
    }

    private boolean isVacant(Object entry) {
        return entry == null;
    }

    private boolean isKeyValue(Object entry) {
        return entry instanceof KeyValue && ((KeyValue) entry).key != null;
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

    public static String hashToDottedString(int hashCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(subhashForLevel(hashCode, 0));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 1));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 2));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 3));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 4));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 5));
        sb.append(".");
        sb.append(subhashForLevel(hashCode, 6));
        return sb.toString();
    }
}
