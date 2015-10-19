package persistent.support;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.toHexString;


public class HashCodes {

    Map <String, TestKey> contents = new HashMap<>();

    /**
     *
     * Enforces the contract between equals() and hashCode() methods.
     *
     * @param i1 - Hash code most significant 5 bits.
     * @param i2 - Hash code next most significant 5 bits.
     * @param i3 - so on..
     * @param i4
     * @param i5
     * @param i6
     * @param i7 - Least significant 2 bits.
     * @param content
     * @return
     */
    public TestKey key(int i1, int i2, int i3, int i4, int i5, int i6, int i7, String content) {
        int hashCode = makeHash(i1, i2, i3, i4, i5, i6, i7);
        if (contents.containsKey(content)) {
            TestKey existingHash = contents.get(content);
            assert (existingHash.hashCode() == hashCode);
        }
        return new TestKey(hashCode, content);
    }

    public static int makeHash(int level1, int level2, int level3, int level4, int level5, int level6, int level7) {
        ensureValid(level1, 32);
        ensureValid(level2, 32);
        ensureValid(level3, 32);
        ensureValid(level4, 32);
        ensureValid(level5, 32);
        ensureValid(level6, 32);
        ensureValid(level7, 4);
        return (level1 << 27) +
                (level2 << 22) +
                (level3 << 17) +
                (level4 << 12) +
                (level5 << 7) +
                (level6 << 2) +
                level7;
    }

    private static void ensureValid(int code, int max) {
        assert(code >= 0 && code < max);
    }

    public static class TestKey {

        /*
         *   content-this    a  a   a
         *   content-that    a  a   b
         *   hashKey-this    a  a   a
         *   hashKey-that    a  b   a
         *
         *   equals          .  .   .
         */

        private final int hashCode;
        private final String content;

        public TestKey(int hashCode, String content) {
            this.hashCode = hashCode;
            this.content = content;
        }

        @Override
        public String toString() {
            return String.format("TestKey{%s, %s}", toHexString(hashCode), content);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestKey) {
                TestKey that = (TestKey) obj;
                return this.content.equals(that.content);
            }
            return false;
        }
    }

}
