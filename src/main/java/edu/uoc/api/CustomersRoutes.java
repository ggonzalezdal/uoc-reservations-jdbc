package edu.uoc.api;

import edu.uoc.api.dto.ApiDtos.ErrorResponse;
import edu.uoc.dao.CustomerDao;
import io.javalin.Javalin;

public final class CustomersRoutes {
    private CustomersRoutes() {}

    public static void register(Javalin app, CustomerDao customerDao) {

        app.get("/customers", ctx -> ctx.json(customerDao.findAll()));

        app.get("/customers/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            var customerOpt = customerDao.findById(id);
            if (customerOpt.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", "Customer not found: " + id, ctx.path()));
                return;
            }

            ctx.json(customerOpt.get());
        });
    }
}

