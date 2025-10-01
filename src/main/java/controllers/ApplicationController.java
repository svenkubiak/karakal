package controllers;

import io.mangoo.routing.Response;

public class ApplicationController {

    public Response health() {
        return Response.ok().bodyText("OK");
    }
}