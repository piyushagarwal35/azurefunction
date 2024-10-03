package hitachi_genai;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String api5Response = callExternalApi("http://localhost:8080/resourceusagemetrics/v1/api/usagemetrics/datasync/subscriptions/0f8c3763-9eeb-40f9-9037-2a5426da75e9/duration");

        // Process the response from API 5
        String api6RequestBody = generateApi6RequestBody(api5Response);

        // Call API 6 with the generated JSON input
        String api6Response = callExternalApiWithJson("http://localhost:8080/resourceusagemetrics/v1/api/usagemetrics/datasync/subscriptions/0f8c3763-9eeb-40f9-9037-2a5426da75e9/load", api6RequestBody);

        // Return the combined response
        String combinedResponse = "API 5 Response: " + api5Response + "\nAPI 6 Response: " + api6Response;
        return request.createResponseBuilder(HttpStatus.OK).body(combinedResponse).build();
    }
        // Parse query parameter
//        String api1Response = callExternalApi("http://localhost:8080/resourceusagemetrics/v1/api/usagemetrics/overallincurredcost");
       // String api2Response = callExternalApi("");
  //      return request.createResponseBuilder(HttpStatus.OK).body(api1Response).build();

    private String callExternalApi(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling API";
        }
    }

    private String callExternalApiWithJson(String url, String json) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(json));
            request.setHeader("Content-type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling API";
        }
    }

    private String generateApi6RequestBody(String api5Response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(api5Response);
            JsonNode resultNode = rootNode.path("data").path("result").get(0);

            String subscriptionId = resultNode.path("subscriptionId").asText();
            String subscriptionName = resultNode.path("subscriptionName").asText();
            JsonNode datesNode = resultNode.path("dates");

            // Create the request body for API 6
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("{");
            requestBody.append("\"subscriptionId\":\"").append(subscriptionId).append("\",");
            requestBody.append("\"subscriptionName\":\"").append(subscriptionName).append("\",");
            requestBody.append("\"usageMetrics\":[");

            for (JsonNode dateNode : datesNode) {
                String date = dateNode.asText();
                requestBody.append("{");
                requestBody.append("\"billedCost\":100.950,");
                requestBody.append("\"billingAccountId\":\"\",");
                requestBody.append("\"chargePeriodStart\":\"").append(date).append("T00:00Z\",");
                requestBody.append("\"chargePeriodEnd\":\"").append(date).append("T23:59Z\",");
                requestBody.append("\"chargeDescription\":\"Sample Description\"");
                requestBody.append("},");
            }

            // Remove the last comma
            if (requestBody.charAt(requestBody.length() - 1) == ',') {
                requestBody.deleteCharAt(requestBody.length() - 1);
            }

            requestBody.append("]");
            requestBody.append("}");

            return requestBody.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}

//        final String query = request.getQueryParameters().get("name");
//        final String name = request.getBody().orElse(query);
//
//        if (name == null) {
//            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
//        } else {
//            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
//        }
//    }
//
//    private String callExternalApi(String url) {
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//            HttpGet request = new HttpGet(url);
//            try (CloseableHttpResponse response = httpClient.execute(request)) {
//                return EntityUtils.toString(response.getEntity());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error calling API";
//        }
//    }

