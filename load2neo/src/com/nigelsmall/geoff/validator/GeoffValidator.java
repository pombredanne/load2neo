package com.nigelsmall.geoff.validator;

import com.nigelsmall.geoff.parser.GeoffDocument;
import com.nigelsmall.geoff.parser.GeoffParserException;
import com.nigelsmall.load2neo.Subgraph;
import org.neo4j.graphdb.Node;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class GeoffValidator {

    public static void validate(InputStream stream) throws IOException, GeoffParserException {
        int i = 0;
        for (Subgraph subgraph : GeoffDocument.parse(stream)) {
            System.out.println("----------------------------------------");
            System.out.println("Subgraph: " + Integer.toString(i));
            System.out.println("----------------------------------------");
            System.out.println(subgraph);
            i += 1;
        }
    }

    public static void main(String... args) throws IOException, GeoffParserException {
        for (String arg: args) {
            System.out.println("========================================");
            System.out.println("Document: " + arg);
            System.out.println("========================================");
            validate(new FileInputStream(arg));
        }
        System.out.println("----------------------------------------");
    }

}
