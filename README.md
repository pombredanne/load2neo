[![Build Status](https://travis-ci.org/nigelsmall/load2neo.png)](https://travis-ci.org/nigelsmall/load2neo)

# Load2neo

Load2neo is a server extension written for Neo4j 2.0 and upwards. It provides a
facility for bulk loading data in the Geoff format and will eventually provide
XML support too!

## Installation

To install, drop the `load2neo.jar` file into the server *plugins*
directory and edit the conf/neo4j-server.properties file to ensure the
extension is mounted:

```
org.neo4j.server.thirdparty_jaxrs_classes=com.nigelsmall.load2neo=/load2neo
```

You will need to restart the server for the configuration and extension to be
loaded.

## Geoff

Geoff is a simple text-based format for storing flat file graph data and should
be recognisable to anyone familiar with Cypher, on which its syntax is based.

### Syntax Overview

A Geoff document is composed of one or more subgraphs, separated by the `~~~~`
sequence.

Each subgraph consists of one or more paths, for example:

```
(alice {"name":"Alice"})
(bob {"name":"Bob"})
(carol {"name":"Carol"})
(dave {"name":"Dave"})
(alice)<-[:KNOWS]->(bob)<-[:KNOWS]->(carol)<-[:KNOWS]->(dave)<-[:KNOWS]->(alice)
```

Properties are represented in JSON format and may be provided in single nodes,
in longer paths or in both:

```
(alice {"name":"Alice"})
(bob {"name":"Bob"})
(alice {"age":33})<-[:KNOWS]->(bob {"age":44})
```

Labels are also supported and one or more may be added to each node:

```
(alice:Person {"name":"Alice"})<-[:KNOWS]->(bob:Person {"name":"Bob"})
```

When loaded, a Geoff subgraph will generally create new nodes and relationships
for all those represented. To use an existing node instead, a hook can be used
to bind a node to one with the same label, key and value. If more than one is
found, the one chosen is non-deterministic.

```
:Person:name:=>(alice:Person {"name":"Alice"})
(alice)-[:KNOWS]->(bob:Person {"name":"Bob"})
(alice)-[:KNOWS]->(carol:Person {"name":"Carol"})
(alice)-[:KNOWS]->(dave:Person {"name":"Dave"})
```


### Loading

```
curl -X POST http://localhost:7474/load2neo/load/geoff -d @foo.geoff
```

### Full Syntax Specification

```
document       := subgraph (_ boundary _ subgraph)*
boundary       := "~~~~"

subgraph       := [element (_ element)*]
element        := comment | hook | path

comment        := "/*" <<any text excluding sequence "*/">> "*/"

hook           := ":" ~ label [~ ":" ~ key] ~ ":" "=>" node

path           := node (forward_path | reverse_path)*
forward_path   := "-" relationship "->" node
reverse_path   := "<-" relationship "-" node

node           := named_node | anonymous_node
named_node     := "(" ~ node_name [_ label_list] [_ property_map] ~ ")"
anonymous_node := "(" ~ [label_list] [_ property_map] ~ ")"
relationship   := "[" ~ ":" type [_ property_map] ~ "]"
label_list     := (":" label)+
label          := name | JSON_STRING
property_map   := "{" ~ [key_value (~ "," ~ key_value)* ~] "}"
node_name      := name | JSON_STRING
name           := (ALPHA | DIGIT | "_")+
type           := name | JSON_STRING
key_value      := key ~ ":" ~ value
key            := name | JSON_STRING
value          := array | JSON_STRING | JSON_NUMBER | JSON_BOOLEAN | JSON_NULL

array          := empty_array | string_array | numeric_array | boolean_array
empty_array    := "[" ~ "]"
string_array   := "[" ~ JSON_STRING (~ "," ~ JSON_STRING)* ~ "]"
numeric_array  := "[" ~ JSON_NUMBER (~ "," ~ JSON_NUMBER)* ~ "]"
boolean_array  := "[" ~ JSON_BOOLEAN (~ "," ~ JSON_BOOLEAN)* ~ "]"

* Mandatory whitespace is represented by "_" and optional whitespace by "~"
```
