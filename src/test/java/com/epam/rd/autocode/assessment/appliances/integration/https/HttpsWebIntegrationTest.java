package com.epam.rd.autocode.assessment.appliances.integration.https;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailLinkFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:https-web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.docker.compose.enabled=false",
            "store.security.google.enabled=false",
            "store.security.web-auth=jwt",
            "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@ActiveProfiles("test")
class HttpsWebIntegrationTest {
    @LocalServerPort int httpsPort;
    @Autowired ServletWebServerApplicationContext context;

    @DynamicPropertySource
    static void tls(DynamicPropertyRegistry properties) {
        properties.add("server.ssl.enabled", () -> true);
        properties.add(
                "server.ssl.key-store", () -> TestTlsCertificate.KEYSTORE.toUri().toString());
        properties.add("server.ssl.key-store-password", () -> TestTlsCertificate.PASSWORD);
        properties.add("server.ssl.key-store-type", () -> "PKCS12");
        properties.add("server.ssl.key-alias", () -> "appliance-store");
        properties.add("server.ssl.enabled-protocols", () -> "TLSv1.3,TLSv1.2");
        properties.add("store.security.cookies.secure", () -> true);
        properties.add("server.servlet.session.cookie.secure", () -> true);
        properties.add("store.https.redirect-enabled", () -> true);
        properties.add("store.https.http-port", () -> 0);
        properties.add("store.https.public-origin", () -> "https://localhost:8443");
    }

    HttpClient client() throws Exception {
        return HttpClient.newBuilder()
                .sslContext(TestTlsCertificate.trustOnlyTestCertificate())
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    URI https(String path) {
        return URI.create("https://localhost:" + httpsPort + path);
    }

    HttpResponse<String> get(HttpClient client, String path, String cookies) throws Exception {
        var request = HttpRequest.newBuilder(https(path));
        if (cookies != null) {
            request.header("Cookie", cookies);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void realHttpsLoginAndLogoutKeepCredentialsSecureAndCsrfProtected() throws Exception {
        var client = client();
        var login = get(client, "/login", null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.sslSession()).isPresent();
        assertThat(login.headers().firstValue("Strict-Transport-Security")).contains("max-age=0");
        String csrf = cookie(login, "STORE-CSRF");
        assertCookieFlags(login, "STORE-CSRF");
        var authenticated =
                client.send(
                        HttpRequest.newBuilder(https("/login"))
                                .header("Cookie", csrf)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "username=mercury%40gmail.com&password=MercuryLocal2026%21&_csrf="
                                                        + encode(token(login.body()))))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(authenticated.statusCode()).isEqualTo(302);
        assertCookieFlags(authenticated, "store_access");
        assertCookieFlags(authenticated, "store_refresh");
        String credentials =
                cookie(authenticated, "store_access")
                        + "; "
                        + cookie(authenticated, "store_refresh");
        var orders = get(client, "/orders", credentials);
        assertThat(orders.statusCode()).isEqualTo(200);
        var denied =
                client.send(
                        HttpRequest.newBuilder(https("/logout"))
                                .header("Cookie", credentials)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(403);
        var logout =
                client.send(
                        HttpRequest.newBuilder(https("/logout"))
                                .header("Cookie", credentials + "; " + cookie(orders, "STORE-CSRF"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "_csrf=" + encode(token(orders.body()))))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isEqualTo(302);
        assertThat(logout.headers().allValues("Set-Cookie"))
                .anyMatch(
                        value -> value.startsWith("store_access=") && value.contains("Max-Age=0"));
    }

    @Test
    void httpRedirectsOnlyReadsToFixedHttpsOriginAndRejectsWrites() throws Exception {
        int httpPort =
                Arrays.stream(
                                ((TomcatWebServer) context.getWebServer())
                                        .getTomcat()
                                        .getService()
                                        .findConnectors())
                        .filter(connector -> !connector.getSecure())
                        .findFirst()
                        .orElseThrow()
                        .getLocalPort();
        var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        URI address = URI.create("http://127.0.0.1:" + httpPort + "/orders?lang=uk");
        var redirect =
                http.send(
                        HttpRequest.newBuilder(address).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(redirect.statusCode()).isEqualTo(308);
        assertThat(redirect.headers().firstValue("Location"))
                .contains("https://localhost:8443/orders?lang=uk");
        var write =
                http.send(
                        HttpRequest.newBuilder(address)
                                .POST(HttpRequest.BodyPublishers.ofString("private-input"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(write.statusCode()).isEqualTo(400);
        assertThat(write.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(write.body()).doesNotContain("private-input");
    }

    @Test
    void securityEmailsUseConfiguredHttpsOrigin() {
        var links = new SecurityMailLinkFactory("https://localhost:8443");
        assertThat(links.registration("token").toString())
                .isEqualTo("https://localhost:8443/register/verify?token=token");
        assertThat(links.passwordReset("token").toString())
                .startsWith("https://localhost:8443/password/reset?");
    }

    static String token(String html) {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String cookie(HttpResponse<?> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .flatMap(value -> HttpCookie.parse(value).stream())
                .filter(cookie -> name.equals(cookie.getName()) && !cookie.getValue().isEmpty())
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .findFirst()
                .orElseThrow();
    }

    static void assertCookieFlags(HttpResponse<?> response, String name) {
        String header =
                response.headers().allValues("Set-Cookie").stream()
                        .filter(value -> value.startsWith(name + "="))
                        .findFirst()
                        .orElseThrow();
        assertThat(header).contains("Secure", "HttpOnly", "SameSite=Lax");
    }
}
