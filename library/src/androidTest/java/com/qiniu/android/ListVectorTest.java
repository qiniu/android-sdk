package com.qiniu.android;

import com.qiniu.android.utils.ListVector;

import junit.framework.Assert;

import java.util.List;

public class ListVectorTest extends BaseTest {

    public void testVectorList() {

        final int count = 1000;
        final ListVector<String> v = new ListVector<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (v.size() < count) {
                    vectorAdd(v);
                }
            }
        }).start();

        while (v.size() < count) {
            vectorList(v);
        }

        Assert.assertTrue("v:" + v, v.size() == count);
    }

    public void testVectorSubList() {

        final int count = 1000;
        final ListVector<String> listVector = new ListVector<>();

        while (listVector.size() < count) {
            vectorAdd(listVector);
        }

        final ListVector<String> v = listVector.subList(0, count/2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (v.size() < count) {
                    vectorAdd(v);
                }
            }
        }).start();


        while (v.size() < count) {
            vectorList(v);
        }

        Assert.assertTrue("v:" + v, v.size() == count);
    }

    private void vectorAdd(List<String> v) {
        String e = v.size() + "";
        v.add(e);
        System.out.println("add e:" + e);
    }

    private void vectorList(ListVector<String> v) {
        final int size = v.size();
        v.enumerateObjects(new ListVector.EnumeratorHandler<String>() {
            @Override
            public boolean enumerate(String s) {
                System.out.println("size:" + size + " value:" + s);
                return false;
            }
        });
    }
}
