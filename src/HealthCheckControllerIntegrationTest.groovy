package gov.noaa.ncei.gis.web

import ContentType
import Application
import Before
import BeforeClass
import Test
import RunWith
import SpringApplicationConfiguration
import WebIntegrationTest
import SpringJUnit4ClassRunner
import WebAppConfiguration

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Test;
import RunWith;
import static MatcherAssert.assertThat;
import static Assert.assertEquals;

import Value;
import SpringApplicationConfiguration;
import TestRestTemplate;
import WebIntegrationTest;
import HttpEntity;
import HttpHeaders;
import HttpMethod;
import HttpStatus;
import MediaType;
import ResponseEntity;
import DirtiesContext;
import SpringJUnit4ClassRunner;
import LinkedMultiValueMap;
import MultiValueMap;

import static Assert.assertEquals;
import static Assert.assertNotNull;
import static Assert.assertTrue;

import Response;
import RestAssured
import RequestSpecification;
//import static org.hamcrest.Matchers.*
import Matchers
import groovy.json.*


@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
@WebIntegrationTest(randomPort = true)
class HealthCheckControllerIntegrationTest {
    @Value('${my.messageValue}')
    private messageValue

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
        println messageValue
        this.url = "http://localhost:${this.port}/healthChecks"
    }
/*
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
    */
}
