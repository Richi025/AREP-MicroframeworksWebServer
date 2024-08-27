package com.server;

import java.util.HashMap;
import java.util.Map;

/**
 * The WebServer class is a simple framework for managing web services and serving static files.
 * It allows developers to define RESTful services using lambda expressions, extract query parameters
 * from HTTP requests, and specify the location of static files to be served by the server.
 */
public class WebServer {
    

    public static Map<String, RESTService> services = new HashMap<>();
    public static String staticFilesLocation = "";

    /**
     * Registers a new service to a specific URL path.
     * This method associates a URL path with a REST service, which is implemented
     * as a lambda expression. When a request is made to the specified URL, the associated
     * service will be executed.
     *
     * @param url The URL path that will trigger the service.
     * @param s   The service, defined as a lambda expression implementing the RESTService interface.
     */
    public static void get(String url, RESTService s) {
        services.put(url, s);
    }

    /**
     * Extracts the value of a specific query parameter from a given HTTP request.
     * This method parses the query string of the request and returns the value of the specified
     * parameter. If the parameter is not found, an empty string is returned.
     *
     * @param request The full HTTP request string, including the query string.
     * @param param   The name of the query parameter to extract.
     * @return The value of the specified query parameter, or an empty string if the parameter is not found.
     */
    public static String queryParams(String request, String param) {
        if (request.contains("?")) {
            String[] parts = request.split("\\?");
            if (parts.length > 1) {
                String queryString = parts[1];
                String[] params = queryString.split("&");
                for (String p : params) {
                    String[] keyValue = p.split("=");
                    if (keyValue[0].equals(param)) {
                        return keyValue.length > 1 ? keyValue[1] : "";
                    }
                }
            }
        }
        return "";
    }

    /**
     * Sets the location of the static files directory.
     * This directory will be used by the server to serve static content such as HTML, CSS, JavaScript, and image files.
     *
     * @param location The path to the directory containing static files.
     */
    public static void staticfiles(String location) {
        staticFilesLocation = location;
    }
}
