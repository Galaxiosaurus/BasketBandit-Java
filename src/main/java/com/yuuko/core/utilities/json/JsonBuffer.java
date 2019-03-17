package com.yuuko.core.utilities.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class JsonBuffer {
    private static final Logger log = LoggerFactory.getLogger(JsonBuffer.class);
    private String jsonOutput;

    public JsonBuffer(String inputUrl, String acceptDirective, String contentTypeDirective, RequestProperty... extraProperties) {
        try(ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            String accept = (acceptDirective.equals("default")) ? "application/json" : acceptDirective;
            String contentType = (contentTypeDirective.equals("default")) ? "application/json" : contentTypeDirective;
            HttpsURLConnection conn = (HttpsURLConnection) new URL(inputUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", accept);
            conn.setRequestProperty("Content-Type", contentType);

            if(extraProperties != null && extraProperties.length > 0) {
                for(RequestProperty property : extraProperties) {
                    conn.setRequestProperty(property.getHeader(), property.getDirective());
                }
            }

            if(conn.getResponseCode() != 200) {
                return;
            }

            byte[] buffer = new byte[1024];
            int length;

            while((length = conn.getInputStream().read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            jsonOutput = result.toString();

        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", this, ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves the json output as a string, does nothing else to it.
     *
     * @return String
     */
    public String getAsString() {
        return jsonOutput;
    }

    /**
     * Retrieves the json output as a JsonObject which can be handled and manipulated with the Google Gson package.
     *
     * @return JsonObject
     * @throws IllegalStateException IllegalStateException
     */
    public JsonObject getAsJsonObject() throws IllegalStateException {
        try {
            return (jsonOutput == null) ? null : new JsonParser().parse(jsonOutput).getAsJsonObject();
        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", this, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Retrieves the json output as a JsonArray which can be handled and manipulated with the Google Gson package.
     *
     * @return JsonArray
     * @throws IllegalStateException IllegalStateException
     */
    public JsonArray getAsJsonArray() throws IllegalStateException {
        try {
            return (jsonOutput == null) ? null : new JsonParser().parse(jsonOutput).getAsJsonArray();
        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", this, ex.getMessage(), ex);
            return null;
        }
    }

}
