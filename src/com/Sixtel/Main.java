package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Spark.init();


        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");



                    HashMap m = new HashMap();


                    return new ModelAndView(m, "home.html");
                })

        );
        new MustacheTemplateEngine();

    }
}
