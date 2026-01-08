# Keycloak OAuth2-Proxy Client Installation Provider

*By please-open.it*

A Keycloak SPI (Service Provider Interface) extension that generates OAuth2-Proxy configuration files directly from Keycloak client settings. This provider simplifies the integration of OAuth2-Proxy with Keycloak by automatically generating the necessary configuration.

## Features

This extension provides two configuration export formats for OAuth2-Proxy:

### 1. Environment Variables Format (`oauth2-proxy-env`)
Generates a `.env` file containing OAuth2-Proxy environment variables:
- Essential variables (provider, client credentials, OIDC issuer, redirect URL)
- Optional variables as commented examples
- Perfect for Docker Compose, Kubernetes ConfigMaps, or systemd services

### 2. Configuration File Format (`oauth2-proxy-config`)
Generates a complete OAuth2-Proxy configuration file with:
- Detailed comments for every parameter
- Step-by-step setup instructions
- Security best practices
- Examples for common use cases
- Guidance for generating secrets and configuring upstreams

## Prerequisites

- Keycloak 26.5.0 or later
- Java 21 or later
- Maven 3.6 or later

## Installation

### 1. Build the Extension

```bash
mvn clean install
```

This will generate a JAR file in the `deployments/` directory.

### 2. Deploy to Keycloak

Copy the generated JAR file to your Keycloak `providers` directory:

```bash
cp deployments/please-open.it-oauth2proxy-client-installation-provider-26.5.0-SNAPSHOT.jar \
   /path/to/keycloak/providers/
```

### 3. Restart Keycloak

Restart your Keycloak instance to load the new provider:

```bash
# For standalone server
/path/to/keycloak/bin/kc.sh start

# For Docker
docker restart keycloak

# For Docker Compose
docker compose restart keycloak
```

## Usage

Once installed, the OAuth2-Proxy configuration options will be available in the Keycloak Admin Console.

### Generating Configuration Files

1. **Login to Keycloak Admin Console**

2. **Navigate to your client:**
   - Select your realm
   - Go to `Clients`
   - Select the client you want to configure for OAuth2-Proxy

3. **Access the Installation tab:**
   - Click on the `Installation` tab (or `Action` → `Download adapter config`)
   - You will see two new format options:
     - **"Oauth2-proxy environment variables"** - Generates `.env` file
     - **"Oauth2-proxy configuration file"** - Generates `.cfg` file with full documentation

4. **Select your preferred format and download**

### Environment Variables Format

The environment variables format (`oauth2proxy.env`) includes:

```bash
OAUTH2_PROXY_PROVIDER="keycloak-oidc"
OAUTH2_PROXY_CLIENT_ID="your-client-id"
OAUTH2_PROXY_CLIENT_SECRET="your-client-secret"
OAUTH2_PROXY_OIDC_ISSUER_URL="https://keycloak.example.com/realms/myrealm"
OAUTH2_PROXY_REDIRECT_URL="https://app.example.com/oauth2/callback"
OAUTH2_PROXY_SCOPE="openid email profile"
# ... additional optional variables
```

**Usage with Docker Compose:**

```yaml
services:
  oauth2-proxy:
    image: quay.io/oauth2-proxy/oauth2-proxy:latest
    env_file:
      - oauth2proxy.env
    ports:
      - "4180:4180"
```

### Configuration File Format

The configuration file format (`oauth2proxy.cfg`) provides a complete, documented configuration:

```toml
## OAuth2 Proxy Configuration File
## Generated from Keycloak for client: your-client-id

provider = "keycloak-oidc"
client_id = "your-client-id"
client_secret = "your-client-secret"
oidc_issuer_url = "https://keycloak.example.com/realms/myrealm"
# ... with detailed comments for every option
```

**Usage with OAuth2-Proxy:**

```bash
oauth2-proxy --config=oauth2proxy.cfg
```

## Configuration Tips

### Client Configuration in Keycloak

For optimal OAuth2-Proxy integration, configure your Keycloak client as follows:

1. **Client Type:** `OpenID Connect`
2. **Client Authentication:** `On` (for confidential clients) or `Off` (for public clients)
3. **Valid Redirect URIs:** Add your OAuth2-Proxy callback URL
   - Example: `https://your-domain.com/oauth2/callback`
4. **Web Origins:** Add your application domain (for CORS)

### Required Post-Generation Steps

After downloading the configuration, you need to:

1. **Generate a Cookie Secret** (for config file format):
   ```bash
   python -c 'import os,base64; print(base64.urlsafe_b64encode(os.urandom(32)).decode())'
   # OR
   openssl rand -base64 32 | tr -- '+/' '-_'
   ```
   Uncomment and set the `cookie_secret` parameter.

2. **Configure Upstreams:**
   Update the `upstreams` parameter with your actual backend service URL(s).

3. **Adjust Security Settings:**
   - Set `cookie_secure = true` in production (requires HTTPS)
   - Configure `email_domains` to restrict access to your organization
   - Review and adjust `cookie_samesite` based on your setup

4. **Verify Redirect URL:**
   Ensure the `redirect_url` matches your deployment and is configured in Keycloak.

## Example Integration

### Docker Compose Setup

```yaml
version: '3.8'

services:
  backend:
    image: your-backend:latest
    ports:
      - "9000:9000"
  
  oauth2-proxy:
    image: quay.io/oauth2-proxy/oauth2-proxy:latest
    env_file:
      - oauth2proxy.env
    environment:
      - OAUTH2_PROXY_COOKIE_SECRET=${COOKIE_SECRET}
      - OAUTH2_PROXY_UPSTREAMS=http://backend:9000
      - OAUTH2_PROXY_HTTP_ADDRESS=0.0.0.0:4180
      - OAUTH2_PROXY_COOKIE_SECURE=true
    ports:
      - "4180:4180"
    depends_on:
      - backend
```

### Kubernetes Deployment

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: oauth2-proxy-config
data:
  oauth2proxy.cfg: |
    # Paste your generated configuration here
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oauth2-proxy
spec:
  template:
    spec:
      containers:
      - name: oauth2-proxy
        image: quay.io/oauth2-proxy/oauth2-proxy:latest
        args:
          - --config=/etc/oauth2-proxy/oauth2proxy.cfg
        volumeMounts:
          - name: config
            mountPath: /etc/oauth2-proxy
      volumes:
        - name: config
          configMap:
            name: oauth2-proxy-config
```

## Development

### Project Structure

```
.
├── src/main/java/it/pleaseopen/clientinstallationprovider/oauth2proxy/
│   ├── Oauth2proxyClientInstallationProvider.java        # Env vars generator
│   └── Oauth2proxyConfigClientInstallationProvider.java  # Config file generator
├── src/main/resources/META-INF/services/
│   └── org.keycloak.protocol.ClientInstallationProvider  # SPI registration
├── pom.xml
└── README.md
```

### Building for Different Keycloak Versions

Update the `keycloak.version` property in `pom.xml`:

```xml
<properties>
    <keycloak.version>26.5.0</keycloak.version>
</properties>
```

Then rebuild:

```bash
mvn clean install
```

## Troubleshooting

### Provider Not Appearing in Keycloak

1. Verify the JAR is in the `providers/` directory
2. Check Keycloak logs for errors during startup
3. Ensure you restarted Keycloak after copying the JAR
4. Verify the SPI service file exists: `META-INF/services/org.keycloak.protocol.ClientInstallationProvider`

### Generated Configuration Not Working

1. **Check Client Credentials:** Ensure client ID and secret match Keycloak
2. **Verify OIDC Issuer URL:** Test by accessing `{issuer_url}/.well-known/openid-configuration`
3. **Validate Redirect URI:** Must match exactly in both OAuth2-Proxy and Keycloak
4. **Cookie Secret:** Must be exactly 32 bytes when base64-decoded
5. **Upstreams:** Ensure backend services are accessible from OAuth2-Proxy

### OAuth2-Proxy Returns 401/403

1. Check email domain restrictions (`email_domains`)
2. Verify scopes include required claims
3. Review OAuth2-Proxy logs for detailed error messages
4. Test token validity at Keycloak's introspection endpoint

## Resources

- [OAuth2-Proxy Documentation](https://oauth2-proxy.github.io/oauth2-proxy/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak SPI Documentation](https://www.keycloak.org/docs/latest/server_development/)

## License

See [LICENSE](LICENSE) file for details.

## Author

**Mathieu Passenaud** - [please-open.it](https://please-open.it)

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Support

For issues and questions:
- Open an issue on the project repository
- Contact: mathieu.passenaud@please-open.it
