package persistent;

/**
 *
 */
public class Bits {

    public static int setBit(int mask, int position) {
        return mask | (1 << position);
    }

    public static int clearBit(int mask, int position) {
        return mask & ~(1 << position);
    }

    public static boolean bitSet(int mask, int position) {
        return (mask & (1 << position)) != 0;
    }

    public static boolean bitClear(int n, int bucket) {
        return !bitSet(n, bucket);
    }

}
