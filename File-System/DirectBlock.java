package fileSystem;
/**
 * Represents a direct block (block containing file data). A
 * DirectBlock tracks its location on disk (so it can be easily read
 * and written) and also the offset at which you want to read / write
 * bytes.
 *
 * To use a DirectBlock, you instantiate it for a particular offset
 * within a particular disk block, like so:
 *
 * <pre>
 * DirectBlock block = new DirectBlock(disk, blockNum, blockOff);
 * </pre>
 *
 * If you want to read from a DirectBlock you should first execute
 * read(). You then use copyTo and copyFrom to copy from the block
 * into a buffer and copy from a buffer into the block
 * (respectively). To save changes to the block to disk, you execute
 * the save() method.
 *
 * Here is an example of printing out the first 10 bytes of a file
 * (assuming those bytes are all normal character bytes) represented
 * by an Inode data structure referenced by the variable inode:
 *
 * <pre>
 * byte[] buf = new byte[10];
 * DirectBlock block = new DirectBlock(disk, inode.ptr[0], 0);
 * block.copyTo(buf, 0);
 * for(i = 0; i &lt; buf.length; ++i) {
 *     System.out.print((char)buf[i]);
 * }
 * </pre>
 *
 * Note that in general, we should use the return value of copyTo to
 * ensure that we don't read garbage from buf. If we want to read more
 * than one blocks worth, we need to make multiple calls to copyTo,
 * each from a different DirectBlock. Here is an example that reads
 * 1024 characters from a file starting at the 10th byte in the file
 * (note that this approach only works for direct pointers, you need
 * more sophistication to support large files):
 *
 * <pre>
 * byte[] buf = new byte[1024];
 * int bufOff = 0, blockOff = 10, ptrNum = 0;
 * while(bufOff &lt; buf.length) {
 *     DirectBlock block = new DirectBlock(disk, inode.ptr[ptrNum], blockOff);
 *     
 * </pre>
 *
 * Note that DirectBlock doesn't understand the length of a file, so
 * if the file data doesn't extend to the end of a block, then it is
 * up to the caller of copyTo to ensure that buf is of the appropriate
 * length. A similar argument applies for copyFrom and extending the
 * length of a file.
 */
public class DirectBlock {
    public static final DirectBlock hole = new DirectBlock();
    private byte[] block = new byte[Disk.BLOCK_SIZE];
    private Disk disk;
    private int blockNum;
    private int blockOff;
    private boolean fresh;
    private boolean inCore = false;
    private boolean dirty  = false;

    private DirectBlock() {
        // Constructs a hole
    }

    public DirectBlock(Disk disk, int blockNum, int blockOff, boolean fresh) {
        this.blockNum = blockNum;
        this.blockOff = blockOff;
        this.disk     = disk;
        this.fresh    = fresh;
    }

    public int copyTo(byte[] buf, int off) {
        if(this != hole && ! inCore)
            read();
        int i = 0;
        for(; i + off < buf.length && i + blockOff < Disk.BLOCK_SIZE; ++i)
            buf[off + i] = block[blockOff + i];
        return i;
    }

    public int copyFrom(byte[] buf, int off) {
        if(! inCore && ! isCompleteOverwrite(buf.length))
            read();
        int i = 0;
        for(; i + off < buf.length && i + blockOff < Disk.BLOCK_SIZE; ++i)
            block[blockOff + i] = buf[off + i];
        dirty = true;
        return i;
    }

    private boolean isCompleteOverwrite(int len) {
        return fresh || (blockOff == 0 && len >= 512);
    }

    public void read() {
        if(disk != null)
            disk.read(blockNum, block);
        inCore = true;
    }

    public void save() {
        if(disk != null)
            disk.write(blockNum, block);
        dirty = false;
    }
}
