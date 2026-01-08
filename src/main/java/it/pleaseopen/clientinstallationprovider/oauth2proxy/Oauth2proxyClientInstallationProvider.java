package it.pleaseopen.clientinstallationprovider.oauth2proxy;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;


/**
 * @author <a href="mailto:mathieu.passenaud@please-open.it">Mathieu Passenaud</a>
 * @version $Revision: 1 $
 */
public class Oauth2proxyClientInstallationProvider implements ClientInstallationProvider {
@Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI baseUri) {
        StringBuilder envVars = new StringBuilder();
        
        // Provider configuration
        envVars.append("OAUTH2_PROXY_PROVIDER=\"keycloak-oidc\"\n");
        envVars.append("OAUTH2_PROXY_PROVIDER_DISPLAY_NAME=\"Keycloak\"\n");
        
        // Client ID
        envVars.append("OAUTH2_PROXY_CLIENT_ID=\"").append(client.getClientId()).append("\"\n");
        
        // Client Secret (only for confidential clients)
        if (showClientCredentialsAdapterConfig(client)) {
            Map<String, Object> adapterConfig = getClientCredentialsAdapterConfig(session, client);
            if (adapterConfig != null && adapterConfig.containsKey("secret")) {
                envVars.append("OAUTH2_PROXY_CLIENT_SECRET=\"").append(adapterConfig.get("secret")).append("\"\n");
            }
        }
        
        // Optional cookie settings (commented out as examples)
        envVars.append("#OAUTH2_PROXY_COOKIE_HTTPONLY=\"true\"\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_NAME=\"_oauth2_proxy\"\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_SAMESITE=\"lax\"\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_SECRET=\"GENERATE_YOUR_OWN_SECRET_HERE\"\n");
        envVars.append("#OAUTH2_PROXY_COOKIE_SECURE=\"false\"\n");
        envVars.append("#OAUTH2_PROXY_EMAIL_DOMAINS=\"*\"\n");
        envVars.append("#OAUTH2_PROXY_HTTP_ADDRESS=\"0.0.0.0:8080\"\n");
        
        // OIDC Issuer URL
        String issuerUrl = baseUri.toString();
        if (!issuerUrl.endsWith("/")) {
            issuerUrl += "/";
        }
        issuerUrl += "realms/" + realm.getName();
        envVars.append("OAUTH2_PROXY_OIDC_ISSUER_URL=\"").append(issuerUrl).append("\"\n");
        
        // Optional pass tokens settings (commented out)
        envVars.append("#OAUTH2_PROXY_PASS_ACCESS_TOKEN=\"true\"\n");
        envVars.append("#OAUTH2_PROXY_PASS_AUTHORIZATION_HEADER=\"true\"\n");
        envVars.append("\n");
        
        // Redirect URL - should be configured by user
        String redirectUrl = "";
        if (!client.getRedirectUris().isEmpty()) {
            redirectUrl = client.getRedirectUris().iterator().next();
        }
        if (redirectUrl.isEmpty()) {
            envVars.append("OAUTH2_PROXY_REDIRECT_URL=\"https://YOUR_DOMAIN/oauth2/callback\"\n");
        } else {
            envVars.append("OAUTH2_PROXY_REDIRECT_URL=\"").append(redirectUrl).append("\"\n");
        }
        
        // Optional reverse proxy settings (commented out)
        envVars.append("#OAUTH2_PROXY_REVERSE_PROXY=\"true\"\n");
        
        // Scopes
        envVars.append("OAUTH2_PROXY_SCOPE=\"openid email profile\"\n");
        
        // Optional authorization headers (commented out)
        envVars.append("#OAUTH2_PROXY_SET_AUTHORIZATION_HEADER=\"true\"\n");
        envVars.append("#OAUTH2_PROXY_SET_XAUTHREQUEST=\"true\"\n");
        envVars.append("#OAUTH2_PROXY_SKIP_PROVIDER_BUTTON=\"false\"\n");
        
        // Upstream configuration (commented out as example)
        envVars.append("#OAUTH2_PROXY_UPSTREAMS=\"http://127.0.0.1:9000\"\n");

        return Response.ok(envVars.toString(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    public static Map<String, Object> getClientCredentialsAdapterConfig(KeycloakSession session, ClientModel client) {
        String clientAuthenticator = client.getClientAuthenticatorType();
        ClientAuthenticatorFactory authenticator = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, clientAuthenticator);
        return authenticator.getAdapterConfiguration(client);
    }


    public static boolean showClientCredentialsAdapterConfig(ClientModel client) {
        if (client.isPublicClient()) {
            return false;
        }

        if (client.isBearerOnly() && !client.isServiceAccountsEnabled() && client.getNodeReRegistrationTimeout() <= 0) {
            return false;
        }

        return true;
    }

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Oauth2-proxy environment variables";
    }

    @Override
    public String getHelpText() {
        return "Environment variables used by the Oauth2-proxy to configure clients.  You must set these environment variables in your Oauth2-proxy deployment.  You may also want to tweak these variables after you download them.";
    }

    @Override
    public void close() {

    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "oauth2-proxy-env";
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public String getFilename() {
        return "oauth2proxy.env";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }
}