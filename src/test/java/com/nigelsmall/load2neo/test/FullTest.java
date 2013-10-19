package com.nigelsmall.load2neo.test;

import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;

public class FullTest {

    @Test
    public void run() throws IOException {
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("test.geoff");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            File file = new File(url.getPath());
            System.out.println(file.getName());
            FileReader reader = new FileReader(file);
            DocumentTest test = new DocumentTest(reader);
            test.run();
        }
    }

}
