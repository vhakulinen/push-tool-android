package com.vhakulinen.pushtoolapp;

public class Response {
    private int code;
    private String message;

    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return this.code; }
    public String getMessage() { return this.message; }
}
