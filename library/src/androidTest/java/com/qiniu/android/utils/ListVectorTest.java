package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ListVectorTest extends BaseTest {

    @Test
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

    @Test
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
