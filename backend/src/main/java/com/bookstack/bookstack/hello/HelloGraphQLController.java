package com.bookstack.bookstack.hello;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class HelloGraphQLController {

    @QueryMapping
    public String hello() {
        return "Hello from BookStack (GraphQL)!";
    }
}