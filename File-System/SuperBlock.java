package fileSystem;
/**
 * A disk block which stores information about a file system.
 */
public class SuperBlock {
    public int size;       // size of file system (in blocks)
    public int msize;      // number of blocks used by the free space map
    public int isize;      // number of inode blocks

    // first bits of free map (size of block - space for 3 ints of metadata)
    public byte freeMap[] = new byte[Disk.BLOCK_SIZE - 12];

    public String toString () {
        return
            "SuperBlock(size: " + size +
            ", isize: " + isize +
            ", msize: " + msize +
            ", FreeMap(\n  " +
            Bitwise.toString(freeMap, ",", "\n  ", 8) + ")";
    }

    /**
     * Compute the number of data blocks in the file system. Will
     * return 0 if the file system is uninitialized.
     *
     * The number of data blocks is the number of blocks in the file
     * system minus the super block size (1), the number of inode
     * blocks, and the number of freemap blocks.
     *
     * @return number of data blocks
     */
    public int dsize() {
        return size - isize - msize - 1;
    }

    /**
     * Compute the number of the first freemap block (returns 0 if
     * there are no freemap blocks because the entire freemap fits in
     * the SuperBlock).
     *
     * @return block number of the first freemap block
     */
    public int mblock0() {
        return msize == 0 ? 0 : 1;
    }

    /**
     * Compute the number of the first inode block.
     *
     * @return block number of the first inode block
     */
    public int iblock0() {
        return 1 + msize;
    }

    /**
     * Compute the number of the first data block.
     *
     * @return block number of the first data block
     */
    public int dblock0() {
        return iblock0() + isize;
    }
}
