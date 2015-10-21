package persistent;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static persistent.Bits.bitSet;
import static persistent.Bits.clearBit;
import static persistent.Bits.setBit;

/**
 *
 */
public class BitsTest {

    @Test
    public void bitSet_returnsTrueWhenGivenBitIsSet() {
        assertThat(bitSet(1, 0), is(true));
        assertThat(bitSet(1, 1), is(false));
        assertThat(bitSet(1, 31), is(false));

        assertThat(bitSet(0x80000000, 31), is(true));
        assertThat(bitSet(0x80000000, 0), is(false));
        assertThat(bitSet(0x80000000, 30), is(false));
    }

    @Test
    public void setBit_returnsNumberWithGivenBitSet() {
        assertThat(setBit(0x80000001, 0), is(0x80000001));
        assertThat(setBit(0x80000001, 31), is(0x80000001));

        assertThat(setBit(0x80000001, 1), is(0x80000003));
    }

    @Test
    public void clearBit_returnsNumberWithGivenBitClear() {
        int num = 0xFFFFFFFF;

        assertThat(clearBit(num, 31), is(0x7FFFFFFF));
        assertThat(clearBit(num, 30), is(0xBFFFFFFF));
        assertThat(clearBit(num, 29), is(0xDFFFFFFF));

        assertThat(clearBit(num, 2), is(0xFFFFFFFB));
        assertThat(clearBit(num, 1), is(0xFFFFFFFD));
        assertThat(clearBit(num, 0), is(0xFFFFFFFE));
    }

}