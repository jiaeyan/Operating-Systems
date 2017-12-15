package fileSystem;
import java.util.Arrays;

/**
 * Indirect pointers from an Inode lead to an IndirectBlock.
 *
 * An IndirectBlock is pointed to either by another indirect block or
 * by the indirect pointers in an Inode (pointers 10, 11, and 12). An
 * indirect block is packed with pointers, meaning that it holds
 * BLOCK_SIZE / POINTER_SIZE pointers.
 */
public class IndirectBlock {
    public static final int COUNT = Disk.BLOCK_SIZE / 4;
    public int ptr[] = new int[COUNT];

    public IndirectBlock() {
        clear();
    }
    
    public void clear() {
        for(int i = 0; i < COUNT; i++) {
            ptr[i] = 0;
        }
    }
    
    public String toString() {
        return
            "IndirectBlock(\n  " + Arrays.toString(ptr).replace(", ", "\n  ") +
            ")";
    }
}
