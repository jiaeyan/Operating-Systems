package test;

import fileSystem.Bitwise;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Test the interface of Bitwise. Note that toString uses isset, so
 * testing the toString and set methods is pretty good coverage.
 */
public class TestBitwise {
    @Test
    public void testToString() {
        assertEquals("11111111", Bitwise.toString((byte)0xff));
        assertEquals("00000000", Bitwise.toString((byte)0x00));
        assertEquals("00000001", Bitwise.toString((byte)0x01));
        assertEquals("00010000", Bitwise.toString((byte)0x10));
        assertEquals("10000000", Bitwise.toString((byte)0x80));
        assertEquals("10001000", Bitwise.toString((byte)0x88));
    }

    @Test
    public void testMultiByteToString() {
        byte bytes1[] = {(byte)0x00, (byte)0xff};
        assertEquals("00000000 11111111", Bitwise.toString(bytes1, " "));
        byte bytes2[] = {(byte)0x88, (byte)0x01};
        assertEquals("10001000 00000001", Bitwise.toString(bytes2, " "));
        byte bytes3[] = {(byte)0x88, (byte)0x01, (byte)0x88, (byte)0x10};
        assertEquals("10001000 00000001 10001000 00010000", Bitwise.toString(bytes3, " "));
        byte bytes4[] = {(byte)0x88};
        assertEquals("10001000", Bitwise.toString(bytes4, " "));
    }
    
    @Test
    public void testIsSet() {
    	byte b = (byte)0x00;
    	assertEquals(false, Bitwise.isset(0, b));
    	b = (byte)0x10;
    	assertEquals(false, Bitwise.isset(1, b));
    	assertEquals(true, Bitwise.isset(4, b));
    	b = (byte) 0x88;
    	assertEquals(false, Bitwise.isset(4, b));
    	assertEquals(true, Bitwise.isset(3, b));
    }
    
    @Test
    public void testMultIsSet() {
    	byte bytes1[] = {(byte)0x88, (byte)0x01};
    	assertEquals(true, Bitwise.isset(0, bytes1));
    	assertEquals(false, Bitwise.isset(8, bytes1));
    	assertEquals(true, Bitwise.isset(15, bytes1));
    	byte bytes2[] = {(byte)0x88, (byte)0x01, (byte)0x88, (byte)0x10};
    	assertEquals(false, Bitwise.isset(1, bytes2));
    	assertEquals(false, Bitwise.isset(14, bytes2));
    	assertEquals(true, Bitwise.isset(15, bytes2));
    	assertEquals(true, Bitwise.isset(16, bytes2));
    	assertEquals(false, Bitwise.isset(17, bytes2));
    }

    @Test
    public void testSet() {
        byte b = 0x00;
        b = Bitwise.set(0, b);
        assertEquals("00000001", Bitwise.toString(b));
        b = Bitwise.set(1, b);
        assertEquals("00000011", Bitwise.toString(b));
        b = Bitwise.set(1, b);
        assertEquals("00000011", Bitwise.toString(b));
        b = Bitwise.set(0, b);
        assertEquals("00000011", Bitwise.toString(b));
        b = Bitwise.set(5, b);
        assertEquals("00100011", Bitwise.toString(b));
    }

    @Test
    public void testMultiByteSet() {
        byte bytes[] = {(byte)0x00, (byte)0x00};
        Bitwise.set(8, bytes);
        assertEquals("00000001 00000000", Bitwise.toString(bytes, " "));
        Bitwise.set(7, bytes);
        Bitwise.set(0, bytes);
        assertEquals("00000001 10000001", Bitwise.toString(bytes, " "));
        Bitwise.set(15, bytes);
        assertEquals("10000001 10000001", Bitwise.toString(bytes, " "));
        Bitwise.set(8, bytes);
        assertEquals("10000001 10000001", Bitwise.toString(bytes, " "));
    }
    
    @Test
    public void testClear() {
    	byte b = (byte) 0xff;
        assertEquals("11111111", Bitwise.toString(b));
        b = Bitwise.clear(0, b);
        assertEquals("11111110", Bitwise.toString(b));
        b = Bitwise.clear(7, b);
        assertEquals("01111110", Bitwise.toString(b));
        b = Bitwise.clear(7, b);
        assertEquals("01111110", Bitwise.toString(b));
        b = Bitwise.clear(0, b);
        assertEquals("01111110", Bitwise.toString(b));
        b = Bitwise.clear(5, b);
        assertEquals("01011110", Bitwise.toString(b));
    }
    
    @Test
    public void testMultiClear() {
        byte bytes[] = {(byte)0xff, (byte)0x01};
        assertEquals("11111111 00000001", Bitwise.toString(bytes, " "));
        Boolean wasClear = Bitwise.clear(15, bytes);
        assertEquals(true, wasClear);
        assertEquals("01111111 00000001", Bitwise.toString(bytes, " "));
        wasClear = Bitwise.clear(15, bytes);
        assertEquals(false, wasClear);
        assertEquals("01111111 00000001", Bitwise.toString(bytes, " "));
        wasClear = Bitwise.clear(14, bytes);
        assertEquals(true, wasClear);
        assertEquals("00111111 00000001", Bitwise.toString(bytes, " "));
        wasClear = Bitwise.clear(7, bytes);
        assertEquals(false, wasClear);
        assertEquals("00111111 00000001", Bitwise.toString(bytes, " "));
        wasClear = Bitwise.clear(0, bytes);
        assertEquals(true, wasClear);
        assertEquals("00111111 00000000", Bitwise.toString(bytes, " "));
    }

    @Test
    public void testClearAll() {
        byte bytes[] = {(byte)0xff, (byte)0x01};
        assertEquals("11111111 00000001", Bitwise.toString(bytes, " "));
        Bitwise.clearAll(bytes);
        assertEquals("00000000 00000000", Bitwise.toString(bytes, " "));
    }
}
