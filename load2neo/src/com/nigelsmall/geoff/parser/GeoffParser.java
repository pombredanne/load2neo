package com.nigelsmall.geoff.parser;

import com.nigelsmall.load2neo.LocalNode;
import com.nigelsmall.load2neo.LocalRelationship;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract parser class to be extended for specific handlers. Initialise the
 * class by supplying source data and call parse to iterate through Geoff
 * elements. For each element encountered, a callback will be activated with
 * the element detail.
 */
public abstract class GeoffParser {

    private static ObjectMapper mapper = new ObjectMapper();

    private static Pattern ARROW        = Pattern.compile("(<-|->|-)");
    private static Pattern BOUNDARY     = Pattern.compile("(~{4})");
    private static Pattern JSON_BOOLEAN = Pattern.compile("(true|false)");
    private static Pattern JSON_NUMBER  = Pattern.compile("(?i)(-?(0|[1-9]\\d*)(\\.\\d+)?(e[+-]?\\d+)?)");
    private static Pattern JSON_STRING  = Pattern.compile("(\".*?(?<!\\\\)\")");
    private static Pattern NAME         = Pattern.compile("([\\p{L}\\p{N}_]+)");

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
        ArrayList<Object> items = new ArrayList<>();
        Pattern valuePattern;
        Class valueType = null;
        this.parseLiteral("[");
        this.parseWhitespace();
        String nextChar = this.peek();
        if (!this.nextChar().equals(']')) {
            if (this.nextChar().equals('\"')) {
                valuePattern = JSON_STRING;
                valueType = String.class;
            } else if ("-0123456789".contains(nextChar)) {
                valuePattern = JSON_NUMBER;
                valueType = Number.class;
            } else if ("tf".contains(nextChar)) {
                valuePattern = JSON_BOOLEAN;
                valueType = Boolean.class;
            } else {
                throw new GeoffParserException("Unexpected character '" + nextChar + "' at position " + Integer.toString(this.n));
            }
            items.add(this.parsePattern(valuePattern, valueType));
            this.parseWhitespace();
            while (this.nextChar().equals(',')) {
                this.parseLiteral(",");
                this.parseWhitespace();
                items.add(this.parsePattern(valuePattern, valueType));
                this.parseWhitespace();
            }
        }
        this.parseLiteral("]");
        if (valueType == Number.class) {
            int itemCount = items.size();
            ArrayList<Integer> integerItems = new ArrayList<>(itemCount);
            ArrayList<Float> floatItems = new ArrayList<>(itemCount);
            for (Object item : items) {
                Number n = (Number)item;
                floatItems.add(n.floatValue());
                if (n instanceof Integer) {
                    integerItems.add(n.intValue());
                }
            }
            if (integerItems.size() == itemCount) {
                return integerItems;
            } else {
                return floatItems;
            }
        }
        return items;
    }

    private String parseComment() throws GeoffParserException {
        this.parseLiteral("/");
        this.parseLiteral("*");
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
                        String arrow1 = this.parsePattern(ARROW).toString();
                        LocalRelationship rel = this.parseRelationshipBox();
                        String arrow2 = this.parsePattern(ARROW).toString();
                        LocalNode otherNode = this.parseNode();
                        if ("-".equals(arrow1) && "-".equals(arrow2)) {
                            throw new GeoffParserException("No relationship direction specified");
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
                this.parseLiteral(":");
                this.parseWhitespace();
                String label = this.parseName();
                this.parseWhitespace();
                this.parseLiteral(":");
                this.parseWhitespace();
                String key = null;
                if (!"=>".equals(this.peek(2))) {
                    key = this.parseName();
                    this.parseWhitespace();
                    this.parseLiteral(":");
                    this.parseWhitespace();
                }
                this.parseLiteral("=>");
                node = this.parseNode();
                this.handleHook(node, label, key);
                break;
            case '/':
                String comment = this.parseComment();
                this.handleComment(comment);
                break;
            case '~':
                this.parsePattern(BOUNDARY);
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

    private char parseLiteral(char ch) throws GeoffParserException {
        if (this.source.charAt(this.n) == ch) {
            this.n += 1;
            return ch;
        } else {
            throw new GeoffParserException("Unexpected character '" + Character.toString(ch) + "' at position " + Integer.toString(this.n));
        }

    }

    private String parseLiteral(String literal) throws GeoffParserException {
        int length = literal.length();
        for (int i = 0; i < length; i++) {
            char ch = this.source.charAt(this.n + i);
            if (literal.charAt(i) != this.source.charAt(this.n + i)) {
                throw new GeoffParserException("Unexpected character '" + Character.toString(ch) + "' at position " + Integer.toString(this.n));
            }

        }

        this.n += length;
        return literal;
    }

    private String parseName() throws GeoffParserException {
        if (this.nextChar().equals('\"')) {
            return this.parsePattern(JSON_STRING, String.class).toString();
        } else {
            return this.parsePattern(NAME).toString();
        }
    }

    private LocalNode parseNode() throws GeoffParserException {
        String name;
        HashSet<String> labels;
        HashMap<String, Object> properties;
        this.parseLiteral('(');
        this.parseWhitespace();
        char nextChar = this.nextChar();
        if (((Character) nextChar).equals(')')) {
            name = null;
            labels = null;
            properties = null;
        } else if (((Character) nextChar).equals(':')) {
            name = null;
            labels = this.parseLabels();
            this.parseWhitespace();
            if (this.nextChar().equals('{')) {
                properties = this.parsePropertyMap();
            } else {
                properties = null;
            }
        } else if (((Character) nextChar).equals('{')) {
            name = null;
            labels = null;
            properties = this.parsePropertyMap();
        } else {
            name = this.parseName();
            this.parseWhitespace();
            if (this.nextChar().equals(':')) {
                labels = this.parseLabels();
            } else {
                labels = null;
            }
            this.parseWhitespace();
            if (this.nextChar().equals('{')) {
                properties = this.parsePropertyMap();
            } else {
                properties = null;
            }
        }
        this.parseWhitespace();
        this.parseLiteral(")");
        return new LocalNode(name, labels, properties);
    }

    private HashSet<String> parseLabels() throws GeoffParserException {
        HashSet<String> labels = new HashSet<>();
        while (this.nextChar().equals(':')) {
            this.parseLiteral(':');
            labels.add(this.parseName());
        }
        return labels;
    }

    private Object parsePattern(Pattern pattern) throws GeoffParserException {
        return this.parsePattern(pattern, null);
    }

    private Object parsePattern(Pattern pattern, Class decodeToClass) throws GeoffParserException {
        Matcher matcher = pattern.matcher(this.source);
        boolean found = matcher.find(this.n);
        String value;
        if (found && matcher.start() == this.n) {
            value = matcher.group(0);
            this.n += value.length();
        } else {
            throw new GeoffParserException("Pattern not found at position " + Integer.toString(this.n));
        }
        if (decodeToClass != null) {
            try {
                return mapper.readValue(value, decodeToClass);
            } catch (IOException e) {
                throw new GeoffParserException("Unable to parse JSON value at position " + Integer.toString(this.n));
            }
        } else {
            return value;
        }
    }

    private HashMap<String, Object> parsePropertyMap() throws GeoffParserException {
        HashMap<String, Object> properties = new HashMap<>();
        this.parseLiteral('{');
        this.parseWhitespace();
        if (!this.nextChar().equals('}')) {
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
        String nextChar = this.peek();
        if (!":".equals(nextChar)) {
            // read and ignore relationship name, if present
            this.parseName();
            this.parseWhitespace();
        }

        this.parseLiteral(":");
        String type = this.parseName();
        this.parseWhitespace();
        nextChar = this.peek();
        LocalRelationship rel;
        if ("{".equals(nextChar)) {
            rel = new LocalRelationship(null, type, this.parsePropertyMap(), null);
            this.parseWhitespace();
        } else {
            rel = new LocalRelationship(null, type, null, null);
        }

        this.parseLiteral("]");
        return rel;
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
            } else if (listValue.get(0) instanceof Float) {
                value = listValue.toArray(new Float[listValueSize]);
            } else if (listValue.get(0) instanceof Boolean) {
                value = listValue.toArray(new Boolean[listValueSize]);
            } else {
                throw new GeoffParserException("Unexpected array type");
            }
        } else if ("\"".equals(nextChar)) {
            value = this.parsePattern(JSON_STRING, String.class);
        } else if ("-0123456789".contains(nextChar)) {
            value = this.parsePattern(JSON_NUMBER, Number.class);
        } else if ("t".equals(nextChar)) {
            this.parseLiteral("true");
            value = true;
        } else if ("f".equals(nextChar)) {
            this.parseLiteral("false");
            value = false;
        } else if ("n".equals(nextChar)) {
            this.parseLiteral("null");
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
