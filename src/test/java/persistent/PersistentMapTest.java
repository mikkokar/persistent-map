package persistent;

import persistent.PersistentMap.KeyValue;
import persistent.PersistentMap.SubMap;
import persistent.support.HashCodes;
import persistent.support.HashCodes.TestKey;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static persistent.PersistentMap.insertCollidingKeys;
import static persistent.support.HashCodes.makeHash;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class PersistentMapTest {
    private HashCodes hashCodes;

    @BeforeMethod
    public void setUp() {
        hashCodes = new HashCodes();
    }

    @Test
    public void insertsObjectInEmptyMap() {
        PersistentMap<String, String> v1 = PersistentMap.create();
        assertThat(v1.isEmpty(), is(true));
        assertThat(v1.size(), is(0));

        PersistentMap<String, String> v2 = v1.put("foo", "bar");
        assertThat(v2.isEmpty(), is(false));
        assertThat(v2.size(), is(1));
        assertThat(v2.get("foo"), is("bar"));
        assertThat(v1.get("foo"), is(nullValue()));
    }

    @Test
    public void replacesExistingKeyValueEntryWithSubmap() {
        PersistentMap<TestKey, String> v1 = PersistentMap.create();

        TestKey keyA = hashCodes.key(5, 3, 0, 0, 0, 0, 0, "a");
        TestKey keyB = hashCodes.key(5, 2, 0, 0, 0, 0, 0, "b");

        PersistentMap<TestKey, String> v2 = v1.put(keyA, "a");
        PersistentMap<TestKey, String> v3 = v2.put(keyB, "b");

        System.out.println("v3: " + v3.dump());

        assertThat(v3.isEmpty(), is(false));
        assertThat(v3.size(), is(2));
        assertThat(v3.get(keyA), is("a"));
        assertThat(v3.get(keyB), is("b"));

        assertThat(v2.isEmpty(), is(false));
        assertThat(v2.size(), is(1));
        assertThat(v2.get(keyA), is("a"));
        assertThat(v2.get(keyB), is(nullValue()));
    }

    @Test
    public void resolvesHashKeyCollisions() {
        PersistentMap<TestKey, String> v1 = PersistentMap.create();

        TestKey keyA = hashCodes.key(0, 1, 2, 3, 4, 5, 3, "a");
        TestKey keyB = hashCodes.key(0, 1, 2, 3, 4, 5, 3, "b");
        TestKey keyC = hashCodes.key(0, 1, 2, 3, 4, 5, 3, "c");

        PersistentMap<TestKey, String> v2 = v1.put(keyA, "a");
        PersistentMap<TestKey, String> v3 = v2.put(keyB, "b");

        assertThat(v3.isEmpty(), is(false));
        assertThat(v3.size(), is(2));

        System.out.println(v3.dump());

        assertThat(v3.get(keyA), is("a"));
        assertThat(v3.get(keyB), is("b"));
        assertThat(v3.get(keyC), is(nullValue()));
    }

    @Test
    public void removesKeyValueFromRoot() {
        PersistentMap<TestKey, String> v1 = PersistentMap.create();

        TestKey keyA = hashCodes.key(4, 0, 0, 0, 0, 0, 0, "a");
        TestKey keyB = hashCodes.key(5, 0, 0, 0, 0, 0, 0, "b");

        PersistentMap<TestKey, String> v2 = v1.put(keyA, "a");
        PersistentMap<TestKey, String> v3 = v2.put(keyB, "b");

        PersistentMap<TestKey, String> v4 = v3.remove(keyA);
        assertThat(v4.get(keyA), is(nullValue()));
        assertThat(v4.size(), is(1));

        PersistentMap<TestKey, String> v5 = v4.remove(keyB);
        assertThat(v5.get(keyB), is(nullValue()));
        assertThat(v5.size(), is(0));
    }

    @Test
    public void removesOneOfManyKeyValuesFromSubMap() {
        // Leaves ohter key-values in the SubMap .

        PersistentMap<TestKey, String> v1 = PersistentMap.create();

        TestKey keyA = hashCodes.key(1, 4, 0, 0, 0, 0, 0, "a");
        TestKey keyB = hashCodes.key(1, 5, 0, 0, 0, 0, 0, "b");

        PersistentMap<TestKey, String> v2 = v1.put(keyA, "a");
        PersistentMap<TestKey, String> v3 = v2.put(keyB, "b");

        PersistentMap<TestKey, String> v4 = v3.remove(keyA);
        assertThat(v4.get(keyA), is(nullValue()));
        assertThat(v4.size(), is(1));

        PersistentMap<TestKey, String> v5 = v4.remove(keyB);
        assertThat(v5.get(keyB), is(nullValue()));
        assertThat(v5.size(), is(0));
    }

    @Test
    public void removesLeafSubmap() {
        PersistentMap<TestKey, String> v1 = PersistentMap.create();

        TestKey keyA = hashCodes.key(1, 2, 4, 0, 0, 0, 0, "a");
        TestKey keyB = hashCodes.key(1, 2, 5, 0, 0, 0, 0, "b");

        PersistentMap<TestKey, String> v2 = v1.put(keyA, "a");
        PersistentMap<TestKey, String> v3 = v2.put(keyB, "b");

        PersistentMap<TestKey, String> v4 = v3.remove(keyA);
        assertThat(v4.get(keyA), is(nullValue()));
        assertThat(v4.size(), is(1));

        PersistentMap<TestKey, String> v5 = v4.remove(keyB);
        assertThat(v5.get(keyB), is(nullValue()));
        assertThat(v5.size(), is(0));

        System.out.println("version 5: " + v5.dump());

        assertThat(v5.nodeAt(1, makeHash(1, 2, 0, 0, 0, 0, 0)), is(nullValue()));
        assertThat(v5.nodeAt(0, makeHash(1, 2, 0, 0, 0, 0, 0)), is(notNullValue()));
    }

    @Test
    public void insertCollidingKeys_createsSubmapChainWhenKeysCollide() {
        /*
         *  level:  0  1  2  3  4  5  6
         *  keyA:   7  6  5  4  3  2  1
         *  keyB:   7  6  5  4  3  2  1
         *          ^                 ^
         *          |                 |
         *       root              full
         *                    collision
         */

        TestKey keyA = hashCodes.key(7, 6, 5, 4, 3, 2, 1, "a");
        TestKey keyB = hashCodes.key(7, 6, 5, 4, 3, 2, 1, "b");
        KeyValue<TestKey, String> oldKv = new KeyValue<>(keyA, "a");

        SubMap root = SubMap.create();

        root = root.set(7, insertCollidingKeys(0, oldKv, keyB, "b"));

        SubMap subMap1 = (SubMap) root.get(7);
        SubMap subMap2 = (SubMap) subMap1.get(6);
        SubMap subMap3 = (SubMap) subMap2.get(5);
        SubMap subMap4 = (SubMap) subMap3.get(4);
        SubMap subMap5 = (SubMap) subMap4.get(3);
        SubMap subMap6 = (SubMap) subMap5.get(2);
        KeyValue<TestKey, String> kvNew = (KeyValue<TestKey, String>) subMap6.get(1);

        assertThat(kvNew.key(), is(keyB));
        assertThat(kvNew.value(), is("b"));
        //        assertThat(kvNew.storedHashCode(), is());
        //        assertThat(kvNew.next(), is(oldKv));
    }

    @Test
    public void insertCollidingKeys_createsSubmapChainWhenKeysCollide_starting_from_level1() {

        /*
         *  level:  0  1  2  3  4  5  6
         *  keyA:   7  6  5  4  3  2  1
         *  keyB:   7  6  5  4  3  2  1
         *          ^  ^              ^
         *          |  |              |
         *       root  |           full
         *             |      collision
         *         start
         */

        TestKey keyA = hashCodes.key(7, 6, 5, 4, 3, 2, 1, "a");
        TestKey keyB = hashCodes.key(7, 6, 5, 4, 3, 2, 1, "b");
        KeyValue<TestKey, String> oldKv = new KeyValue<>(keyA, "a");

        SubMap root = SubMap.create();

        root = root.set(6, insertCollidingKeys(1, oldKv, keyB, "b"));

        SubMap subMap2 = (SubMap) root.get(6);
        SubMap subMap3 = (SubMap) subMap2.get(5);
        SubMap subMap4 = (SubMap) subMap3.get(4);
        SubMap subMap5 = (SubMap) subMap4.get(3);
        SubMap subMap6 = (SubMap) subMap5.get(2);
        KeyValue<TestKey, String> kvNew = (KeyValue<TestKey, String>) subMap6.get(1);

        assertThat(kvNew.key(), is(keyB));
        assertThat(kvNew.value(), is("b"));
        //        assertThat(kvNew.storedHashCode(), is());
        //        assertThat(kvNew.next(), is(oldKv));
    }

    @Test
    public void insertCollidingKeys_createsSubmapChainWhenKeysCollide_starting_from_level_diff1() {
        /*
         *  level:  0  1  2  3  4  5  6
         *  keyA:   7  6  4  0  0  0  0
         *  keyB:   7  6  5  0  0  0  0
         *             ^  ^
         *             |  |
         *          root  |
         *                |
         *             keys
         *             differ
         */
        TestKey keyA = hashCodes.key(0, 6, 4, 0, 0, 0, 0, "a");
        TestKey keyB = hashCodes.key(0, 6, 5, 0, 0, 0, 0, "b");
        KeyValue<TestKey, String> oldKv = new KeyValue<>(keyA, "a");

        SubMap root = SubMap.create();

        root = root.set(6, insertCollidingKeys(1, oldKv, keyB, "b"));

        SubMap subMap2 = (SubMap) root.get(6);
        KeyValue<TestKey, String> kv4 = (KeyValue<TestKey, String>) subMap2.get(4);
        KeyValue<TestKey, String> kv5 = (KeyValue<TestKey, String>) subMap2.get(5);

        assertThat(kv4.key(), is(keyA));
        assertThat(kv5.key(), is(keyB));
        //        assertThat(kvNew.storedHashCode(), is());
        //        assertThat(kvNew.next(), is(oldKv));
    }


    @Test
    public void stressTestMap() {
        PersistentMap<String, String> hamt = PersistentMap.create();

        Map<String, String> refMap = new HashMap<>();
        for (int i = 0; i < 12000; i++) {
            String key = randomString();
            String value = randomString();
            refMap.put(key, value);
        }

        for (Map.Entry<String, String> entry : refMap.entrySet()) {
            hamt = hamt.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : refMap.entrySet()) {
            String expected = entry.getValue();
            String value = hamt.get(entry.getKey());
            assertThat(value, is(expected));
        }

        System.out.println(hamt.metrics());
    }

    Random r = new Random();
    private String randomString() {
        return String.format("%04x%04x%04x%04x", r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt());
    }

}