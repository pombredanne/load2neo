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

package com.nigelsmall.load2neo.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class DiscoveryResource {

    public DiscoveryResource() { }

    @GET
    @Produces("application/json")
    public Response getServiceIndex(@Context UriInfo info) {
        String absolutePath = info.getAbsolutePath().toString();
        String index = "{\n    \"geoff_loader\": \"" + absolutePath + "load/geoff\",\n    \"load2neo_version\": \"0.2.0\"\n}\n";
        return Response.status(Response.Status.OK).entity(index).build();
    }

}
