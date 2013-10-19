package com.nigelsmall.load2neo.test;

import com.nigelsmall.geoff.reader.GeoffReader;
import com.nigelsmall.load2neo.AbstractNode;
import com.nigelsmall.load2neo.Subgraph;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class GeoffReaderTest {

    @Test
    public void testCanReadSingleNode() throws IOException {
        String text = "(alice)";
        Subgraph subgraph = new GeoffReader(text).readSubgraph();
        assert subgraph.order() == 1;
        assert subgraph.size() == 0;
        Map<String, AbstractNode> nodes = subgraph.getNodes();
        assert nodes.containsKey("alice");
    }

    @Test
    public void testCanReadSmallGraph() throws IOException {
        String text = "(alice)-[:KNOWS]->(bob)";
        Subgraph subgraph = new GeoffReader(text).readSubgraph();
        assert subgraph.order() == 2;
        assert subgraph.size() == 1;
        Map<String, AbstractNode> nodes = subgraph.getNodes();
        assert nodes.containsKey("alice");
        assert nodes.containsKey("bob");
    }

}
