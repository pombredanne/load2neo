package com.nigelsmall.geoff.parser;

import com.nigelsmall.load2neo.LocalNode;
import com.nigelsmall.load2neo.LocalRelationship;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Abstract parser class to be extended for specific handlers. Initialise the
 * class by supplying source data and call parse to iterate through Geoff
 * elements. For each element encountered, a callback will be activated with
 * the element detail.
 */
public abstract class GeoffParser {

    private static ObjectMapper mapper = new ObjectMapper();

    private String source;
    private int n;
    private int sourceLength;

    public GeoffParser(String source) {
        this.source = source;// original source data
        this.n = 0;// pointer to next position to be parsed
        this.sourceLength = source.length();
    }

    public abstract void handleBoundary();

    public abstract void handleComment(String comment);

    public abstract void handleHook(LocalNode node, String label, String key);

    public abstract void handleNode(LocalNode node);

    public abstract void handleRelationship(LocalRelationship rel);

    private Character nextChar() {
        if (this.n < this.sourceLength) {
            return this.source.charAt(this.n);
        } else {
            return null;
        }

    }

    private boolean nextCharEquals(char ch) {
        if (this.n < this.sourceLength) {
            return this.source.charAt(this.n) == ch;
        } else {
            return false;
        }

    }

    private String peek() {
        return this.peek(1);
    }

    private String peek(int length) {
        int last = this.sourceLength;
        int start = Math.min(this.n, last);
        int end = Math.min(this.n + length, last);
        return this.source.substring(start, end);
    }

    public void parse() throws GeoffParserException {
        this.n = 0;
        while (this.parseElement()) {
            //
        }
    }

    private List parseArray() throws GeoffParserException {
        this.parseLiteral('[');
        this.parseWhitespace();
        if (this.nextCharEquals(']')) {
            this.parseLiteral(']');
            return new ArrayList<Object>();
        }
        if (this.nextCharEquals('"')) {
            ArrayList<Object> items = new ArrayList<>();
            items.add(this.parseString());
            this.parseWhitespace();
            while (this.nextCharEquals(',')) {
                this.parseLiteral(',');
                this.parseWhitespace();
                items.add(this.parseString());
                this.parseWhitespace();
            }
            this.parseLiteral(']');
            return items;
        } else if (this.nextCharEquals('-') || Character.isDigit(this.nextChar())) {
            ArrayList<Object> integerItems = new ArrayList<>();
            ArrayList<Object> doubleItems = new ArrayList<>();
            Number n = this.parseNumber();
            doubleItems.add(n.doubleValue());
            if (n instanceof Integer) {
                integerItems.add(n.intValue());
            }
            this.parseWhitespace();
            while (this.nextCharEquals(',')) {
                this.parseLiteral(',');
                this.parseWhitespace();
                n = this.parseNumber();
                doubleItems.add(n.doubleValue());
                if (n instanceof Integer) {
                    integerItems.add(n.intValue());
                }
                this.parseWhitespace();
            }
            this.parseLiteral(']');
            if (integerItems.size() == doubleItems.size()) {
                return integerItems;
            } else {
                return doubleItems;
            }
        } else if (this.nextCharEquals('t') || this.nextCharEquals('f')) {
            ArrayList<Object> items = new ArrayList<>();
            items.add(this.parseBoolean());
            this.parseWhitespace();
            while (this.nextCharEquals(',')) {
                this.parseLiteral(',');
                this.parseWhitespace();
                items.add(this.parseBoolean());
                this.parseWhitespace();
            }
            this.parseLiteral(']');
            return items;
        } else {
            throw new GeoffParserException("Disarray at position " + Integer.toString(this.n));
        }
    }

    private String parseArrow() throws GeoffParserException {
        if (this.nextCharEquals('<')) {
            this.parseLiteral('<');
            this.parseLiteral('-');
            return "<-";
        } else if (this.nextCharEquals('-')) {
            this.parseLiteral('-');
            if (this.nextCharEquals('>')) {
                this.parseLiteral('>');
                return "->";
            } else {
                return "-";
            }
        } else {
            throw new GeoffParserException("Broken arrow at position " + Integer.toString(this.n));
        }
    }

    private boolean parseBoolean() throws GeoffParserException {
        if (this.nextCharEquals('t')) {
            parseLiteral('t');
            parseLiteral('r');
            parseLiteral('u');
            parseLiteral('e');
            return true;
        } else if (this.nextCharEquals('f')) {
            parseLiteral('f');
            parseLiteral('a');
            parseLiteral('l');
            parseLiteral('s');
            parseLiteral('e');
            return false;
        } else {
            throw new GeoffParserException("Cannot establish truth at position " + Integer.toString(this.n));
        }
    }

    private void parseBoundary() throws GeoffParserException {
        this.parseLiteral('~');
        this.parseLiteral('~');
        this.parseLiteral('~');
        this.parseLiteral('~');
        while (this.nextCharEquals('~')) {
            this.parseLiteral('~');
        }
    }

    private String parseComment() throws GeoffParserException {
        this.parseLiteral('/');
        this.parseLiteral('*');
        int m = this.n;
        int endOfComment = this.source.indexOf("*/", this.n);
        if (endOfComment >= 0) {
            this.n = endOfComment + 2;
            return this.source.substring(m, endOfComment).trim();
        } else {
            this.n = this.sourceLength;
            return this.source.substring(m).trim();
        }

    }

    private boolean parseElement() throws GeoffParserException {
        this.parseWhitespace();
        if (this.n >= this.source.length()) {
            return false;
        }
        switch (this.nextChar()) {
            case '(':
                LocalNode node = this.parseNode();
                ArrayList<LocalRelationship> relationships = new ArrayList<>();
                while (this.n < this.sourceLength && "<-".indexOf((int) this.nextChar()) >= 0) {
                    String nextChar = this.peek();
                    if ("<-".contains(nextChar)) {
                        String arrow1 = this.parseArrow();
                        LocalRelationship rel = this.parseRelationshipBox();
                        String arrow2 = this.parseArrow();
                        LocalNode otherNode = this.parseNode();
                        if ("-".equals(arrow1) && "-".equals(arrow2)) {
                            throw new GeoffParserException("Lack of direction at position " + Integer.toString(this.n));
                        }
                        if ("<-".equals(arrow1)) {
                            relationships.add(new LocalRelationship(otherNode, rel.getType(), rel.getProperties(), node));
                        }
                        if ("->".equals(arrow2)) {
                            relationships.add(new LocalRelationship(node, rel.getType(), rel.getProperties(), otherNode));
                        }
                        node = otherNode;
                    } else {
                        throw new GeoffParserException("Unexpected character '" + nextChar + "' at position " + Integer.toString(this.n));
                    }
                }
                this.parseWhitespace();
                Map<String, Object> properties = null;
                if ("{".equals(this.peek())) {
                    properties = this.parsePropertyMap();
                }
                if (relationships.size() > 0) {
                    for (LocalRelationship rel : relationships) {
                        rel.mergeProperties(properties);
                        this.handleRelationship(rel);
                    }
                } else {
                    node.mergeProperties(properties);
                    this.handleNode(node);
                }
                break;
            case ':':
                this.parseLiteral(':');
                this.parseWhitespace();
                String label = this.parseName();
                this.parseWhitespace();
                this.parseLiteral(':');
                this.parseWhitespace();
                String key = null;
                if (!"=>".equals(this.peek(2))) {
                    key = this.parseName();
                    this.parseWhitespace();
                    this.parseLiteral(':');
                    this.parseWhitespace();
                }
                this.parseLiteral('=');
                this.parseLiteral('>');
                node = this.parseNode();
                this.handleHook(node, label, key);
                break;
            case '/':
                this.handleComment(this.parseComment());
                break;
            case '~':
                this.parseBoundary();
                this.handleBoundary();
                break;
            default:
                throw new GeoffParserException("Unexpected character '" + this.peek() + "' at position " + Integer.toString(this.n));
        }
        return true;
    }

    private void parseKeyValuePairInto(HashMap<String, Object> map) throws GeoffParserException {
        String key = this.parseName();
        this.parseWhitespace();
        this.parseLiteral(':');
        this.parseWhitespace();
        Object value = this.parseValue();
        map.put(key, value);
    }

    private HashSet<String> parseLabels() throws GeoffParserException {
        HashSet<String> labels = new HashSet<>();
        while (this.nextCharEquals(':')) {
            this.parseLiteral(':');
            labels.add(this.parseName());
        }
        return labels;
    }

    private char parseLiteral(char ch) throws GeoffParserException {
        if (this.source.charAt(this.n) == ch) {
            this.n += 1;
            return ch;
        } else {
            throw new GeoffParserException("Unexpected character '" + Character.toString(ch) + "' at position " + Integer.toString(this.n));
        }
    }

    private String parseName() throws GeoffParserException {
        if (this.nextCharEquals('"')) {
            return this.parseString();
        } else {
            int m = this.n;
            while (n < this.sourceLength && Character.isLetterOrDigit(this.source.charAt(this.n)) || this.source.charAt(this.n) == '_') {
                this.n += 1;
            }
            return this.source.substring(m, this.n);
        }
    }

    private LocalNode parseNode() throws GeoffParserException {
        String name;
        HashSet<String> labels;
        HashMap<String, Object> properties;
        this.parseLiteral('(');
        this.parseWhitespace();
        if (this.nextCharEquals(')')) {
            name = null;
            labels = null;
            properties = null;
        } else if (this.nextCharEquals(':')) {
            name = null;
            labels = this.parseLabels();
            this.parseWhitespace();
            if (this.nextCharEquals('{')) {
                properties = this.parsePropertyMap();
            } else {
                properties = null;
            }
        } else if (this.nextCharEquals('{')) {
            name = null;
            labels = null;
            properties = this.parsePropertyMap();
        } else {
            name = this.parseName();
            this.parseWhitespace();
            if (this.nextCharEquals(':')) {
                labels = this.parseLabels();
            } else {
                labels = null;
            }
            this.parseWhitespace();
            if (this.nextCharEquals('{')) {
                properties = this.parsePropertyMap();
            } else {
                properties = null;
            }
        }
        this.parseWhitespace();
        this.parseLiteral(')');
        return new LocalNode(name, labels, properties);
    }

    private Number parseNumber() throws GeoffParserException {
        boolean isReal = false;
        int m = this.n;
        if (this.nextCharEquals('-')) {
            this.n += 1;
        }
        while (Character.isDigit(this.nextChar())) {
            this.n += 1;
        }
        if (this.nextCharEquals('.')) {
            isReal = true;
            this.n += 1;
            while (Character.isDigit(this.nextChar())) {
                this.n += 1;
            }
        }
        if (this.nextCharEquals('E') || this.nextCharEquals('e')) {
            isReal = true;
            this.n += 1;
            if (this.nextCharEquals('+') || this.nextCharEquals('-')) {
                this.n += 1;
            }
            while (Character.isDigit(this.nextChar())) {
                this.n += 1;
            }
        }
        if (isReal) {
            return Double.parseDouble(this.source.substring(m, this.n));
        } else {
            return Integer.parseInt(this.source.substring(m, this.n));
        }
    }

    private HashMap<String, Object> parsePropertyMap() throws GeoffParserException {
        HashMap<String, Object> properties = new HashMap<>();
        this.parseLiteral('{');
        this.parseWhitespace();
        if (!this.nextCharEquals('}')) {
            this.parseKeyValuePairInto(properties);
            this.parseWhitespace();
            while (",".equals(this.peek())) {
                this.parseLiteral(',');
                this.parseWhitespace();
                this.parseKeyValuePairInto(properties);
                this.parseWhitespace();
            }

        }
        this.parseLiteral('}');
        return properties;
    }

    private LocalRelationship parseRelationshipBox() throws GeoffParserException {
        this.parseLiteral('[');
        this.parseWhitespace();
        if (this.nextCharEquals(':')) {
            // read and ignore relationship name, if present
            this.parseName();
            this.parseWhitespace();
        }
        this.parseLiteral(':');
        String type = this.parseName();
        this.parseWhitespace();
        LocalRelationship rel;
        if (this.nextCharEquals('{')) {
            rel = new LocalRelationship(null, type, this.parsePropertyMap(), null);
            this.parseWhitespace();
        } else {
            rel = new LocalRelationship(null, type, null, null);
        }
        this.parseLiteral(']');
        return rel;
    }

    /** Consumes a string in JSON format
     */
    private String parseString() throws GeoffParserException {
        int m = this.n;
        this.parseLiteral('"');
        boolean endOfString = false;
        while (!endOfString) {
            while (n < this.sourceLength && this.source.charAt(this.n) != '"') {
                this.n += 1;
            }
            if (this.source.charAt(this.n - 1) != '\\') {
                endOfString = true;
            }
            this.parseLiteral('"');
        }
        try {
            return mapper.readValue(this.source.substring(m, this.n), String.class);
        } catch (IOException e) {
            throw new GeoffParserException("Unable to parse JSON string at position " + Integer.toString(m));
        }
    }

    private Object parseValue() throws GeoffParserException {
        String nextChar = this.peek();
        Object value;
        if ("[".equals(nextChar)) {
            List listValue = this.parseArray();
            int listValueSize = listValue.size();
            if (listValueSize == 0) {
                value = new Object[0];
            } else if (listValue.get(0) instanceof String) {
                value = listValue.toArray(new String[listValueSize]);
            } else if (listValue.get(0) instanceof Integer) {
                value = listValue.toArray(new Integer[listValueSize]);
            } else if (listValue.get(0) instanceof Double) {
                value = listValue.toArray(new Double[listValueSize]);
            } else if (listValue.get(0) instanceof Boolean) {
                value = listValue.toArray(new Boolean[listValueSize]);
            } else {
                throw new GeoffParserException("Unexpected array type");
            }
        } else if ("\"".equals(nextChar)) {
            value = this.parseString();
        } else if ("-0123456789".contains(nextChar)) {
            //value = this.parsePattern(JSON_NUMBER, Number.class);
            value = this.parseNumber();
        } else if ("t".equals(nextChar) || "f".equals(nextChar)) {
            value = this.parseBoolean();
        } else if ("n".equals(nextChar)) {
            this.parseLiteral('n');
            this.parseLiteral('u');
            this.parseLiteral('l');
            this.parseLiteral('l');
            value = null;
        } else {
            throw new GeoffParserException("Unexpected character '" + nextChar + "' at position " + Integer.toString(this.n));
        }
        return value;
    }

    private String parseWhitespace() {
        int m = this.n;
        while (this.n < this.sourceLength && Character.isWhitespace(this.source.charAt(this.n))) {
            this.n += 1;
        }
        return this.source.substring(m, this.n);
    }

}
