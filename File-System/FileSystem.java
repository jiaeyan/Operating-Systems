package fileSystem;
import java.io.IOException;

/**
 * Your FileSystem implementation must create and manage a private
 * disk and provide standard file system operations. Your file system
 * will reside on the disk.
 *
 * You will also need to manage other private instance variables in
 * your FileSystem implementation to track open files, inodes, etc.
 */
public interface FileSystem {
    /**
     * Initialize the disk to the state representing an empty file
     * system.
     *
     * Create an empty file system on a private Disk. The file system
     * will comprise size number of blocks, which
     * <emph>includes</emph> isize number of inode blocks (i.e.,
     * formatDisk(100, 10) should produce a system with 100 blocks
     * total, 10 of which are InodeBlocks.
     *
     * @param  size  Number of blocks total in the file system
     * @param  isize Number of InodeBlocks in the file system
     * @return       0 on success, -1 otherwise
     */
    public int formatDisk(int size, int isize) throws IOException;
    
    /**
     * Stop the file system. Must close all open files and shut down
     * the disk.
     *
     * @return 0 on success, -1 otherwise
     */
    public int shutdown() throws IOException;
    
    /**
     * Create a new file.
     *
     * @return fd of the new open file on success, -1 otherwise
     */
    public int create() throws IOException;
    
    /**
     * Open an existing file.
     *
     * @param  inumber inumber of the file to open
     * @return         fd of the open file on success, -1 otherwise
     */
    public int open(int inumber) throws IOException;
    
    /**
     * Get the inumber associated with an open file.
     *
     * @param  fd  fd of an open file
     * @return     inumber of the file, -1 on error
     */
    public int inumber(int fd) throws IOException;
    
    /**
     * Read up to buffer.length bytes into buffer from open file
     * identified by fd and returns the number of bytes read.
     *
     * If there are fewer than buffer.length bytes between the current
     * seek pointer and the end of the file (as indicated by the size
     * field in the inode), only the remaining bytes are read. If the
     * current seek pointer is greater than or equal to the file size,
     * then read returns zero and the buffer is unmodified. (The
     * current seek pointer cannot be less than zero). The seek
     * pointer is incremented by the number of bytes read.
     *
     * @param  fd     fd of an open file
     * @param  buffer buffer to read into; reads buffer.length bytes
     * @return        number of bytes read into buffer, -1 on error
     */
    public int read(int fd, byte[] buffer) throws IOException;
    
    /**
     * Write buffer.length bytes from buffer to the file starting at
     * the current seek pointer and advances the seek pointer by that
     * amount.
     *
     * It is not an error if the seek pointer is greater than the size
     * of the file. In this case, holes may be created.
     *
     * @param  fd     fd of an open file
     * @param  buffer buffer to write from; writes buffer.length bytes
     * @return        number of bytes written, -1 on error
     */
    public int write(int fd, byte[] buffer) throws IOException;
    
    /**
     * Seek by offset relative to whence in the open file identified
     * by fd.
     *
     * Seeking adjusts the current seek pointer in an open file. An
     * open file is identified by fd in a FileTable; each fd is
     * associated with a separate seek pointer (even if two fds are
     * associated with the same file on disk). The whence argument
     * specifies the position in the file to which offset is
     * relative. See the documentation for {@link Whence} for more
     * information.
     *
     * @param  fd     fd of an open file
     * @param  offset seek amount relative to whence
     * @param  whence relative position for seek
     * @return        new seek pointer (relative to beginning of file),
     *                or -1 on error
     */
    public int seek(int fd, int offset, Whence whence) throws IOException;
    
    /**
     * Close the file identified by fd.
     *
     * Must write the inode for the file back to disk and free the
     * entry for fd in the FileTable.
     *
     * @param  fd  fd of an open file
     * @return     0 on success, -1 on error
     */
    public int close(int fd) throws IOException;
    
    /**
     * Delete the file specified by inumber. It is an error to delete
     * an open file.
     *
     * Must free the inode and all blocks associated with the file.
     *
     * @param  inumber inumber of file (must <emph>not</emph> be open)
     * @return         0 on success, -1 on error
     */
    public int delete(int inumber) throws IOException;
}
