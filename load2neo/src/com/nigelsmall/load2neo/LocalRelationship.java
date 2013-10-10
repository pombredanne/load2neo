package com.nigelsmall.load2neo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocalRelationship {

    private static ObjectMapper mapper = new ObjectMapper();

    private LocalNode startNode;
    private String type;
    private HashMap<String, Object> properties;
    private LocalNode endNode;

    public LocalRelationship(LocalNode startNode, String type, Map<String, Object> properties, LocalNode endNode) {
        this.startNode = startNode;
        this.type = type;
        this.mergeProperties(properties);
        this.endNode = endNode;
    }

    public String toString() {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("(");
        parts.add(this.startNode.getName());
        parts.add(")-[:");
        parts.add(this.type);
        if (this.properties != null) {
            if (parts.size() > 0) {
                parts.add(" ");
            }
            try {
                parts.add(mapper.writeValueAsString(this.properties));
            } catch (IOException e) {
                //
            }
        }
        parts.add("]->(");
        parts.add(this.endNode.getName());
        parts.add(")");
        return StringUtils.join(parts, "");
    }

    public LocalNode getStartNode() {
        return this.startNode;
    }

    public void setStartNode(LocalNode node) {
        this.startNode = node;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void mergeProperties(Map<String, Object> properties) {
        if (properties != null) {
            if (this.properties == null) {
                this.properties = new HashMap<>(properties);
            } else {
                this.properties.putAll(properties);
            }
        }
    }

    public LocalNode getEndNode() {
        return this.endNode;
    }

    public void setEndNode(LocalNode node) {
        this.endNode = node;
    }

}
