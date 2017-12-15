package fileSystem;
/**
 * Tracks open files and provides an interface to interact with files.
 *
 * A FileTable provides an interface to interact with files and to
 * track open files by their file descriptor (an integer, often
 * abbreviated "fd").
 *
 * This class keeps track of all files currently open.  In a real Unix
 * system, this information is split into three levels: There is a
 * table of "in-core inodes" with one entry for each file that has
 * been opened by any process, there is another table with one entry
 * for each "instance" of an open file (allowing multiple seek
 * pointers into the same file), and there is a per-process table
 * mapping instances to file descriptors.}
 */
public class FileTable {
    public static final int MAX_FILES = 20;
    private FileDescriptor[] fds = new FileDescriptor[MAX_FILES];

    /**
     * An ADT that contains data about a file. FileTable uses
     * FileDescriptor to manage file properties for each file
     * descriptor (fd).
     *
     * Properties that each FileDescriptor contains:
     * <ul>
     *   <li>inode</li>
     *   <li>inumber</li>
     *   <li>seek pointer</li>
     * </ul>
     */
    private static class FileDescriptor {
        private Inode inode;
        private int inumber;
        private int seekPtr;
    
        public FileDescriptor(Inode newInode, int newInumber) {
            inode   = newInode;
            inumber = newInumber;
            seekPtr = 0;
        }
    
        public Inode getInode() {
            return inode;
        }
    
        public int getInumber() {
            return inumber;
        }
    
        public int getSeekPointer() {
            return seekPtr;
        }

        public void setSeekPointer(int p) {
            seekPtr = p;
        }

        public void setFileSize(int size) {
            inode.size = size;
        }
    }
    
    /**
     * Return the next free file descriptor position(fd).
     *
     * @return next free file descriptor, or -1 if the file table is
     *         full
     */
    public int allocate() {
        for(int fd = 0; fd < MAX_FILES; fd++) {
            if(fds[fd] == null) {
                return fd;
            }
        }
        System.err.println("Cannot open file (filetable is full)");
        return -1;
    }
    
    /**
     * Add a new file at the specified fd. Use allocate() to get a
     * free file descriptor.
     *
     * @param  inode   the Inode to add
     * @param  inumber the inumber of the file
     * @param  fd      the identifier of the file descriptor
     * @return         0 on success, -1 otherwise
     */
    public int add(Inode inode, int inumber, int fd) {
        if(fds[fd] != null)
            return -1;
        fds[fd] = new FileDescriptor(inode, inumber);
        return 0;
    }
    
    /**
     * Delete a file descriptor from the file table.
     *
     * @param fd The file descriptor to delete
     */
    public void free(int fd) {
        fds[fd] = null;
    }

    /**
     * Checks to see if a fd is valid.
     *
     * @param fd the identifier of a file descriptor
     * @return   true if the fd is valid, false otherwise
     */
    public boolean isValid(int fd) {
        if(fd < 0 || fd >= MAX_FILES) {
            System.err.println(
                "ERROR: Invalid file descriptor (must be 0 <= fd <= " +
                MAX_FILES + ") : " + fd);
            return false;
        }
        if(fds[fd] == null) {
            return false;
        }
        return true;
    }

    /**
     * Get the Inode for a file descriptor.
     *
     * @param  fd the identifier of a file descriptor
     * @return    null if the file descriptor at fd does not exist
     */
    public Inode getInode(int fd) {
        if(fds[fd] == null) {
            return null;
        }
        return fds[fd].getInode();
    }

    /**
     * Get the Inumber for a file descriptor.
     *
     * @param  fd the identifier of a file descriptor
     * @return    0 if invalid, otherwise Inumber
     */
    public int getInumber(int fd) {
        if(fds[fd] == null){
            return 0;
        }
        return fds[fd].getInumber();
    }

    /**
     * Get the seek pointer for a file descriptor.
     *
     * @param  fd the identifier of a file descriptor
     * @return    seek pointer, -1 if fd is invalid
     */
    public int getSeekPointer(int fd) {
        if(fds[fd] == null){
            return -1;
        }
        return fds[fd].getSeekPointer();
    }

    /**
     * Set the seek pointer for a file descriptor.
     *
     * @param  fd the identifier of a file descriptor
     * @param  p  the new seek pointer
     * @return    0 on success, -1 otherwise
     */
    public int setSeekPointer(int fd, int p) {
        if(fds[fd] == null) {
            return -1;
        }
        fds[fd].setSeekPointer(p);
        return 0;
    }

    /**
     * Set the size of the file described by file descriptor fd.
     *
     * @param  fd   the identifier of a file descriptor
     * @param  size the new size of the file
     * @return      0 on success, -1 otherwise
     */
    public int setFileSize(int fd, int size) {
        if(fds[fd] == null) {
            return -1;
        }
        fds[fd].setFileSize(size);
        return 1;
    }

    /**
     * Get the file descriptor (fd) from a file's inumber.
     *
     * @param  inumber the inumber of a file
     * @return         the fd on success, -1 on error
     */
    public int getFdFromInumber(int inumber) {
        for(int fd = 0; fd < MAX_FILES; fd++) {
            if(fds[fd] != null) {
                if (fds[fd].getInumber() == inumber) {
                    return fd;
                }
            }       
        }
        return -1;
    }
}
