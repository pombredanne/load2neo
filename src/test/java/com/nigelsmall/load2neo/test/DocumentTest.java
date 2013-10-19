package com.nigelsmall.load2neo.test;

import com.nigelsmall.geoff.reader.GeoffReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;

public class DocumentTest {

    final private Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    final GeoffReader geoffReader;

    public DocumentTest(Reader reader) throws IOException {
        this.geoffReader = new GeoffReader(reader);
    }

    public void run() throws IOException {
        while (this.geoffReader.hasMore()) {
            SubgraphTest test = new SubgraphTest(this.geoffReader.readSubgraph());
            test.run();
        }
    }

}
