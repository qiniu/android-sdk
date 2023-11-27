package com.qiniu.android.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ListVector
 *
 * @param <E> 元素类型
 */
public class ListVector<E> extends Vector<E> {

    /**
     * 构造函数
     */
    public ListVector() {
        super();
    }

    /**
     * 构造函数
     *
     * @param initialCapacity   initialCapacity
     * @param capacityIncrement capacityIncrement
     */
    public ListVector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    /**
     * 对象遍历
     *
     * @param handler handler
     */
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

    /**
     * create subList
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return subList
     */
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

    /**
     * EnumeratorHandler
     * @param <T> enumerate 对象的类型
     */
    public interface EnumeratorHandler<T> {
        boolean enumerate(T t);
    }
}
