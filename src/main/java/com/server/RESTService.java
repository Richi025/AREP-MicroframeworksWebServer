package com.server;

/**
 * Interface representing a REST service. 
 * 
 * This interface defines a method for processing HTTP requests and generating corresponding responses.
 * Implementations of this interface should handle the logic for interacting with the server and 
 * providing appropriate responses based on the request received.
 */
public interface RESTService {

    /**
     * Processes an HTTP request and generates a corresponding response.
     * 
     * @param request  the HTTP request to be processed, typically in the form of a JSON string or a query string
     * @param response the HTTP response generated as a result of processing the request
     * @return a string representing the processed value, which may be a message, data, or status
     */
    public String getValue(String request, String response);
    
}
