package com.qiniu.android;

import java.io.Serializable;

/**
 * Created by jemy on 2019/9/5.
 */

public class TestCompany implements Serializable {
    String name;
    int age;

    public TestCompany(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
