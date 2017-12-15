package fileSystem;
import java.io.*;

/**
 * A Disk simulates a block-oriented storage device.
 *
 * The number of blocks on the disk is NUM_BLOCKS, and each block is
 * BLOCK_SIZE bytes in size.
 */
public class Disk {
	public final static String NAME    = "DISK";
	public final static int BLOCK_SIZE = 512;
	public final static int NUM_BLOCKS = 100000;
	public static boolean printStats   = false;

	private int readCount;  // useful for debugging, incremented on each read
	private int writeCount; // useful for debugging, incremented on each write
	private File file;
	private RandomAccessFile disk;

	/**
	 * Construct a new simulated disk.
	 *
	 * Initializes the name of the underlying file to Disk.NAME and
	 * opens that file for RandomAccess in the underlying file
	 * system. You may only interact with this file through the
	 * block-oriented interface of the simulated disk (i.e., through
	 * the public methods that Disk instances provide).
	 *
	 * Will terminate your program if there is an IO problem with the
	 * underlying file.
	 */
	public Disk() {
		try {
			file = new File(NAME);
			disk = new RandomAccessFile(file, "rw");
		} catch(IOException e) {
			System.err.println("Unable to start the disk");
			System.exit(1);
		}
	}

	/**
	 * Read a block into the buffer.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param blocknum block to read (in range 0 .. NUM_BLOCKS - 1)
	 * @param buffer   byte buffer to store the block into; must be
	 *                 large enough to hold BLOCK_SIZE number of bytes
	 * @throws RuntimeException if blocknum is out of range or if your
	 *                          buffer is too small
	 */
	public void read(int blocknum, byte[] buffer) {
		if(buffer.length != BLOCK_SIZE) {
			throw new RuntimeException("read: buffer too small");
		}
		try {
			seek(blocknum);
			disk.read(buffer);
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	}

	/**
	 * Read a block into a SuperBlock.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param  blocknum block to read (in range 0 .. NUM_BLOCKS - 1)
	 * @param  block    SuperBlock to mutate based on contents of block
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void read(int blocknum, SuperBlock block) {
		try {
			seek(blocknum);
			block.size = disk.readInt();
			block.isize = disk.readInt();
			block.msize = disk.readInt();
			for(int i=0; i<block.freeMap.length; i++) {
				block.freeMap[i] = disk.readByte();
			}
		} catch(EOFException e) {
			if(blocknum != 0) {
				System.err.println(e);
				System.exit(1);
			}
			block.size = block.isize = block.msize = 0;
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	}

	/**
	 * Read a block into an InodeBlock.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param  blocknum block to read (in range 0 .. NUM_BLOCKS - 1)
	 * @param  block    InodeBlock to mutate based on contents of block
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void read(int blocknum, InodeBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.inodes.length; i++) {
				block.inodes[i].flags = disk.readInt();
				block.inodes[i].owner = disk.readInt();
				block.inodes[i].size = disk.readInt();
				for(int j=0; j<13; j++) {
					block.inodes[i].ptr[j] = disk.readInt();
				}
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	}

	/**
	 * Read a block into an IndirectBlock.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param  blocknum block to read (in range 0 .. NUM_BLOCKS - 1)
	 * @param  block    IndirectBlock to mutate based on contents of block
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void read(int blocknum, IndirectBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.ptr.length; i++) {
				block.ptr[i] = disk.readInt();
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	}

	/**
	 * Read a block into a FreeMapBlock.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param  blocknum block to read (in range 0 .. NUM_BLOCKS - 1)
	 * @param  block    FreeMapBlock to mutate based on contents of block
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void read(int blocknum, FreeMapBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.map.length; i++) {
				block.map[i] = disk.readByte();
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	}

	/**
	 * Write bytes from the buffer to block number blocknum on disk.
	 *
	 * Immediately terminates your program if there is an underlying
	 * I/O error.
	 *
	 * @param  blocknum block to write (in range 0 .. NUM_BLOCKS - 1)
	 * @param  buffer   bytes to write (must be of size BLOCK_SIZE)
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void write(int blocknum, byte[] buffer) {
		if(buffer.length != BLOCK_SIZE) 
			throw new RuntimeException(
					"Write: bad buffer size " + buffer.length);
		try {
			seek(blocknum);
			disk.write(buffer);
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	/**
	 * Write a SuperBlock to disk.
	 *
	 * @param blocknum block to write (in range 0 .. NUM_BLOCKS - 1)
	 * @param block    SuperBlock to write
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void write(int blocknum, SuperBlock block) {
		try {
			seek(blocknum);
			disk.writeInt(block.size);
			disk.writeInt(block.isize);
			disk.writeInt(block.msize);
			for(int i=0; i<block.freeMap.length; i++) {
				disk.writeByte(block.freeMap[i]);
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	/**
	 * Write an InodeBlock to disk.
	 *
	 * @param blocknum block to write (in range 0 .. NUM_BLOCKS - 1)
	 * @param block    InodeBlock to write
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void write(int blocknum, InodeBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.inodes.length; i++) {
				disk.writeInt(block.inodes[i].flags);
				disk.writeInt(block.inodes[i].owner);
				disk.writeInt(block.inodes[i].size);
				for(int j=0; j<13; j++) {
					disk.writeInt(block.inodes[i].ptr[j]);
				}
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	/**
	 * Write an IndirectBlock to disk.
	 *
	 * @param blocknum block to write (in range 0 .. NUM_BLOCKS - 1)
	 * @param block    InodeBlock to write
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void write(int blocknum, IndirectBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.ptr.length; i++) {
				disk.writeInt(block.ptr[i]);
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	/**
	 * Write a FreeMapBlock to disk.
	 * 
	 * @param blocknum block to write (in range 0 .. NUM_BLOCKS - 1)
	 * @param block    FreeMapBlock to write
	 * @throws RuntimeException if blocknum is out of range
	 */
	public void write(int blocknum, FreeMapBlock block) {
		try {
			seek(blocknum);
			for(int i=0; i<block.map.length; i++) {
				disk.write(block.map[i]);
			}
		} catch(IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	/**
	 * Stop the disk.
	 *
	 * @param removeFile true if you want to delete the disk file,
	 *                   false otherwise
	 */
	public void stop(boolean removeFile) {
		if(printStats)
			System.out.println(this);
		if(removeFile) {
			file.delete();
		}

		try{
			disk.close();
		} catch (IOException e){
			System.out.println("Disk couldn't be closed");
		}
	}

	/**
	 * Stop the disk.
	 */
	public void stop() {
		stop(true);
	}

	public String toString() {
		return NAME + "(reads: " + readCount + ", writes: " + writeCount + ")";
	}

	/**
	 * Seek the underlying disk file to the specified blocknum.
	 *
	 * @private
	 * @param blocknum Block number to which to seek the underlying file
	 * @throws IOException if there is a disk error
	 * @throws RuntimeException if blocknum is out of range
	 */
	private void seek(int blocknum) throws IOException {
		if(blocknum < 0 || blocknum >= NUM_BLOCKS) {
			throw new RuntimeException ("Attempt to read block " +
					blocknum + " is out of range");
		}
		disk.seek((long)(blocknum * BLOCK_SIZE));
	}
}
