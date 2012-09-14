package com.facebook.presto.block.uncompressed;

import com.facebook.presto.Range;
import com.facebook.presto.Tuple;
import com.facebook.presto.TupleInfo;
import com.facebook.presto.block.Cursor;
import com.facebook.presto.slice.Slice;
import com.google.common.base.Preconditions;

import java.util.NoSuchElementException;

import static com.facebook.presto.SizeOf.SIZE_OF_LONG;
import static com.facebook.presto.TupleInfo.Type.FIXED_INT_64;
import static com.google.common.base.Preconditions.checkNotNull;

public class UncompressedLongBlockCursor
        implements Cursor
{
    private static final TupleInfo INFO = new TupleInfo(FIXED_INT_64);

    private final TupleInfo tupleInfo;
    private final Slice slice;
    private final Range range;
    private long position = -1;
    private int offset = -1;

    public UncompressedLongBlockCursor(UncompressedBlock block)
    {
        this(checkNotNull(block, "block is null").getTupleInfo(), block.getSlice(), block.getRange());
    }

    public UncompressedLongBlockCursor(TupleInfo tupleInfo, Slice slice, Range range)
    {
        this.tupleInfo = tupleInfo;
        this.slice = slice;
        this.range = range;
    }

    @Override
    public TupleInfo getTupleInfo()
    {
        return tupleInfo;
    }

    @Override
    public Range getRange()
    {
        return range;
    }

    @Override
    public boolean isFinished()
    {
        return position > range.getEnd();
    }

    @Override
    public boolean advanceNextValue()
    {
        if (position >= range.getEnd()) {
            return false;
        }

        if (position < 0) {
            position = range.getStart();
            offset = 0;
        } else {
            position++;
            offset += SIZE_OF_LONG;
        }
        return true;
    }

    @Override
    public boolean advanceNextPosition()
    {
        // every position is a new value
        return advanceNextValue();
    }

    @Override
    public boolean advanceToPosition(long newPosition)
    {
        Preconditions.checkArgument(newPosition >= this.position, "Can't advance backwards");

        // advance to specified position
        position = newPosition;

        // if new position is out of range, return false
        if (newPosition > range.getEnd()) {
            return false;
        }

        // adjust offset
        offset = (int) ((position - this.range.getStart()) * SIZE_OF_LONG);
        return true;
    }

    @Override
    public long getPosition()
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return position;
    }

    @Override
    public long getCurrentValueEndPosition()
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return position;
    }

    @Override
    public Tuple getTuple()
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return new Tuple(slice.slice(offset, SIZE_OF_LONG), INFO);
    }

    @Override
    public long getLong(int field)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        Preconditions.checkElementIndex(0, 1, "field");
        return slice.getLong(offset);
    }

    @Override
    public double getDouble(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Slice getSlice(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean currentTupleEquals(Tuple value)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        Slice tupleSlice = value.getTupleSlice();
        return tupleSlice.length() == SIZE_OF_LONG && slice.getLong(offset) == tupleSlice.getLong(0);
    }
}