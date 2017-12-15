package fileSystem;
/**
 * Stores 1 bit for each data block. The first bits of the free map
 * are stored in the super block. If the bitmap is too large to be
 * packed entirely into the superblock, then the remaining bits are
 * packed into 1 or more free map blocks.
 */
public class FreeMapBlock {
    public static final int ENTRIES_PER_BLOCK = Disk.BLOCK_SIZE * 8;
    public byte[] map = new byte[Disk.BLOCK_SIZE];

    public String toString() {
        return
            "FreeMapBlock(\n  " + Bitwise.toString(map, ",", "\n  ", 8) + ")";
    }
}
