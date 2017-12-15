package fileSystem;
/**
 * Determines the position from whence a seek operation should
 * start. In other words, when you seek by an offset, that offset is
 * relative to some position in the file. This enum defines names for
 * each of the three types of positions relative to which you can
 * seek. The three types are as follows:
 *
 * <dl>
 *   <dt>SEEK_SET</dt>
 *   <dd>Seek relative to the beginning of the file</dd>
 *   <dt>SEEK_CUR</dt>
 *   <dd>Seek relative to the current position in the file</dd>
 *   <dt>SEEK_END</dt>
 *   <dd>Seek relative to the end of the file</dd>
 * </dl>
 */
public enum Whence { SEEK_SET, SEEK_CUR, SEEK_END }
