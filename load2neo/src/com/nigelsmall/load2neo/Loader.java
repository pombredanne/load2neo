package com.nigelsmall.load2neo;

import com.nigelsmall.geoff.parser.GeoffDocument;
import com.nigelsmall.geoff.parser.GeoffParserException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.Map;

@Path("/load")
public class Loader {

    private final GraphDatabaseService database;

    public Loader(@Context GraphDatabaseService database) {
        this.database = database;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/geoff")
    public Response postGeoff(String text) {
        Response.Status statusCode;
        StringBuilder content = new StringBuilder();
        try (Transaction tx = database.beginTx()) {
            for (Subgraph subgraph : GeoffDocument.parse(text)) {
                Map<String, Node> nodes = subgraph.loadInto(database);
                for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                    content.append('"');
                    content.append(entry.getKey());
                    content.append('"');
                    content.append('\t');
                    content.append(entry.getValue().getId());
                    content.append('\n');
                }
            }
            tx.success();
            statusCode = Response.Status.OK;
        } catch (GeoffParserException e) {
            statusCode = Response.Status.BAD_REQUEST;
            content.append(e.toString());
            content.append('\n');
        }
        return Response.status(statusCode).entity(content.toString().getBytes(Charset.forName("UTF-8"))).build();
    }

}
