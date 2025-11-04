package com.lov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

/**
 * File: Main.java
 * This is the main entry point for the FTI website's backend server.
 * It uses the SparkJava framework to serve the website and handle form submissions.
 */
public class Main {

    // A dedicated logger is better practice than System.out.println or printStackTrace
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        // --- Server Configuration ---

        // Set the port the server will run on. Use the PORT environment variable if set, otherwise default to 8080.
        port(getHerokuAssignedPort());

        // Tell Spark where to find our static files (HTML, CSS, JS, images).
        staticFiles.location("/public");


        // --- Routing ---

        // Define the POST endpoint for the enrollment form.
        post("/enroll", (request, response) -> {
            
            response.type("application/json");

            try {
                String requestBody = request.body();
                JsonObject submission = gson.fromJson(requestBody, JsonObject.class);

                // Check for null values which might occur with malformed JSON
                if (submission == null || submission.get("name") == null || submission.get("email") == null) {
                    response.status(400); // Bad Request
                    return "{\"status\":\"error\", \"message\":\"Malformed request. Name and email are required.\"}";
                }

                // --- DATA HANDLING ---
                // In a real application, you would save this to a database or send an email.
                // For this example, we log it to confirm receipt.
                LOGGER.info("=========================================");
                LOGGER.info("===      NEW ENROLLMENT REQUEST      ===");
                LOGGER.info("=========================================");
                LOGGER.info("Name: {}", submission.get("name").getAsString());
                LOGGER.info("Email: {}", submission.get("email").getAsString());
                LOGGER.info("Phone: {}", submission.get("phone").getAsString());
                LOGGER.info("Services: {}", submission.get("services").getAsString());
                LOGGER.info("Wants Free Consultation: {}", submission.get("consultation").getAsBoolean());
                LOGGER.info("=========================================");

                // Return a success message to the frontend.
                return "{\"status\":\"success\", \"message\":\"Enrollment received!\"}";

            } catch (JsonSyntaxException jsonEx) {
                // This catches errors if the incoming data isn't valid JSON.
                LOGGER.error("Error parsing JSON from request", jsonEx);
                response.status(400); // Bad Request
                return "{\"status\":\"error\", \"message\":\"Invalid data format.\"}";
            } catch (Exception e) {
                // This is a general catch-all for any other unexpected server errors.
                LOGGER.error("An unexpected error occurred", e);
                response.status(500); // Internal Server Error
                return "{\"status\":\"error\", \"message\":\"An internal server error occurred.\"}";
            }
        });

        LOGGER.info("FTI server started. Access at http://localhost:{}", getHerokuAssignedPort());
    }

    /**
     * A helper method to get the port from the environment variable.
     * @return The port number.
     */
    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String port = processBuilder.environment().get("PORT");
        if (port != null) {
            return Integer.parseInt(port);
        }
        return 8080; // Default port
    }
}
