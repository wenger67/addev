package com.vinson.addev;

import com.github.javafaker.Faker;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test_fake() {
        float ret = (float) Faker.instance().number().randomDouble(5, -2, 2);
        System.out.println(ret);
    }
}