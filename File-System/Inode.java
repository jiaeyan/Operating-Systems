package fileSystem;
import java.util.Arrays;

/**
 * Each file in your file system is described by an <emph>index
 * node</emph> (Inode for short).
 *
 * Use Inodes to describe each file in your file system. Inodes are
 * used to find the blocks that belong to a file, and also contain
 * metadata about the file.
 */
public class Inode {
    public final static int SIZE = 64; // size in bytes
    public int flags;
    public int owner;
    public int size;
    public int ptr[] = new int[13];

    public void allocate() {
        flags = 1;
        owner = 0;
        size  = 0;
        Arrays.fill(ptr, 0);
    }

    public String toString() {
        return
            "Inode(flags: " + flags +
            ", owner: " + owner +
            ", size: " + size +
            ", ptr: [" + Arrays.toString(ptr) + "])";
    }
}
