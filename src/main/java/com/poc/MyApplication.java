package com.poc;

import com.poc.verticle.CrudVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class MyApplication {

    public static void main(String[] args) {
            Vertx vertxapp = Vertx.vertx();
            Verticle myVerticle = new CrudVerticle();
            vertxapp.deployVerticle(myVerticle);
    }
}
