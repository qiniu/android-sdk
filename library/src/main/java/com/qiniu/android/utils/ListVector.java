package com.qiniu.android.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListVector<E> extends Vector<E> {

    public ListVector() {
        super();
    }

    public ListVector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    public synchronized void enumerateObjects(EnumeratorHandler<? super E> handler) {
        if (handler == null) {
            return;
        }

        final E[] elementData = (E[]) this.elementData;
        final int elementCount = this.elementCount;
        for (int i = 0; i < elementCount; i++) {
            if (handler.enumerate(elementData[i])) {
                break;
            }
        }
    }

    @Override
    public synchronized ListVector<E> subList(int fromIndex, int toIndex) {
        ListVector listVector = new ListVector<E>();
        // c.toArray might (incorrectly) not return Object[] (see 6260652)
        if (elementData.getClass() != Object[].class) {
            listVector.elementData = Arrays.copyOf(elementData, elementCount, Object[].class);
            listVector.elementCount = listVector.elementData.length;
        } else {
            listVector.elementData = Arrays.copyOf(elementData, elementCount);
            listVector.elementCount = elementCount;
        }
        return listVector;
    }

    public interface EnumeratorHandler<T> {
        boolean enumerate(T t);
    }
}
