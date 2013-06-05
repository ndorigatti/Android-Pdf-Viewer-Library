package net.sf.andpdf.nio;

import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * A wrapper for the java.nio.ByteBuffer class
 * 
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public final class NioByteBuffer extends ByteBuffer {

    /**
     * the underlying buffer
     */
    private java.nio.ByteBuffer nioBuf;

    @Override
	public byte[] array() {
        return nioBuf.array();
    }

    @Override
	public int arrayOffset() {
        return nioBuf.arrayOffset();
    }

    public CharBuffer asCharBuffer() {
        return nioBuf.asCharBuffer();
    }

    public DoubleBuffer asDoubleBuffer() {
        return nioBuf.asDoubleBuffer();
    }

    public FloatBuffer asFloatBuffer() {
        return nioBuf.asFloatBuffer();
    }

    public IntBuffer asIntBuffer() {
        return nioBuf.asIntBuffer();
    }

    public LongBuffer asLongBuffer() {
        return nioBuf.asLongBuffer();
    }

    public java.nio.ByteBuffer asReadOnlyBuffer() {
        return nioBuf.asReadOnlyBuffer();
    }

    public ShortBuffer asShortBuffer() {
        return nioBuf.asShortBuffer();
    }

    public int capacity() {
        return nioBuf.capacity();
    }

    public Buffer clear() {
        return nioBuf.clear();
    }

    public java.nio.ByteBuffer compact() {
        return nioBuf.compact();
    }

    public int compareTo(final java.nio.ByteBuffer otherBuffer) {
        return nioBuf.compareTo(otherBuffer);
    }

    @Override
	public NioByteBuffer duplicate() {
        return new NioByteBuffer(nioBuf.duplicate());
    }

    @Override
    public boolean equals(final Object other) {
        return nioBuf.equals(other);
    }

    @Override
	public void flip() {
        nioBuf.flip();
    }

    @Override
	public byte get() {
        return nioBuf.get();
    }

    @Override
	public void get(final byte[] dest, final int off, final int len) {
        nioBuf.get(dest, off, len);
    }

    @Override
	public void get(final byte[] dest) {
        nioBuf.get(dest);
    }

    @Override
	public byte get(final int index) {
        return nioBuf.get(index);
    }

    @Override
	public char getChar() {
        return nioBuf.getChar();
    }

    @Override
	public char getChar(final int index) {
        return nioBuf.getChar(index);
    }

    public double getDouble() {
        return nioBuf.getDouble();
    }

    public double getDouble(final int index) {
        return nioBuf.getDouble(index);
    }

    public float getFloat() {
        return nioBuf.getFloat();
    }

    public float getFloat(final int index) {
        return nioBuf.getFloat(index);
    }

    @Override
	public int getInt() {
        return nioBuf.getInt();
    }

    public int getInt(final int index) {
        return nioBuf.getInt(index);
    }

    @Override
	public long getLong() {
        return nioBuf.getLong();
    }

    public long getLong(final int index) {
        return nioBuf.getLong(index);
    }

    @Override
	public short getShort() {
        return nioBuf.getShort();
    }

    public short getShort(final int index) {
        return nioBuf.getShort(index);
    }

    @Override
	public boolean hasArray() {
        return nioBuf.hasArray();
    }

    @Override
    public int hashCode() {
        return nioBuf.hashCode();
    }

    @Override
	public boolean hasRemaining() {
        return nioBuf.hasRemaining();
    }

    public boolean isDirect() {
        return nioBuf.isDirect();
    }

    public boolean isReadOnly() {
        return nioBuf.isReadOnly();
    }

    @Override
	public int limit() {
        return nioBuf.limit();
    }

    @Override
	public void limit(final int newLimit) {
        nioBuf.limit(newLimit);
    }

    @Override
	public void mark() {
        nioBuf.mark();
    }

    public ByteOrder order() {
        return nioBuf.order();
    }

    public java.nio.ByteBuffer order(final ByteOrder byteOrder) {
        return nioBuf.order(byteOrder);
    }

    @Override
	public int position() {
        return nioBuf.position();
    }

    @Override
	public void position(final int newPosition) {
        nioBuf.position(newPosition);
    }

    @Override
	public void put(final byte b) {
        nioBuf.put(b);
    }

    public NioByteBuffer put(final byte[] src, final int off, final int len) {
        nioBuf.put(src, off, len);
        return this;
    }

    @Override
	public void put(final byte[] src) {
        nioBuf.put(src);
    }

    public ByteBuffer put(final java.nio.ByteBuffer src) {
        nioBuf.put(src);
        return this;
    }

    @Override
	public void put(final ByteBuffer src) {
        nioBuf.put(src.toNIO());
    }

    @Override
	public void put(final int index, final byte b) {
        nioBuf.put(index, b);
    }

    @Override
	public void putChar(final char value) {
        nioBuf.putChar(value);
    }

    public NioByteBuffer putChar(final int index, final char value) {
        nioBuf.putChar(index, value);
        return this;
    }

    public NioByteBuffer putDouble(final double value) {
        nioBuf.putDouble(value);
        return this;
    }

    public NioByteBuffer putDouble(final int index, final double value) {
        nioBuf.putDouble(index, value);
        return this;
    }

    public NioByteBuffer putFloat(final float value) {
        nioBuf.putFloat(value);
        return this;
    }

    public NioByteBuffer putFloat(final int index, final float value) {
        nioBuf.putFloat(index, value);
        return this;
    }

    @Override
	public void putInt(final int index, final int value) {
        nioBuf.putInt(index, value);
    }

    @Override
	public void putInt(final int value) {
        nioBuf.putInt(value);
    }

    public void putLong(final int index, final long value) {
        nioBuf.putLong(index, value);
    }

    @Override
	public void putLong(final long value) {
        nioBuf.putLong(value);
    }

    public void putShort(final int index, final short value) {
        nioBuf.putShort(index, value);
    }

    @Override
	public void putShort(final short value) {
        nioBuf.putShort(value);
    }

    @Override
	public int remaining() {
        return nioBuf.remaining();
    }

    @Override
	public void reset() {
        nioBuf.reset();
    }

    @Override
	public void rewind() {
        nioBuf.rewind();
    }

    @Override
	public NioByteBuffer slice() {
        return new NioByteBuffer(nioBuf.slice());
    }

    @Override
    public String toString() {
        return nioBuf.toString();
    }

    public NioByteBuffer(final java.nio.ByteBuffer nioBuf) {
        this.nioBuf = nioBuf;
    }

    @Override
	public java.nio.ByteBuffer toNIO() {
        return nioBuf;
    }

    public static NioByteBuffer fromNIO(final java.nio.ByteBuffer nioBuf) {
        return new NioByteBuffer(nioBuf);
    }

    public static NioByteBuffer allocate(final int i) {
        return new NioByteBuffer(java.nio.ByteBuffer.allocate(i));
    }

    public static NioByteBuffer wrap(final byte[] bytes) {
        return new NioByteBuffer(java.nio.ByteBuffer.wrap(bytes));
    }
}
