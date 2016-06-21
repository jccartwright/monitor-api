package gov.noaa.ncei.gis.web

import com.jayway.restassured.http.ContentType
import gov.noaa.ncei.gis.Application
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.RestAssured
import com.jayway.restassured.specification.RequestSpecification;
//import static org.hamcrest.Matchers.*
import org.hamcrest.Matchers
import groovy.json.*


@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
@WebIntegrationTest(randomPort = true)
class HealthCheckControllerTest {
    @Value('${local.server.port}')
    private port;

    @Value('${security.user.password}')
    private password

    @Value('${security.user.name}')
    private username

    private url

    private JsonSlurper jsonSlurper = new JsonSlurper()

    @Before
    void setup() {
        this.url = "http://localhost:${this.port}/healthChecks"
    }

    @Test
    public void testGetHealthChecks() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        ResponseEntity<String> entity = new TestRestTemplate().exchange(
                url, HttpMethod.GET,
                new HttpEntity<Void>(headers), String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        def json = jsonSlurper.parseText(entity.body)
        println json
    }

    @Test
    public void getHealthChecks() {
        final Response response = RestAssured.given()
                .accept(ContentType.JSON)
                .get(url)
        assertThat(response.getStatusCode(), Matchers.equalTo(200))
        //TODO compare JSON content        println response.body
    }

    @Test
    public void testCreateHealthCheckWithValidCredentials() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        ResponseEntity<String> entity = new TestRestTemplate(username, password).exchange(
                url, HttpMethod.POST,
                new HttpEntity<Void>(headers), String.class);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }

    @Test
    public void createHealthCheckWithInvalidCredentials() {
        final Response response = RestAssured.given()
                .auth()
                .preemptive()
                .basic(username, 'badpassword')
                .accept(ContentType.JSON)
                .post(url)
        assertThat(response.getStatusCode(), Matchers.equalTo(401))
    }

    @Test
    public void createHealthCheckWithoutCredentials() {
        final Response response = RestAssured.given()
                .accept(ContentType.JSON)
                .post(url)
        assertThat(response.getStatusCode(), Matchers.equalTo(401))
    }
}
