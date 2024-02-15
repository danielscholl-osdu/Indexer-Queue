package azure.utils;

import com.google.common.base.Strings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class TestUtils {
    static Logger log=Logger.getLogger(TestUtils.class.getName());
    protected static String token = null;

    protected static final String domain = System.getProperty("DOMAIN", System.getenv("DOMAIN"));
    private static final AzureServicePrincipal azureServicePrincipal = new AzureServicePrincipal();

    public static final String getAclSuffix() {
        String environment = getEnvironment();

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

    public static String getApiPath(String baseUrl, String api) throws Exception {
        URL mergedURL = new URL(baseUrl + api);
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

    public static ClientResponse send(String baseUrl, String path, String httpMethod, Map<String, String> headers, String requestBody,
                                      String query) throws Exception {

        ClientResponse response = null;
        Client client = TestUtils.getClient();
        client.setConnectTimeout(300000);
        client.setReadTimeout(300000);
        client.setFollowRedirects(false);

        WebResource webResource = client.resource(TestUtils.getApiPath(baseUrl, path + query));
        int count = 1;
        int MaxRetry = 3;
        while (count < MaxRetry) {
            try {
                headers.put("correlation-id", headers.getOrDefault("correlation-id", UUID.randomUUID().toString()));
                WebResource.Builder builder = webResource.type(MediaType.APPLICATION_JSON)
                  .header("Authorization", token);
                headers.forEach((k, v) -> builder.header(k, v));
                response = builder.method(httpMethod, ClientResponse.class, requestBody);
                if (response.getStatusInfo().getFamily().equals(Response.Status.Family.valueOf("SERVER_ERROR"))) {
                    count++;
                    Thread.sleep(5000);
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
            }
        }
        return response;
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
        return Client.create();
    }
}
