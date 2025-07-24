package com.zihowl.thecalendar.data.source.remote.graphql;

import java.util.Map;

public class GraphQLRequest {
    private String query;
    private Map<String, Object> variables;

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public GraphQLRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    public String getQuery() { return query; }
    public Map<String, Object> getVariables() { return variables; }
}