package fileSystem;
/**
 * Provides the mechanism for interacting with bits in the
 * free map, regardless of the block in which they reside.
 *
 * The free map is a bitmap that starts in the super block and
 * extends over subsequent metadata blocks. Every data block has a
 * corresponding bit in the free map. Instances of this class
 * provide a consistent interface to free map bits regardless of
 * where they reside in the file system metadata.
 *
 * Will also save dirty freemap blocks whenever you call
 * save(). Freemap blocks are automatically marked dirty whenever
 * you modifiy them.
 */
public class FreeMap {
    private Disk           disk;
    private SuperBlock     superBlock;
    private FreeMapBlock[] freeMapBlocks;
    private boolean[]      blockIsDirty;

    /**
     * Construct a new FreeMap. A FreeMap object caches all freemap
     * blocks and writes dirty blocks back to disk on demand.
     */
    public FreeMap(Disk disk, SuperBlock superBlock) {
        this.disk       = disk;
        this.superBlock = superBlock;
        freeMapBlocks   = new FreeMapBlock[superBlock.msize];
        blockIsDirty    = new boolean[superBlock.msize + 1];
        for(int i = 0; i < superBlock.msize; ++i) {
            freeMapBlocks[i] = new FreeMapBlock();
            disk.read(superBlock.mblock0() + i, freeMapBlocks[i]);
        }
    }

    /**
     * Find a free data block and set its bit.
     *
     * As a side effect, updates the dirty flag for the block
     * containing the updated bit. You must call save() when you are
     * done with an operation that sets one or more bits in the
     * freemap.
     *
     * @return block number of free data block or 0 if no free space
     */
    public int find() {
        for(int n = superBlock.dblock0(); n < superBlock.size; ++n)
            if(new Bit(n).set())
                return n; // found and set a clear bit at n
        return 0;
    }

    /**
     * Clear a freemap bit (free the corresponding data block).
     *
     * As a side effect, updates the dirty flag for the block
     * containing the updated bit. You must call save() when you are
     * done with an operation that sets one or more bits in the
     * freemap.
     *
     * @param blockNum block number of data block
     */
    public void clear(int blockNum) {
        new Bit(blockNum).clear();
    }

    /**
     * Save dirty freemap blocks (will also save the superblock if
     * bits in superBlock.freeMap were changed).
     *
     * You should call this function before you complete any operation
     * that set or cleared freemap bits.
     */
    public void save() {
        if(blockIsDirty[0]) {
            disk.write(0, superBlock);
            blockIsDirty[0] = false;
        }
        for(int i = 0; i < freeMapBlocks.length; ++i) {
            if(blockIsDirty[i + 1]) {
                disk.write(superBlock.mblock0() + i, freeMapBlocks[i]);
                blockIsDirty[i + 1] = false;
            }
        }
    }

    /**
     * Represents an individual bit within the freemap, and provides
     * the means to find, check, set, and clear it, and to mark its
     * containing block dirty.
     */
    private class Bit {
        private byte[] map;    // bitmap within a block
        private int    mapi;   // index of bit in map
        private int    dirtyi; // index of this block in blockIsDirty

        /**
         * Find the portion of the bitmap and offset within that
         * portion where the bit corresponding to blockNum is located.
         *
         * @param blockNum Block number of data block on disk
         */
        public Bit(int blockNum) {
            blockNum -= superBlock.dblock0();
            if(blockNum < superBlock.freeMap.length * 8) {
                // bit is in the map stored in the SuperBlock
                mapi       = blockNum;
                map        = superBlock.freeMap;
                dirtyi     = 0;
            } else {
                // bit is in some FreeMapBlock after the SuperBlock
                blockNum  -= superBlock.freeMap.length * 8;
                int i      = blockNum / FreeMapBlock.ENTRIES_PER_BLOCK;
                mapi       = blockNum % FreeMapBlock.ENTRIES_PER_BLOCK;
                map        = freeMapBlocks[i].map;
                dirtyi     = i + 1; // 0th index is for SuperBlock
            }
        }

        /**
         * Mark the block containing this bit dirty.
         */
        private void markDirty() {
            blockIsDirty[dirtyi] = true;
        }

        /**
         * Check if the bit is set.
         * 
         * @return true if the bit is set, false otherwise.
         */
        public boolean isset() {
            return Bitwise.isset(mapi, map);
        }

        /**
         * Set the bit and mark the containing block dirty if it was
         * clear.
         *
         * @return true if the bit changed, false otherwise
         */
        public boolean set() {
            if(! isset()) {
                Bitwise.set(mapi, map);
                markDirty();
                return true;
            }
            return false;
        }

        /**
         * Clear the bit and mark the containing block dirty if it was
         * set.
         *
         * @return true if the bit changed, false otherwise
         */
        public boolean clear() {
            if(isset()) {
                Bitwise.clear(mapi, map);
                markDirty();
                return true;
            }
            return false;
        }
    }
}
