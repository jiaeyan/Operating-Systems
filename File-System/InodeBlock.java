package fileSystem;
import java.util.Arrays;

/**
 * Blocks on your disk which contain inodes are InodeBlocks.
 *
 * Each InodeBlock on your disk should be packed with Inodes. That is,
 * each inode block holds BLOCK_SIZE / INODE_SIZE inodes.
 */
public class InodeBlock {
    public static final int COUNT = Disk.BLOCK_SIZE / Inode.SIZE;
    public Inode inodes[] = new Inode[COUNT];

    public InodeBlock() {
        for (int i = 0; i < COUNT; i++) {
            inodes[i] = new Inode();
        }
    }
    
    public String toString() {
        return
            "InodeBlock(\n  " + Arrays.toString(inodes).replace(", ", "\n  ") +
            ")";
    }
}
