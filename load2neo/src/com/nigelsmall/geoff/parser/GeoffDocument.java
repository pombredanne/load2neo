package com.nigelsmall.geoff.parser;

import com.nigelsmall.load2neo.LocalNode;
import com.nigelsmall.load2neo.LocalRelationship;
import com.nigelsmall.load2neo.Subgraph;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class GeoffDocument {

    private static class SubgraphBuilder extends GeoffParser {

        Subgraph subgraph;
        ArrayList<Subgraph> subgraphs;

        public SubgraphBuilder(String text) {
            super(text);
            this.subgraphs = new ArrayList<>();
            this.handleBoundary();
        }

        public void handleBoundary() {
            this.subgraph = new Subgraph();
            this.subgraphs.add(this.subgraph);
        }

        public void handleComment(String comment) {
        }

        public void handleHook(LocalNode node, String label, String key) {
            this.subgraph.mergeNode(node).setHook(label, key);
        }

        public void handleNode(LocalNode node) {
            this.subgraph.mergeNode(node);
        }

        public void handleRelationship(LocalRelationship rel) {
            this.subgraph.addRelationship(rel);
        }

    }

    public static Subgraph[] parse(String text) throws GeoffParserException {
        SubgraphBuilder builder = new SubgraphBuilder(text);
        builder.parse();
        return builder.subgraphs.toArray(new Subgraph[builder.subgraphs.size()]);
    }

    public static Subgraph[] parse(InputStream stream) throws GeoffParserException, IOException {
        return parse(IOUtils.toString(stream, "UTF-8"));
    }

    private GeoffDocument() {
        super();
    }

}
