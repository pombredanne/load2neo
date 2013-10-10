package com.nigelsmall.load2neo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class LocalNode {

    private static ObjectMapper mapper = new ObjectMapper();

    private String name;
    private boolean named;
    private HashSet<String> labels;
    private HashMap<String, Object> properties;
    private String hookLabel;
    private String hookKey;

    public LocalNode(String name, Set<String> labels, Map<String, Object> properties) {
        if (name == null) {
            this.name = UUID.randomUUID().toString();
            this.named = false;
        } else {
            this.name = name;
            this.named = true;
        }
        this.mergeLabels(labels);
        this.mergeProperties(properties);
    }

    public String toString() {
        final ArrayList<String> parts = new ArrayList<>();
        if (this.name != null && !this.name.equals("")) {
            parts.add(this.name);
        }
        if (this.labels != null) {
            String labels = "";
            for (String label : this.labels) {
                labels += ":" + label;
            }
            parts.add(labels);
        }
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
        if (this.hookLabel == null) {
            return "(" + StringUtils.join(parts, "") + ")";
        } else if (this.hookKey == null) {
            return ":" + this.hookLabel + ":=>(" + StringUtils.join(parts, "") + ")";
        } else {
            return ":" + this.hookLabel + ":" + this.hookKey + ":=>(" + StringUtils.join(parts, "") + ")";
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean isNamed() {
        return this.named;
    }

    public Set<String> getLabels() {
        return this.labels;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void mergeNode(LocalNode node) {
        if (node.name != null) {
            this.name = node.name;
        }
        this.mergeLabels(node.labels);
        this.mergeProperties(node.properties);
    }

    public void mergeLabels(Set<String> labels) {
        if (labels != null) {
            if (this.labels == null) {
                this.labels = new HashSet<>(labels);
            } else {
                this.labels.addAll(labels);
            }
        }
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

    public void setHook(String label, String key) {
        if (this.labels == null) {
            this.labels = new HashSet<>();
        }
        this.labels.add(label);
        this.hookLabel = label;
        if (key != null) {
            if (this.properties == null) {
                this.properties = new HashMap<>();
            }
            if (!this.properties.containsKey(key)) {
                this.properties.put(key, null);
            }
        }
        this.hookKey = key;
    }

    public String getHookLabel() {
        return this.hookLabel;
    }

    public String getHookKey() {
        return this.hookKey;
    }

}
