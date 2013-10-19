package com.nigelsmall.load2neo.test;

import com.nigelsmall.geoff.reader.GeoffReader;
import com.nigelsmall.load2neo.AbstractNode;
import com.nigelsmall.load2neo.Subgraph;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class SimpleGeoffReaderTest {

    private Subgraph testSubgraph(String geoff, int order, int size) throws IOException {
        System.out.println(geoff);
        Subgraph subgraph = new GeoffReader(geoff).readSubgraph();
        assertSubgraph(subgraph, order, size);
        return subgraph;
    }

    private void assertSubgraph(Subgraph subgraph, int order, int size) {
        assert subgraph.order() == order;
        assert subgraph.size() == size;
    }

    private void assertNode(AbstractNode node, int labelCount, int propertyCount) {
        assert node.getLabels().size() == labelCount;
        assert node.getProperties().size() == propertyCount;
    }

    @Test
    public void testBasicGraphs() throws IOException {
        testSubgraph("(alice)", 1, 0);
        testSubgraph("(alice)-[:KNOWS]->(bob)", 2, 1);
        testSubgraph("(alice)<-[:KNOWS]->(bob)", 2, 2);
        testSubgraph("(alice)<-[:LOVES]-(bob)-[:HATES]->(carol)", 3, 2);
        testSubgraph("(alice)<-[:KNOWS]->(bob)<-[:KNOWS]->(carol)<-[:KNOWS]->(alice)", 3, 6);
    }

    @Test
    public void testLabelsAndProperties() throws IOException {
        testSubgraph("()", 1, 0);
        testSubgraph("({\"foo\":\"bar\"})", 1, 0);
        testSubgraph("({\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
        testSubgraph("(:Foo)", 1, 0);
        testSubgraph("(:Foo {\"foo\":\"bar\"})", 1, 0);
        testSubgraph("(:Foo {\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
        testSubgraph("(:Foo:Bar)", 1, 0);
        testSubgraph("(:Foo:Bar {\"foo\":\"bar\"})", 1, 0);
        testSubgraph("(:Foo:Bar {\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
        testSubgraph("(a)", 1, 0);
        testSubgraph("(b {\"foo\":\"bar\"})", 1, 0);
        testSubgraph("(c {\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
        testSubgraph("(d:Foo)", 1, 0);
        testSubgraph("(e:Foo {\"foo\":\"bar\"})", 1, 0);
        testSubgraph("(f:Foo {\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
        testSubgraph("(g:Foo:Bar)", 1, 0);
        testSubgraph("(h:Foo:Bar {\"foo\":\"bar\"})", 1, 0);
        testSubgraph("(i:Foo:Bar {\"foo\":\"bar\",\"baz\":\"qux\"})", 1, 0);
    }

}
