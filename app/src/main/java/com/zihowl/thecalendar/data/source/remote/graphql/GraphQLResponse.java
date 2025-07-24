package com.zihowl.thecalendar.data.source.remote.graphql;

import java.util.List;

public class GraphQLResponse<T> {
    private T data;
    private List<Object> errors;

    public T getData() { return data; }
    public List<Object> getErrors() { return errors; }
}