package com.facebook.presto.block.uncompressed;

import com.facebook.presto.Range;
import com.facebook.presto.Tuple;
import com.facebook.presto.TupleInfo;
import com.facebook.presto.block.Cursor;
import com.facebook.presto.slice.Slice;
import com.google.common.base.Preconditions;

import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

public class UncompressedBlockCursor
        implements Cursor
{
    private final TupleInfo tupleInfo;
    private final Slice slice;
    private final Range range;
    private long position = -1;
    private int offset = -1;
    private int size = -1;

    public UncompressedBlockCursor(UncompressedBlock block)
    {
        this(checkNotNull(block, "info is null").getTupleInfo(), checkNotNull(block, "block is null").getSlice(), block.getRange());
    }

    public UncompressedBlockCursor(TupleInfo tupleInfo, Slice slice, Range range)
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
            offset += size;
        }
        size = tupleInfo.size(slice, offset);
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

        if (newPosition > range.getEnd()) {
            position = newPosition;
            return false;
        }

        // move to initial position
        if (position < 0) {
            position = range.getStart();
            offset = 0;
            size = tupleInfo.size(slice, 0);
        }

        // advance to specified position
        while (position < newPosition) {
            position++;
            offset += size;
            size = tupleInfo.size(slice, offset);
        }

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
        return new Tuple(slice.slice(offset, size), tupleInfo);
    }

    @Override
    public long getLong(int field)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return tupleInfo.getLong(slice, offset, field);
    }

    @Override
    public double getDouble(int field)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return tupleInfo.getDouble(slice, offset, field);
    }

    @Override
    public Slice getSlice(int field)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        return tupleInfo.getSlice(slice, offset, field);
    }

    @Override
    public boolean currentTupleEquals(Tuple value)
    {
        Preconditions.checkState(position >= 0, "Need to call advanceNext() first");
        if (isFinished()) {
            throw new NoSuchElementException();
        }
        Slice tupleSlice = value.getTupleSlice();
        return slice.equals(offset, size, tupleSlice, 0, tupleSlice.length());
    }
}