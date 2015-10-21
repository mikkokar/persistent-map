package persistent;

import org.testng.annotations.Test;
import persistent.PersistentMap.KeyValue;
import persistent.PersistentMap.SubMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 *
 */
public class SubMapTest {

    @Test
    public void settingEntryReturnsNewCopy() {
        SubMap v1 = new SubMap();

        SubMap v2 = v1.set(3, "foo");
        assertThat(v1.isEmpty(), is(true));
        assertThat(v1.get(3), is(nullValue()));
        assertThat(v1.capacity(), is(0));

        assertThat(v2.isEmpty(), is(false));
        assertThat(v2.get(3), is("foo"));
        assertThat(v2.capacity(), is(1));
    }

    @Test
    public void settingEntryUpdatesBitPopulationCount() {
        SubMap v1 = new SubMap();

        SubMap v2 = v1.set(3, "foo-3");
        assertThat(v1.isEmpty(), is(true));
        assertThat(v1.get(3), is(nullValue()));
        assertThat(v1.capacity(), is(0));

        assertThat(v2.isEmpty(), is(false));
        assertThat(v2.get(3), is("foo-3"));
        assertThat(v2.capacity(), is(1));

        SubMap v3 = v2.set(5, "foo-5");
        assertThat(v3.isEmpty(), is(false));
        assertThat(v3.get(3), is("foo-3"));
        assertThat(v3.get(5), is("foo-5"));
        assertThat(v3.capacity(), is(2));

        SubMap v4 = v3.set(4, "foo-4");
        assertThat(v4.isEmpty(), is(false));
        assertThat(v4.get(3), is("foo-3"));
        assertThat(v4.get(5), is("foo-5"));
        assertThat(v4.get(4), is("foo-4"));
        assertThat(v4.capacity(), is(3));
    }

    @Test
    public void replacingEntryUpdatesBitPopulationCount() {
        SubMap v1 = new SubMap();

        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(5, "foo-5");
        SubMap v4 = v3.set(4, "foo-4");

        SubMap v5 = v4.replace(3, "bar");
        assertThat(v5.get(3), is("bar"));
        assertThat(v5.get(5), is("foo-5"));
        assertThat(v5.get(4), is("foo-4"));
    }

    @Test
    public void exposesPresenseOfEntryAtIndex() {
        SubMap v1 = new SubMap();
        SubMap v2 = v1.set(3, "foo-3");
        SubMap v3 = v2.set(7, "foo-7");
        SubMap v4 = v3.set(31, "foo-31");

        assertThat(v4.isPresent(0), is(false));
        assertThat(v4.isPresent(1), is(false));
        assertThat(v4.isPresent(2), is(false));
        assertThat(v4.isPresent(3), is(true));
        assertThat(v4.isPresent(4), is(false));
        assertThat(v4.isPresent(6), is(false));
        assertThat(v4.isPresent(7), is(true));
        assertThat(v4.isPresent(8), is(false));
        assertThat(v4.isPresent(30), is(false));
        assertThat(v4.isPresent(31), is(true));
    }

    /*
                // [  5  4  3  2  1  0 ]
                //          x
                // [     4  3  2  1  0 ]  new_1
                //

     */
    @Test
    public void remove_assertsWhenAttemptToRemoveFromEmptyMap() {
        // Todo;
    }

    @Test
    public void remove_returnsEmptySubmapWhenLastEntryIsRemoved() {
        SubMap v1 = new SubMap(1, new KeyValue<>("Foo", "bar"));
        SubMap v2 = v1.removeEntry(1);
        assertThat(v2.isEmpty(), is(true));
        assertThat(v2.capacity(), is(0));
    }

    @Test
    public void remove_removesLowestEntry() {
        KeyValue<String, String> kv1 = new KeyValue<>("Foo", "this");
        KeyValue<String, String> kv2 = new KeyValue<>("Bar", "that");

        SubMap v1 = new SubMap(1, kv1, 2, kv2);
        SubMap v2 = v1.removeEntry(1);
        assertThat(v2.get(2), is(kv2));
        assertThat(v2.get(1), is(nullValue()));
        assertThat(v2.capacity(), is(1));
    }

    @Test
    public void remove_removesHighestEntry() {
        KeyValue<String, String> kv1 = new KeyValue<>("Foo", "this");
        KeyValue<String, String> kv2 = new KeyValue<>("Bar", "that");

        SubMap v1 = new SubMap(1, kv1, 2, kv2);
        SubMap v2 = v1.removeEntry(2);
        assertThat(v2.get(1), is(kv1));
        assertThat(v2.get(2), is(nullValue()));
        assertThat(v2.capacity(), is(1));
    }

    @Test
    public void remove_removesEntryInTheMiddle() {
        KeyValue<String, String> kv1 = new KeyValue<>("Foo", "this");
        KeyValue<String, String> kv2 = new KeyValue<>("Bar", "that");
        KeyValue<String, String> kv3 = new KeyValue<>("Baz", "this and that");

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