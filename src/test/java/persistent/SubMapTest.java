package persistent;

import persistent.PersistentMap.SubMap;
import org.testng.annotations.Test;

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

    @Test
    public void testBitPosition() {
        assertThat(SubMap.bitSet(1, 0), is(true));
        assertThat(SubMap.bitSet(1, 1), is(false));
        assertThat(SubMap.bitSet(1, 31), is(false));

        assertThat(SubMap.bitSet(0x80000000, 31), is(true));
        assertThat(SubMap.bitSet(0x80000000, 0), is(false));
        assertThat(SubMap.bitSet(0x80000000, 30), is(false));
    }

    @Test
    public void testSetBit() {
        assertThat(SubMap.setBit(0x80000001, 0), is(0x80000001));
        assertThat(SubMap.setBit(0x80000001, 31), is(0x80000001));

        assertThat(SubMap.setBit(0x80000001, 1), is(0x80000003));
    }

    @Test
    public void testPopulationCount() {
        assertThat(PersistentMap.populationCountAt(0xF0000001, 31), is(4));

    }
}