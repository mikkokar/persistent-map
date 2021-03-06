package persistent;

import org.testng.annotations.Test;
import persistent.PersistentMap.KeyEntry;
import persistent.PersistentMap.SubMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 *
 */
public class SubMapTest {

    @Test
    public void settingEntryIncreasesCapacity() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo");
        SubMap v3a = v2.set(9, "foo-bar");
        SubMap v3b = v2.set(10, "foo-baz");

        assertThat(v1.isEmpty(), is(true));
        assertThat(v1.capacity(), is(0));

        assertThat(v2.isEmpty(), is(false));
        assertThat(v2.capacity(), is(1));

        assertThat(v3a.isEmpty(), is(false));
        assertThat(v3a.capacity(), is(2));

        assertThat(v3b.isEmpty(), is(false));
        assertThat(v3b.capacity(), is(2));
    }

    @Test
    public void settingEntryReturnsNewCopy() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo");

        assertThat(v1.get(3), is(nullValue()));
        assertThat(v2.get(3), is("foo"));
    }

    @Test
    public void canStoreIntoMostSignificantBitBucket() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(31, "foo31");

        assertThat(v1.get(31), is(nullValue()));
        assertThat(v2.get(31), is("foo31"));
    }

    @Test
    public void settingEntryUpdatesBitPopulationCount() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo-3");
        assertThat(v1.get(3), is(nullValue()));

        assertThat(v2.get(3), is("foo-3"));

        SubMap v3 = v2.set(5, "foo-5");
        assertThat(v3.get(3), is("foo-3"));
        assertThat(v3.get(5), is("foo-5"));

        SubMap v4 = v3.set(4, "foo-4");
        assertThat(v4.get(3), is("foo-3"));
        assertThat(v4.get(5), is("foo-5"));
        assertThat(v4.get(4), is("foo-4"));
    }

    @Test
    public void settingMostSignificantBitEntryUpdatesBitPopulationCount() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(31, "foo-31");

        assertThat(v3.get(3), is("foo-3"));
        assertThat(v3.get(31), is("foo-31"));
    }

    @Test
    public void replacingEntryUpdatesBitPopulationCount() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(5, "foo-5");
        SubMap v4 = v3.set(4, "foo-4");

        SubMap v5 = v4.replace(3, "bar");

        assertThat(v5.get(3), is("bar"));
        assertThat(v5.get(5), is("foo-5"));
        assertThat(v5.get(4), is("foo-4"));
    }

    @Test
    public void replacingMostSignificantBitEntryUpdatesBitPopulationCount() {
        SubMap v1 = SubMap.create();

        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(5, "foo-5");
        SubMap v4 = v3.set(31, "foo-31");

        SubMap v5 = v4.replace(31, "bar-31");

        assertThat(v5.get(3), is("foo-3"));
        assertThat(v5.get(5), is("foo-5"));
        assertThat(v5.get(31), is("bar-31"));
    }

    @Test
    public void exposesPresenseOfEntryAtIndex() {
        SubMap v1 = SubMap.create();
        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(7, "foo-7");
        SubMap v4 = v3.set(31, "foo-31");

        assertThat(v4.isPresent(3), is(true));
        assertThat(v4.isPresent(7), is(true));
        assertThat(v4.isPresent(31), is(true));

        assertThat(v4.isPresent(0), is(false));
        assertThat(v4.isPresent(1), is(false));
        assertThat(v4.isPresent(2), is(false));
        assertThat(v4.isPresent(4), is(false));
        assertThat(v4.isPresent(6), is(false));
        assertThat(v4.isPresent(8), is(false));
        assertThat(v4.isPresent(30), is(false));
    }

    /*
                // [  5  4  3  2  1  0 ]
                //          x
                // [     4  3  2  1  0 ]  new_1
                //

     */
//    @Test
//    public void remove_assertsWhenAttemptToRemoveFromEmptyMap() {
//         Todo;
//    }

    @Test
    public void remove_returnsEmptySubmapWhenLastEntryIsRemoved() {
        SubMap v1 = new SubMap(1, new KeyEntry<>("Foo", "bar"));
        SubMap v2 = v1.removeEntry(1);
        assertThat(v2.isEmpty(), is(true));
        assertThat(v2.capacity(), is(0));
    }

    @Test
    public void remove_removesLowestEntry() {
        KeyEntry<String, String> kv1 = new KeyEntry<>("Foo", "this");
        KeyEntry<String, String> kv2 = new KeyEntry<>("Bar", "that");

        SubMap v1 = new SubMap(1, kv1, 2, kv2);
        SubMap v2 = v1.removeEntry(1);
        assertThat(v2.get(2), is(kv2));
        assertThat(v2.get(1), is(nullValue()));
        assertThat(v2.capacity(), is(1));
    }

    @Test
    public void remove_removesHighestEntry() {
        KeyEntry<String, String> kv1 = new KeyEntry<>("Foo", "this");
        KeyEntry<String, String> kv2 = new KeyEntry<>("Bar", "that");

        SubMap v1 = new SubMap(1, kv1, 2, kv2);
        SubMap v2 = v1.removeEntry(2);
        assertThat(v2.get(1), is(kv1));
        assertThat(v2.get(2), is(nullValue()));
        assertThat(v2.capacity(), is(1));
    }

    @Test
    public void remove_removesEntryInTheMiddle() {
        KeyEntry<String, String> kv1 = new KeyEntry<>("Foo", "this");
        KeyEntry<String, String> kv2 = new KeyEntry<>("Bar", "that");
        KeyEntry<String, String> kv3 = new KeyEntry<>("Baz", "this and that");

        SubMap v1 = new SubMap(1, kv1, 2, kv2);
        SubMap v2 = v1.set(3, kv3);

        SubMap v3 = v2.removeEntry(2);

        assertThat(v3.get(1), is(kv1));
        assertThat(v3.get(2), is(nullValue()));
        assertThat(v3.get(3), is(kv3));
        assertThat(v3.capacity(), is(2));
    }

    @Test
    public void testPopulationCount() {
        assertThat(PersistentMap.populationCountAt(0xF0000001, 31), is(4));
    }
}