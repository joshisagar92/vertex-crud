package com.poc.verticle;

import com.google.gson.Gson;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import com.poc.entity.Product;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.stream.Collectors;

public class CrudVerticle extends AbstractVerticle {

    private JDBCClient jdbcClient;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Router router = Router.router(vertx);

        router.get("/api/products").handler(this::getAll);

        router.route("/api/products*").handler(BodyHandler.create());
        router.post("/api/products").handler(this::addData);
        router.delete("/api/products/:id").handler(this::deleteData);

        ConfigStoreOptions config = new ConfigStoreOptions()
                .setFormat("properties")
                .setType("file")
                .setConfig(new JsonObject().put("path", "config.properties").put("config", true)
                );

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(config);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);


        configRetriever.configStream().handler(conf -> {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClass(conf.getString("database.driver"));
            ds.setJdbcUrl(conf.getString("database.url"));
            ds.setUser(conf.getString("database.user"));
            ds.setPassword(conf.getString("database.password"));
            jdbcClient = JDBCClient.create(vertx, ds);
        });

        vertx.createHttpServer()
                .requestHandler(router::handle).listen(8088, result -> {
            if(result.succeeded()){
                startPromise.complete();
            }else {
                startPromise.fail(result.cause());
            }
        });
    }

    private void deleteData(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");

        String sql = "DELETE FROM productdetails WHERE id = "+Integer.parseInt(id);
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            jdbcClient.getConnection(asyncResult -> {
               if(asyncResult.succeeded()){
                    SQLConnection connection = asyncResult.result();
                    connection.update(sql,updateResultAsyncResult -> {
                        if (updateResultAsyncResult.succeeded()) {
                            routingContext.response().setStatusCode(204).end();
                        }else {
                            routingContext.fail(updateResultAsyncResult.cause());
                        }

                    });
                }else {
                    routingContext.fail(asyncResult.cause());
                }

            });
        }

    }

    private void getAll(RoutingContext routingContext) {
        jdbcClient.getConnection(asyncResult -> {
            SQLConnection connection = asyncResult.result();
            connection.query("SELECT * FROM productdetails",resultSet -> {
                List<Product> products = resultSet.result().getRows().stream()
                        .map(entries -> new Product(entries.getInteger("id"),entries.getString("name")))
                        .collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(products));
            });
            if(asyncResult.failed()){
                routingContext.fail(asyncResult.cause());
            }
        });
    }

    private void addData(RoutingContext routingContext) {
        Product product = new Gson().fromJson(routingContext.getBodyAsString(), Product.class);
        String sql = "INSERT INTO productdetails (id,name) VALUES("+product.getId()+",\'"+product.getName()+"\')";
        jdbcClient.getConnection(ar -> {
            SQLConnection connection = ar.result();
            connection.update(sql, updateResult -> {
                if (updateResult.succeeded()) {
                    routingContext.response()
                            .setStatusCode(201)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(product));
                }else if(updateResult.failed()) {
                    routingContext.fail(updateResult.cause());
                }
            });

            if(ar.failed()){
                routingContext.fail(ar.cause());
            }
        });
    }
}
