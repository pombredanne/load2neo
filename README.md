[![Build Status](https://travis-ci.org/nigelsmall/load2neo.png)](https://travis-ci.org/nigelsmall/load2neo)

# Load2neo

[Load2neo](http://nigelsmall.com/load2neo) is a server extension written for
Neo4j 2.0 and upwards that provides a facility for bulk loading data into a
Neo4j database. Currently, only the [Geoff](http://nigelsmall.com/geoff)
format is supported although XML support will follow.

## Installation

To install, drop the jar file into the server *plugins* directory and edit the
`conf/neo4j-server.properties` file to ensure the extension is mounted:

```
org.neo4j.server.thirdparty_jaxrs_classes=com.nigelsmall.load2neo=/load2neo
```

You will need to restart the server for the configuration and extension to be
loaded.


## Usage

```
curl -X POST http://localhost:7474/load2neo/load/geoff -d '(alice {"name":"Alice})<-[:KNOWS]->(bob {"name":"Bob"})'
```

```
curl -X POST http://localhost:7474/load2neo/load/geoff -d @foo.geoff
```
