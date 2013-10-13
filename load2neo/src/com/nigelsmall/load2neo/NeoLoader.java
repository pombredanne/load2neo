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

import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NeoLoader {

    final private Logger logger = LoggerFactory.getLogger(NeoLoader.class);
    final private GraphDatabaseService database;

    public NeoLoader(GraphDatabaseService database) {
        this.database = database;
    }

    /**
     * Load a subgraph into the database.
     *
     * @param subgraph the subgraph to load
     * @return a Map of named Nodes that have been loaded
     */
    public Map<String, Node> load(Subgraph subgraph) {
        logger.info("Loading subgraph");
        long t0 = System.currentTimeMillis();
        HashMap<String, Node> nodes = new HashMap<>();
        HashMap<String, Node> namedNodes = new HashMap<>();
        for (AbstractNode abstractNode : subgraph.getNodes().values()) {
            Node node = this.createOrUpdateNode(abstractNode);
            nodes.put(abstractNode.getName(), node);
            if (abstractNode.isNamed()) {
                namedNodes.put(abstractNode.getName(), node);
            }
        }
        for (AbstractRelationship abstractRelationship : subgraph.getRelationships()) {
            Node startNode = nodes.get(abstractRelationship.getStartNode().getName());
            Node endNode = nodes.get(abstractRelationship.getEndNode().getName());
            DynamicRelationshipType type = DynamicRelationshipType.withName(abstractRelationship.getType());
            Relationship rel = startNode.createRelationshipTo(endNode, type);
            this.addProperties(rel, abstractRelationship.getProperties());
        }
        long t1 = System.currentTimeMillis() - t0;
        logger.info("Loaded " + Integer.toString(nodes.size()) + " nodes in " + Long.toString(t1) + "ms");
        return namedNodes;
    }

    /**
     * Create a new node or update an existing one. An update will occur only
     * if this is a hooked node specification and a match can be found.
     *
     * @param abstractNode an abstract node specification
     * @return the concrete Node object that is either fetched or created
     */
    public Node createOrUpdateNode(AbstractNode abstractNode) {
        Node node = null;
        String hookLabel = abstractNode.getHookLabel();
        // is this a hooked node?
        if (hookLabel != null) {
            // determine the label, key and value to look up
            Label label = DynamicLabel.label(hookLabel);
            String hookKey = abstractNode.getHookKey();
            Object hookValue = null;
            if (abstractNode.getProperties().containsKey(hookKey)) {
                hookValue = abstractNode.getProperties().get(hookKey);
            }
            // find the "first" node with the given label, key and value
            for (Node foundNode : database.findNodesByLabelAndProperty(label, hookKey, hookValue)) {
                node = foundNode;
                break;
            }
        }
        // if not hooked, or cannot find, create anew
        if (node == null) {
            node = database.createNode();
        }
        this.addLabels(node, abstractNode.getLabels());
        this.addProperties(node, abstractNode.getProperties());
        return node;
    }

    /**
     * Add a set of labels to a node.
     *
     * @param node the destination Node to which to add the labels
     * @param labels a set of strings containing label names
     */
    public void addLabels(Node node, Set<String> labels) {
        if (labels == null)
            return;
        for (String label : labels) {
            node.addLabel(DynamicLabel.label(label));
        }
    }

    /**
     * Add a map of properties to a node or relationship.
     *
     * @param entity the destination Node or Relationship to which to add the properties
     * @param properties a Map of key-value property pairs
     */
    public void addProperties(PropertyContainer entity, Map<String, Object> properties) {
        if (properties == null)
            return;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                entity.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

}
