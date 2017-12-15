package test;

import java.io.*;
import java.util.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.experimental.runners.Enclosed;

import fileSystem.Disk;
import fileSystem.FileTable;
import fileSystem.IndirectBlock;
import fileSystem.Inode;
import fileSystem.InodeBlock;
import fileSystem.MyFileSystem;
import fileSystem.Whence;
import static org.junit.Assert.*;

/**
 * Test MyFileSystem, including some internal implementation details
 * related to how SuperBlock and FreeMap work together.
 *
 * Normally, when you write a test suite, you would try to test
 * primarily the public interface. However, in this case it makes
 * sense to ensure that the file system is working correctly by
 * testing its internal state as well as its interface.
 *
 * {@internal We can access disk, freeMap, etc. from MyFileSystem
 * because they are declared package-local instead of private.}
 */
@RunWith(Enclosed.class)
public class TestMyFileSystem {
    public static void deleteDisk() {
        new File("DISK").delete();
    }

    @BeforeClass
    public static void init() {
        Disk.printStats = false;
    }

    public static class Format {
        private MyFileSystem fs;

        @BeforeClass
        public static void startFresh() {
            deleteDisk();
        }

        @Before
        public void setUp() throws IOException {
            fs = new MyFileSystem();
            fs.formatDisk(100, 5);
        }
        
        @After
        public void tearDown() throws IOException {
            fs.shutdown();
        }

        private void testSuperBlock() {
            assertEquals(100, fs.superBlock.size); // size in # of blocks
            assertEquals(0, fs.superBlock.msize);   // map fits in superblock
            assertEquals(5, fs.superBlock.isize);   // 5 inode blocks
            assertEquals(94, fs.superBlock.dsize());  // size - isize - super
        }
        
        @Test
        public void testFormatDisk() throws IOException {
            testSuperBlock();
        }
    }

    public static class FreeMap {
        private MyFileSystem fs;
        
        @Before
        public void setUp() throws IOException {
            deleteDisk();
            fs = new MyFileSystem();
            fs.formatDisk(100, 5);
        }
        
        @After
        public void tearDown() throws IOException {
            fs.shutdown();
        }
        
        @Test
        public void testFreeMapFind() {
            assertEquals(6, fs.freeMap.find());     // after super and inodes
            assertEquals(7, fs.freeMap.find());     // 6th was just reserved
            fs.freeMap.clear(6);                    // 6th block made free again
            assertEquals(6, fs.freeMap.find());     // because 6th was made free
            assertEquals(8, fs.freeMap.find());     // 7th was still reserved
            for(int i = 9; i < 100; ++i)            // reserve rest of blocks
                assertEquals(i, fs.freeMap.find()); // finding in order
            assertEquals(0, fs.freeMap.find());     // no more free blocks
            fs.freeMap.clear(42);                   // clear another block
            assertEquals(42, fs.freeMap.find());    // block we just cleared
        }
    }

    public static class FileSystemInterface {
        private MyFileSystem fs;
        
        @Before
        public void setUp() throws IOException {
            deleteDisk();
            fs = new MyFileSystem();
            fs.formatDisk(100, 2);
        }
        
        @After
        public void tearDown() throws IOException {
            fs.shutdown();
        }

        @Test
        public void testFileTable() {
            // Ensure that we can allocate and add MAX_FILES file descriptors
            for(int fd = 0; fd < FileTable.MAX_FILES; ++fd) {
                assertEquals(fd, fs.fileTable.allocate());
                assertEquals(0, fs.fileTable.add(new Inode(), fd + 1, fd));
            }
            // Ensure that -1 is returned when file table is full
            assertEquals(-1, fs.fileTable.allocate());

            // To avoid errors when shutting down, free everything
            // we allocated (since we didn't actually make any real
            // files that can be closed)
            for(int fd = 0; fd < FileTable.MAX_FILES; ++fd)
                fs.fileTable.free(fd);
        }

        @Test
        public void testCreate() throws IOException {
            int maxInumber = 2 * InodeBlock.COUNT;
            // assume maxInumber < FileTable.MAX_FILES
            for(int inumber = 1; inumber <= maxInumber; ++inumber) {
                int fd = fs.create();
                assertEquals(inumber, fs.fileTable.getInumber(fd));
            }
            assertEquals(-1, fs.create());
        }

        @Test
        public void testOpen() throws IOException {
            int fd = fs.create();
            assertEquals(0, fd);
            assertEquals(1, fs.fileTable.getInumber(fd));
            fs.shutdown();
            fs = new MyFileSystem();
            fd = fs.open(1);
            assertEquals(0, fd);
            assertEquals(1, fs.fileTable.getInumber(fd));
            assertEquals(-1, fs.open(2));
        }

        @Test
        public void testInumber() throws IOException {
            int fd1 = fs.create();
            int fd2 = fs.create();
            assertEquals(2, fs.fileTable.getInumber(fd2));
            assertEquals(2, fs.inumber(fd2));
            assertEquals(1, fs.fileTable.getInumber(fd1));
            assertEquals(1, fs.inumber(fd1));
        }

        @Test
        public void testSeek() throws IOException {
            int fd = fs.create();
            assertEquals(0,  fs.fileTable.getSeekPointer(fd));
            assertEquals(10, fs.seek(fd, 10, Whence.SEEK_SET));
            assertEquals(10, fs.seek(fd, 10, Whence.SEEK_END));
            assertEquals(20, fs.seek(fd, 10, Whence.SEEK_CUR));
            assertEquals(0,  fs.seek(fd, 0,  Whence.SEEK_SET));

            // Hack to test SEEK_END with non-zero file size before we
            // test write.
            //
            fs.fileTable.getInode(fd).size = 42;
            assertEquals(50, fs.seek(fd, 8, Whence.SEEK_END));
            assertEquals(50, fs.seek(fd, 0, Whence.SEEK_CUR));
            assertEquals(50, fs.fileTable.getSeekPointer(fd));
        }

        @Test
        public void testClose() throws IOException {
            int fd = fs.create();
            assertEquals(0, fd);
            assertEquals(1, fs.fileTable.getInumber(fd));

            // Change size of file and write it out with close
            fs.fileTable.getInode(fd).size = 100;
            assertEquals(0, fs.close(fd)); // close should be successful

            // After closing the file, the fd should be reused
            fd = fs.create();
            assertEquals(1, fs.create());

            // Re-open the file, the size should still be 100
            fd = fs.open(1);
            assertEquals(2, fd);
            assertEquals(100, fs.fileTable.getInode(fd).size);
        }

        @Test
        public void testDelete() throws IOException {
            int fd = fs.create();
            int inumber = fs.inumber(fd);
            assertEquals(-1, fs.delete(inumber));
            assertEquals(0, fs.close(fd));
            fd = fs.open(inumber);
            assertEquals(0, fd);
            assertEquals(0, fs.close(fd));
            assertEquals(0, fs.delete(inumber));
            assertEquals(-1, fs.open(inumber));
        }

        private void testFileSize(int fd, int expectedSize) {
            assertEquals(expectedSize, fs.fileTable.getInode(fd).size);
        }

        @Test
        public void testWrite() throws IOException {
            int fd = fs.create();

            // Write 3 bytes and make sure space is allocated and
            // size is adjusted
            byte[] foo = { 'f', 'o', 'o' };
            assertEquals(3, fs.write(fd, foo));
            assertEquals(3, fs.seek(fd, 0, Whence.SEEK_CUR));
            assertEquals(4, fs.freeMap.find()); // block 3 should be used
            fs.freeMap.clear(4);
            testFileSize(fd, 3);

            // Write 4 blocks worth of data and check again
            byte[] buf = new byte[Disk.BLOCK_SIZE * 4];
            Arrays.fill(buf, (byte)'a');
            assertEquals(Disk.BLOCK_SIZE * 4, fs.write(fd, buf));
            assertEquals(8, fs.freeMap.find()); // used 4 more blocks
            testFileSize(fd, 3 + Disk.BLOCK_SIZE * 4);

            // Write 3 more bytes, introducing a hole
            byte[] bar = { 'b', 'a', 'r' };
            fs.seek(fd, Disk.BLOCK_SIZE * 2, Whence.SEEK_CUR); // hole
            assertEquals(Disk.BLOCK_SIZE * 6 + 3,
                         fs.seek(fd, 0, Whence.SEEK_CUR));
            assertEquals(3, fs.write(fd, bar));
            testFileSize(fd, Disk.BLOCK_SIZE * 6 + 6);

            // We introduced a hole, so even though the file is now
            // Disk.BLOCK_SIZE * 6 + 6 bytes long, using up what would
            // appear to be a total of 7 blocks (0 -- 6), in fact one
            // of those blocks is a hole. So, when we allocate a free
            // block, we should actually get the 7th block after the
            // first data block (and there are 3 metadata blocks, so
            // 7 + 3).
            assertEquals(7 + 3, fs.freeMap.find());
        }

        @Test
        public void testRead() throws IOException {
            int fd = fs.create();

            // First write some data in
            byte[] foo = { 'f', 'o', 'o' };
            assertEquals(3, fs.write(fd, foo));

            // Set seek pointer to 0 and make sure we can read what we wrote
            assertEquals(0, fs.seek(fd, 0, Whence.SEEK_SET));
            byte[] buf = new byte[foo.length];
            assertEquals(3, fs.read(fd, buf));
            assertTrue(Arrays.equals(foo, buf));
            assertEquals(3, fs.seek(fd, 0, Whence.SEEK_CUR));

            // Make sure that we read less if our buffer is smaller
            // than our file
            buf = new byte[1];
            fs.seek(fd, 0, Whence.SEEK_SET);
            assertEquals(1, fs.read(fd, buf));

            // Make sure that we read less if our file is smaller than
            // our buffer
            buf = new byte[10000];
            fs.seek(fd, 0, Whence.SEEK_SET);
            assertEquals(3, fs.read(fd, buf));
            
            // Seek beyond end of file and read; should return 0
            assertEquals(6, fs.seek(fd, 6, Whence.SEEK_SET));
            buf[0] = 'a'; buf[1] = 'b'; buf[2] = 'c';
            assertEquals(0, fs.read(fd, buf));
            assertEquals(6, fs.seek(fd, 0, Whence.SEEK_CUR));
            assertEquals(buf[0], 'a');
            assertEquals(buf[1], 'b');
            assertEquals(buf[2], 'c');

            // Introduce a hole
            byte[] bar = { 'b', 'a', 'r' };
            fs.seek(fd, Disk.BLOCK_SIZE * 2, Whence.SEEK_SET);
            assertEquals(3, fs.write(fd, bar));
            testFileSize(fd, Disk.BLOCK_SIZE * 2 + bar.length);

            // Read the file and check its contents
            buf = new byte[Disk.BLOCK_SIZE * 2 + bar.length];
            fs.seek(fd, 0, Whence.SEEK_SET);
            assertEquals(buf.length, fs.read(fd, buf));
            assertEquals('f', buf[0]);
            assertEquals('o', buf[1]);
            assertEquals('o', buf[2]);
            int i;
            for(i = foo.length; i < buf.length - bar.length; ++i)
                assertEquals('\0', buf[i]);
            assertEquals('b', buf[i + 0]);
            assertEquals('a', buf[i + 1]);
            assertEquals('r', buf[i + 2]);
        }

        @Test
        public void testOverwrite() throws IOException {
            int fd = fs.create();

            // Write to a block, then write to a different place in
            // that block, and make sure that the first write was not
            // overwritten with zeros (makes sure that non-fresh
            // blocks are read-before-updated).
            byte[] foo = { 'f', 'o', 'o' };
            byte[] bar = { 'b', 'a', 'r' };
            assertEquals(3, fs.write(fd, foo));
            assertEquals(6, fs.seek(fd, 3, Whence.SEEK_CUR));
            assertEquals(3, fs.write(fd, bar));
            testFileSize(fd, 9);
            byte[] buf = new byte[9];
            assertEquals(0, fs.seek(fd, 0, Whence.SEEK_SET));
            assertEquals(9, fs.read(fd, buf));
            assertEquals('f',  buf[0]);
            assertEquals('o',  buf[1]);
            assertEquals('o',  buf[2]);
            assertEquals('\0', buf[3]);
            assertEquals('\0', buf[4]);
            assertEquals('\0', buf[5]);
            assertEquals('b',  buf[6]);
            assertEquals('a',  buf[7]);
            assertEquals('r',  buf[8]);
        }
    }
    
    public static class Indirection {
        private MyFileSystem fs;
        
        @Before
        public void setUp() throws IOException {
            deleteDisk();
            fs = new MyFileSystem();
        }
        
        @After
        public void tearDown() throws IOException {
            fs.shutdown();
        }
             
        @Test
        public void testSingleIndirect() throws IOException {
        	fs.formatDisk(100, 2);
            int fd = fs.create();
            
            byte[] buf = new byte[Disk.BLOCK_SIZE * (10+3)];
            Arrays.fill(buf, (byte)'a');
            assertEquals(Disk.BLOCK_SIZE*(10+3), fs.write(fd, buf));

            //will read two blocks requiring single indirection
            byte[] foo = new byte[Disk.BLOCK_SIZE*2];
            assertEquals(Disk.BLOCK_SIZE*(9), fs.seek(fd, Disk.BLOCK_SIZE*(9), Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            byte[] bar = new byte[Disk.BLOCK_SIZE*2];
            Arrays.fill(bar, (byte)'a');
            assertTrue(Arrays.equals(bar, foo));
        }
        
        @Test
        public void testHoleInSinglelIndirect() throws IOException {
        	fs.formatDisk(100, 2);
            int fd = fs.create();
            
            assertEquals(Disk.BLOCK_SIZE*(10+2), fs.seek(fd, Disk.BLOCK_SIZE*(10+2), Whence.SEEK_SET)); // hole
            
            byte[] buf = new byte[Disk.BLOCK_SIZE];
            Arrays.fill(buf, (byte)'a');
            assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));

            //will read two blocks that belong in a hole requiring single indirection
            byte[] foo = new byte[Disk.BLOCK_SIZE*2];
            assertEquals(Disk.BLOCK_SIZE*(9), fs.seek(fd, Disk.BLOCK_SIZE*(9), Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            byte[] bar = new byte[Disk.BLOCK_SIZE*2];
            Arrays.fill(bar, (byte)'\0');
            assertTrue(Arrays.equals(bar, foo));
        }
        
        @Test
        public void testSinglelIndirectWithFullFreeMap() throws IOException {
        	fs.formatDisk(11, 2);
        	int fd = fs.create();
        	int inumber = fs.inumber(fd);
            
            byte[] foo = new byte[Disk.BLOCK_SIZE * 10];
            Arrays.fill(foo, (byte)'a');
            assertEquals(-1, fs.write(fd, foo));
            fs.close(fd);
            fs.delete(inumber);
            
        	fs.formatDisk(15, 2);
        	fd = fs.create();
         
            byte[] buf = new byte[Disk.BLOCK_SIZE * 12];
            Arrays.fill(buf, (byte)'a');
            assertEquals(-1, fs.write(fd, buf));
        }
        
        @Test
        public void testSingleIndirectionComplex() throws IOException {
        	fs.formatDisk(100, 10);
        	
        	//create first file, write 10000 bytes
        	int fd1 = fs.create();
        	int inumber1 = fs.inumber(fd1);
        	byte[] w1 = new byte[10000];
            Arrays.fill(w1, (byte)'1');
            assertEquals(10000, fs.write(fd1, w1));
            
            //create second file, seek 1000 bytes, write 1000 bytes
            int fd2 = fs.create();
        	int inumber2 = fs.inumber(fd2);
            fs.seek(fd2, 1000, Whence.SEEK_SET);
	       	byte[] w2 = new byte[1000];
            Arrays.fill(w2, (byte)'2');
            assertEquals(1000, fs.write(fd2, w2));
        	
            //read 50 bytes from first file at offset 9990
            fs.seek(fd1, -10, Whence.SEEK_CUR);
            byte[] r1 = new byte[50];
            assertEquals(10, fs.read(fd1, r1));
            assertEquals((byte)'1', r1[9]);
            
            //read 50 bytes from second file at offset 990
            fs.seek(fd2, -1010, Whence.SEEK_END);
            byte[] r2 = new byte[50];
            assertEquals(50, fs.read(fd2, r2));
            assertEquals((byte)'\0', r2[0]);
            assertEquals((byte)'2', r2[10]);
            
            //close first file
            fs.close(fd1);
            
            //create third file, write 1000 bytes, seek 10, write 1000 bytes
            int fd3 = fs.create();
            byte[] w3 = new byte[1000];
            Arrays.fill(w3, (byte)'3');
            assertEquals(1000, fs.write(fd3, w3));
            fs.seek(fd3, 10, Whence.SEEK_CUR);
            assertEquals(1000, fs.write(fd3, w3));
            
            //read 30 bytes from third file at offset 990
            fs.seek(fd1, 990, Whence.SEEK_SET);
            byte[] r3 = new byte[30];
            assertEquals(30, fs.read(fd3, r3));
            assertEquals((byte)'3', r3[0]);
            assertEquals((byte)'\0', r3[10]);
            assertEquals((byte)'3', r3[20]);
            
            //open first file back into the system and read 50 bytes
            fd1 = fs.open(inumber1);
            byte[] r4 = new byte[50];
            assertEquals(50, fs.read(fd1, r4));
            assertEquals((byte)'1', r4[0]);
            assertEquals((byte)'1', r4[49]);
            
            //close second file
            fs.close(fd2);
            //delete second file
            fs.delete(inumber2);
            
            //open second file (should give an error)
            assertEquals(-1, fs.open(inumber2));
        }
        
        @Test
        public void testNoSpaceForIndirectBlock() throws IOException {
        	fs.formatDisk(100, 10);
        	
        	int fd1 = fs.create();
        	byte[] buf = new byte[43 * Disk.BLOCK_SIZE];
        	Arrays.fill(buf, (byte)'A');
        	assertEquals(43 * Disk.BLOCK_SIZE, fs.write(fd1, buf));
        	
        	int fd2 = fs.create();
        	Arrays.fill(buf, (byte)'B');
        	assertEquals(43 * Disk.BLOCK_SIZE, fs.write(fd2, buf));
        	
        	//At this point, we should have 1 block left (remember the superblock)
        	int fd3 = fs.create();
        	byte[] foo = new byte[Disk.BLOCK_SIZE];
        	Arrays.fill(buf, (byte)'C');
        	assertEquals(130*Disk.BLOCK_SIZE, fs.seek(fd3, 130*Disk.BLOCK_SIZE, Whence.SEEK_SET));
        	assertEquals(-1, fs.write(fd3, foo));        	
        }
        
        @Test
        public void testDoubleIndirect() throws IOException {
        	fs.formatDisk(500, 2);
            int fd = fs.create();
            
            int size = (10 + IndirectBlock.COUNT + IndirectBlock.COUNT / 2) * Disk.BLOCK_SIZE; 
            byte[] buf = new byte[size];
            Arrays.fill(buf, (byte)'a');
            assertEquals(size, fs.write(fd, buf));
            
            //will read two blocks requiring double indirection
            byte[] foo = new byte[Disk.BLOCK_SIZE*2];
            int seekAt = (10 + IndirectBlock.COUNT - 1) * Disk.BLOCK_SIZE;
            assertEquals(seekAt, fs.seek(fd, seekAt, Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            byte[] bar = new byte[Disk.BLOCK_SIZE*2];
            Arrays.fill(bar, (byte)'a');
            assertTrue(Arrays.equals(bar, foo));
            
        }        
        
        @Test
        public void testTripleIndirect() throws IOException {
        	fs.formatDisk(20000, 2);
            int fd = fs.create();
            
            int size = (10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT + 10) * Disk.BLOCK_SIZE; 
            byte[] buf = new byte[size];
            Arrays.fill(buf, (byte)'a');
            assertEquals(size, fs.write(fd, buf));
            
            //will read two blocks requiring double indirection
            byte[] foo = new byte[Disk.BLOCK_SIZE*2];
            int seekAt = (10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT - 1) * Disk.BLOCK_SIZE;
            assertEquals(seekAt, fs.seek(fd, seekAt, Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            byte[] bar = new byte[Disk.BLOCK_SIZE*2];
            Arrays.fill(bar, (byte)'a');
            assertTrue(Arrays.equals(bar, foo));
        }
    
        @Test
        public void testHoleInDoubleAndTripleIndirect() throws IOException {
        	fs.formatDisk(20000, 2);
            int fd = fs.create();
            assertEquals(Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + 2), fs.seek(fd, Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + 2), Whence.SEEK_SET)); // hole
            
            byte[] buf = new byte[Disk.BLOCK_SIZE];
            Arrays.fill(buf, (byte)'a');
            assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));

            //will read two blocks that belong in a hole requiring double indirection
            byte[] foo = new byte[Disk.BLOCK_SIZE*2];
            assertEquals(Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT -1), fs.seek(fd, Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT -1), Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            byte[] bar = new byte[Disk.BLOCK_SIZE*2];
            Arrays.fill(bar, (byte)'\0');
            assertTrue(Arrays.equals(bar, foo));
            
            fd = fs.create();
            assertEquals(Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT + 2), 
            		fs.seek(fd, Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT + 2), Whence.SEEK_SET)); // hole
            
            assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));

            //will read two blocks that belong in a hole requiring triple indirection
            assertEquals(Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT -1), 
            		fs.seek(fd, Disk.BLOCK_SIZE*(10 + IndirectBlock.COUNT + IndirectBlock.COUNT * IndirectBlock.COUNT -1), Whence.SEEK_SET));
            assertEquals(Disk.BLOCK_SIZE*2, fs.read(fd, foo));

            assertTrue(Arrays.equals(bar, foo));
        }
        
        @Test
        public void testDoubleAndTripleIndirectWithFullFreeMap() throws IOException {
        	fs.formatDisk(150, 2);
        	int fd = fs.create();
        	int inumber = fs.inumber(fd);
            
            byte[] foo = new byte[Disk.BLOCK_SIZE * 151];
            Arrays.fill(foo, (byte)'a');
            assertEquals(-1, fs.write(fd, foo));
            fs.close(fd);
            fs.delete(inumber);
            
        	fs.formatDisk(17000, 2);
        	fd = fs.create();
         
            byte[] buf = new byte[Disk.BLOCK_SIZE * 17001];
            Arrays.fill(buf, (byte)'a');
            assertEquals(-1, fs.write(fd, buf));
        }
        
        @Test
        public void testIndirection() throws IOException {
        	fs.formatDisk(20000, 2);
        	int fd = fs.create();
        	
        	//write data to various blocks
        	byte[] buf = new byte[Disk.BLOCK_SIZE];
        	Arrays.fill(buf, (byte)'A');
        	assertEquals(4608, fs.seek(fd, 4608, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));

        	Arrays.fill(buf, (byte)'B');
        	assertEquals(5120, fs.seek(fd, 5120, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));
        	
        	Arrays.fill(buf, (byte)'C');
        	assertEquals(70144, fs.seek(fd, 70144, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));
        	
        	Arrays.fill(buf, (byte)'D');
        	assertEquals(70656, fs.seek(fd, 70656, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));
        	
        	Arrays.fill(buf, (byte)'E');
        	assertEquals(8458752, fs.seek(fd, 8458752, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));
        	
        	Arrays.fill(buf, (byte)'F');
        	assertEquals(8459264, fs.seek(fd, 8459264, Whence.SEEK_SET));
        	assertEquals(Disk.BLOCK_SIZE, fs.write(fd, buf));
        	
        	// Read back data overlapping block boundaries
        	byte[] foo = new byte[20];
        	assertEquals(5110, fs.seek(fd, 5110, Whence.SEEK_SET));
        	assertEquals(20, fs.read(fd, foo));
        	assertEquals((byte)'A', foo[9]);
        	assertEquals((byte)'B', foo[10]);
        	
        	assertEquals(70646, fs.seek(fd, 70646, Whence.SEEK_SET));
        	assertEquals(20, fs.read(fd, foo));
        	assertEquals((byte)'C', foo[9]);
        	assertEquals((byte)'D', foo[10]);
        	
        	assertEquals(8459254, fs.seek(fd, 8459254, Whence.SEEK_SET));
        	assertEquals(20, fs.read(fd, foo));
        	assertEquals((byte)'E', foo[9]);
        	assertEquals((byte)'F', foo[10]);
  
        }
    }
}
