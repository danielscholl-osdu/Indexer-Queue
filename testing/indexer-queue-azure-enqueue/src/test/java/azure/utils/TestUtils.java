package azure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import org.opengroup.osdu.azure.util.AzureServicePrincipal;

import static azure.pubsub.PubsubEndpointIntegrationTest.indentatedBody;

public class TestUtils {
    static Logger log=Logger.getLogger(TestUtils.class.getName());
    protected static String token = null;

    protected static final String domain = System.getProperty("DOMAIN", System.getenv("DOMAIN"));
    private static final AzureServicePrincipal azureServicePrincipal = new AzureServicePrincipal();

    public static final String getAclSuffix() {
        String environment = getEnvironment();
        //build.gradle currently throws exception if a variable is set to empty or not set at all
        //workaround by setting it to an "empty" string to construct the url
        if (environment.equalsIgnoreCase("empty")) environment = "";
        if (!environment.isEmpty())
            environment = "." + environment;

        return String.format("%s%s.%s", TenantUtils.getTenantName(), environment, domain);
    }

    public static final String getAcl() {
        return String.format("data.default.owners@%s", getAclSuffix());
    }

    public static String getEnvironment() {
        return System.getProperty("DEPLOY_ENV", System.getenv("DEPLOY_ENV"));
    }

    public static String getApiPath(String api) throws Exception {
        String baseUrl = System.getProperty("STORAGE_URL", System.getenv("STORAGE_URL"));
        URL mergedURL = new URL(baseUrl + api);
        System.out.println(mergedURL.toString());
        return mergedURL.toString();
    }

    public static synchronized String getToken() throws Exception {
        if (Strings.isNullOrEmpty(token)) {
            String sp_id = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            String sp_secret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
            String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
            String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
            token = azureServicePrincipal.getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
        }
        return "Bearer " + token;
    }

    public static ClientResponse send(String path, String httpMethod, Map<String, String> headers, String requestBody,
                                      String query) throws Exception {

        log(httpMethod, TestUtils.getApiPath(path + query), headers, requestBody);
        ClientResponse response = null;
        Client client = TestUtils.getClient();
        client.setConnectTimeout(300000);
        client.setReadTimeout(300000);
        client.setFollowRedirects(false);

        WebResource webResource = client.resource(TestUtils.getApiPath(path + query));
        int count = 1;
        int MaxRetry = 3;
        while (count < MaxRetry) {
            try {
                headers.put("correlation-id", headers.getOrDefault("correlation-id", UUID.randomUUID().toString()));
                WebResource.Builder builder = webResource.type(MediaType.APPLICATION_JSON)
                  .header("Authorization", token);
                headers.forEach((k, v) -> builder.header(k, v));
                //removing Auth header before logging
                headers.remove("Authorization");
                log.info(String.format("Request method: %s\nRequest Headers: %s\nRequest Body: %s", httpMethod, headers, indentatedBody(requestBody)));
                log.info(String.format("Attempt: #%s/%s, CorrelationId: %s", count, MaxRetry, headers.get("correlation-id")));
                response = builder.method(httpMethod, ClientResponse.class, requestBody);
                if (response.getStatusInfo().getFamily().equals(Response.Status.Family.valueOf("SERVER_ERROR"))) {
                    count++;
                    Thread.sleep(5000);
                    continue;
                } else {
                    break;
                }
            } catch (Exception ex) {
                log.severe("Exception While Making Request: " + ex.getMessage());
                ex.printStackTrace();
                count++;
                if (count == MaxRetry) {
                  throw new AssertionError("Error: Send request error", ex);
                }
            } finally {
                //log response
                if (response != null)
                  log.info(String.format("This is the response received : %s\nResponse Headers: %s\nResponse Status code: %s", response, response.getHeaders(), response.getStatus()));
            }
        }
        return response;
    }

    public static ClientResponse send(String url, String path, String httpMethod, Map<String, String> headers,
                                      String requestBody, String query) throws Exception {

      log(httpMethod, url + path, headers, requestBody);
      Client client = TestUtils.getClient();
      ClientResponse response = null;
      client.setConnectTimeout(300000);
      client.setReadTimeout(300000);
      client.setFollowRedirects(false);

      WebResource webResource = client.resource(url + path);
      int count = 1;
      int MaxRetry = 3;
      while (count < MaxRetry) {
          try {
            headers.put("correlation-id", headers.getOrDefault("correlation-id", UUID.randomUUID().toString()));
            WebResource.Builder builder = webResource.type(MediaType.APPLICATION_JSON)
              .header("Authorization", token);
            headers.forEach((k, v) -> builder.header(k, v));
            //removing Auth header before logging
            headers.remove("Authorization");
            log.info(String.format("Http Method: %s\nRequest URL: %s\nRequest Headers: %s\nRequest Body: %s", httpMethod, url, headers, indentatedBody(requestBody)));
            log.info(String.format("Attempt: #%s/%s, CorrelationId: %s", count, MaxRetry, headers.get("correlation-id")));
            response = builder.method(httpMethod, ClientResponse.class, requestBody);
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.valueOf("SERVER_ERROR"))) {
              count++;
              Thread.sleep(5000);
              continue;
            } else {
              break;
            }
          } catch (Exception ex) {
            log.severe("Exception While Making Request: " + ex.getMessage());
            ex.printStackTrace();
            count++;
            if (count == MaxRetry) {
              throw new AssertionError("Error: Send request error", ex);
            }
          } finally {
              //log response
              if (response != null)
                    log.info(String.format("This is the response received : %s\nResponse Headers: %s\nResponse Status code: %s", response, response.getHeaders(), response.getStatus()));
          }
      }
      return response;
    }

    private static void log(String method, String url, Map<String, String> headers, String body) {
        log.info(String.format("%s: %s", method, url));
        log.info(body);
    }

    protected static Client getClient() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
        allowMethods("PATCH");
        return Client.create();
    }

    private static void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
