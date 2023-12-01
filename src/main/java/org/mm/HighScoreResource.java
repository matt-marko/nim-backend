package org.mm;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import java.net.URI;

@Path("/high-scores")
public class HighScoreResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHighScores() {
        List<HighScoreEntity> highScores = HighScoreEntity.listAll();
        return Response.ok(highScores).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHighScoreById(@PathParam("id") Long id) {
        return HighScoreEntity.findByIdOptional(id)
                .map(highScore -> Response.ok(highScore).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/size")
    public Integer countHighScores() {
        return HighScoreEntity.listAll().size();
    }

    @POST
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createHighScore(HighScoreEntity highScore) {
        HighScoreEntity.persist(highScore);

        if(highScore.isPersistent()) {
            return Response.created(URI.create("/high-scores" + highScore.id)).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @DELETE
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteById(@PathParam("id") Long id) {
        boolean deleted = HighScoreEntity.deleteById(id);

        if(deleted) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}