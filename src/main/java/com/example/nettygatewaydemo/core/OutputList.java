package com.example.nettygatewaydemo.core;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.MathUtil;

import java.util.AbstractList;
import java.util.RandomAccess;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

final class OutputList extends AbstractList<Object> implements RandomAccess {

    private static final OutputList.CodecOutputListRecycler NOOP_RECYCLER = new OutputList.CodecOutputListRecycler() {
        @Override
        public void recycle(OutputList object) {
            // drop on the floor and let the GC handle it.
            System.out.println("recycle");
        }
    };

    private static final FastThreadLocal<OutputLists> CODEC_OUTPUT_LISTS_POOL =
            new FastThreadLocal<OutputLists>() {
                @Override
                protected OutputLists initialValue() throws Exception {
                    // 16 CodecOutputList per Thread are cached.
                    return new OutputLists(16);
                }
            };

    private interface CodecOutputListRecycler {
        void recycle(OutputList outputList);
    }

    private static final class OutputLists implements OutputList.CodecOutputListRecycler {
        private final OutputList[] elements;
        private final int mask;

        private int currentIdx;
        private int count;

        OutputLists(int numElements) {
            elements = new OutputList[MathUtil.safeFindNextPositivePowerOfTwo(numElements)];
            for (int i = 0; i < elements.length; ++i) {
                // Size of 16 should be good enough for the majority of all users as an initial capacity.
                elements[i] = new OutputList(this, 16);
            }
            count = elements.length;
            currentIdx = elements.length;
            mask = elements.length - 1;
        }

        public OutputList getOrCreate() {
            if (count == 0) {
                // Return a new CodecOutputList which will not be cached. We use a size of 4 to keep the overhead
                // low.
                return new OutputList(NOOP_RECYCLER, 4);
            }
            --count;

            int idx = (currentIdx - 1) & mask;
            OutputList list = elements[idx];
            currentIdx = idx;
            return list;
        }

        @Override
        public void recycle(OutputList outputList) {
            int idx = currentIdx;
            elements[idx] = outputList;
            currentIdx = (idx + 1) & mask;
            ++count;
            assert count <= elements.length;
        }
    }

    static OutputList newInstance() {
        return CODEC_OUTPUT_LISTS_POOL.get().getOrCreate();
    }

    private final OutputList.CodecOutputListRecycler recycler;
    private int size;
    private Object[] array;
    private boolean insertSinceRecycled;

    private OutputList(OutputList.CodecOutputListRecycler recycler, int size) {
        this.recycler = recycler;
        array = new Object[size];
    }

    @Override
    public Object get(int index) {
        checkIndex(index);
        return array[index];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(Object element) {
        checkNotNull(element, "element");
        try {
            insert(size, element);
        } catch (IndexOutOfBoundsException ignore) {
            // This should happen very infrequently so we just catch the exception and try again.
            expandArray();
            insert(size, element);
        }
        ++ size;
        return true;
    }

    @Override
    public Object set(int index, Object element) {
        checkNotNull(element, "element");
        checkIndex(index);

        Object old = array[index];
        insert(index, element);
        return old;
    }

    @Override
    public void add(int index, Object element) {
        checkNotNull(element, "element");
        checkIndex(index);

        if (size == array.length) {
            expandArray();
        }

        if (index != size) {
            System.arraycopy(array, index, array, index + 1, size - index);
        }

        insert(index, element);
        ++ size;
    }

    @Override
    public Object remove(int index) {
        checkIndex(index);
        Object old = array[index];

        int len = size - index - 1;
        if (len > 0) {
            System.arraycopy(array, index + 1, array, index, len);
        }
        array[-- size] = null;

        return old;
    }

    @Override
    public void clear() {
        // We only set the size to 0 and not null out the array. Null out the array will explicit requested by
        // calling recycle()
        size = 0;
    }

    /**
     * Returns {@code true} if any elements where added or set. This will be reset once {@link #recycle()} was called.
     */
    boolean insertSinceRecycled() {
        return insertSinceRecycled;
    }

    /**
     * Recycle the array which will clear it and null out all entries in the internal storage.
     */
    void recycle() {
        for (int i = 0 ; i < size; i ++) {
            array[i] = null;
        }
        size = 0;
        insertSinceRecycled = false;

        recycler.recycle(this);
    }

    /**
     * Returns the element on the given index. This operation will not do any range-checks and so is considered unsafe.
     */
    Object getUnsafe(int index) {
        return array[index];
    }

    private void checkIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("expected: index < ("
                    + size + "),but actual is (" + size + ")");
        }
    }

    private void insert(int index, Object element) {
        array[index] = element;
        insertSinceRecycled = true;
    }

    private void expandArray() {
        // double capacity
        int newCapacity = array.length << 1;

        if (newCapacity < 0) {
            throw new OutOfMemoryError();
        }

        Object[] newArray = new Object[newCapacity];
        System.arraycopy(array, 0, newArray, 0, array.length);

        array = newArray;
    }
}

