/*
 * Copyright 2013, Nigel Small
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nigelsmall.load2neo;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Subgraph {

    private HashMap<String, LocalNode> nodes;
    private ArrayList<LocalRelationship> relationships;

    public Subgraph() {
        super();
        this.nodes = new HashMap<>();
        this.relationships = new ArrayList<>();
    }

    public String toString() {
        ArrayList<String> s = new ArrayList<>();
        for (LocalNode node : this.nodes.values()) {
            s.add(node.toString());
        }
        for (LocalRelationship rel : this.relationships) {
            s.add(rel.toString());
        }
        return StringUtils.join(s, "\n");
    }

    public LocalNode mergeNode(LocalNode node) {
        if (this.nodes.containsKey(node.getName())) {
            this.nodes.get(node.getName()).mergeNode(node);
        } else {
            this.nodes.put(node.getName(), node);
        }
        return this.nodes.get(node.getName());
    }

    public void addRelationship(LocalRelationship rel) {
        this.mergeNode(rel.getStartNode());
        this.mergeNode(rel.getEndNode());
        this.relationships.add(rel);
    }

    /** Load this subgraph into a Neo4j database
     *
     * @param database the GraphDatabaseService to load into
     * @return a Map of named Nodes that have been loaded
     */
    public Map<String, Node> loadInto(GraphDatabaseService database) {
        HashMap<String, Node> nodes = new HashMap<>();
        HashMap<String, Node> namedNodes = new HashMap<>();
        for (LocalNode localNode : this.nodes.values()) {
            Node node = null;
            String hookLabel = localNode.getHookLabel();
            if (hookLabel != null) {
                String hookKey = localNode.getHookKey();
                Object hookValue = null;
                if (localNode.getProperties().containsKey(hookKey)) {
                    hookValue = localNode.getProperties().get(hookKey);
                }
                for (Node foundNode : database.findNodesByLabelAndProperty(DynamicLabel.label(hookLabel), hookKey, hookValue)) {
                    node = foundNode;
                    break;
                }
            }
            if (node == null) {
                node = database.createNode();
            }
            Set<String> labels = localNode.getLabels();
            if (labels != null) {
                for (String label : labels) {
                    node.addLabel(DynamicLabel.label(label));
                }
            }
            Map<String, Object> properties = localNode.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() != null) {
                        node.setProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            nodes.put(localNode.getName(), node);
            if (localNode.isNamed()) {
                namedNodes.put(localNode.getName(), node);
            }
        }
        for (LocalRelationship localRelationship : this.relationships) {
            Node startNode = nodes.get(localRelationship.getStartNode().getName());
            Node endNode = nodes.get(localRelationship.getEndNode().getName());
            Relationship rel = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(localRelationship.getType()));
            Map<String, Object> properties = localRelationship.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    rel.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        return namedNodes;
    }

}
