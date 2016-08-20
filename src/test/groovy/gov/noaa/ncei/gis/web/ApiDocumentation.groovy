package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.Application
import gov.noaa.ncei.gis.domain.HealthCheck
import gov.noaa.ncei.gis.domain.HealthCheckRepository
import gov.noaa.ncei.gis.domain.Tag
import gov.noaa.ncei.gis.domain.TagRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.http.MediaType
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.Charset
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*
import static org.springframework.restdocs.payload.PayloadDocumentation.*
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
@WebAppConfiguration
//@TransactionConfiguration(defaultRollback = true)
@Transactional
class ApiDocumentation {
    @Autowired
    private WebApplicationContext webApplicationContext

    @Autowired
    private HealthCheckRepository healthCheckRepository

    @Autowired
    private TagRepository tagRepository

    @Value('${security.user.name}')
    private String username
    @Value('${security.user.password}')
    private String password

    private RestTemplate restTemplate
    private MockRestServiceServer mockServer
    private MockMvc mockMvc
    private MediaType applicationJsonMediaType =
            new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"))

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler documentationHandler;

    @Before
    void setup() {
        println "username = ${username}"
        println "password = ${password}"

        //bootstrap some data
//        bootstrapTags()
//        bootstrapArcGISCheck()
//        bootstrapWmsCheck()
//        bootstrapCatalogCheck()

        this.documentationHandler = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));

        this.restTemplate = new RestTemplate()

        this.mockServer = MockRestServiceServer.createServer(this.restTemplate)

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(this.documentationHandler)
                .apply(springSecurity())
                .build();
    }


    @Test
    public void headersExample() throws Exception {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk())
            .andDo(this.documentationHandler.document(
                responseHeaders(
                    headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `${applicationJsonMediaType}`")
                )
            ))
    }


    @Test
    void errorExample() {
        def nonExistentId = 9999

        //throws exception so mock returns null body. When called outside of mock, body contains JSON response
        MvcResult mvcResult = mockMvc.perform(get('/healthChecks/{id}', nonExistentId))
        .andExpect(status().isNotFound())
        //.andDo(MockMvcResultHandlers.print())
        .andReturn();

        assert mvcResult.resolvedException instanceof HealthCheckNotFoundException
    }


    @Test
    void indexExample() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andDo(this.documentationHandler.document(
                links(
                    linkWithRel("healthChecks").description("The <<resources-healthChecks,HealthChecks resource>>"),
                    linkWithRel("tags").description("The <<resources-tags,Tags resource>>"),
                    linkWithRel("profile").description("The <<resources-profile, Application-level profile semantics resource>>")),
                responseFields(
                    fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"))
            ))
    }


    @Test
    void healthChecksListExample() {
        //bootstrap some data
        bootstrapTags()
        bootstrapArcGISCheck()

        this.mockMvc
            .perform(get("/healthChecks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(applicationJsonMediaType))
            .andExpect(jsonPath('$', hasSize(1)))
            .andDo(documentationHandler.document(requestParameters(
                parameterWithName("tag").optional().description("find only checks with this tag"),
                parameterWithName("failedOnly").optional().description("find only checks that are currently failed. Defaults to false")
            )))
    }


    @Test
    void healthChecksRunExample() {
        this.mockMvc
                .perform(post("/healthChecks/run")
                    .with(httpBasic(username, password)))
                .andExpect(status().isNoContent())
                .andDo(documentationHandler.document())
    }


    @Test
    void healthChecksCreateExample() {
        String jsonBody = '{ "url":  "http://www.example.com", "tags": ["sample","demo"] }'

        String checkLocation = mockMvc
            .perform(
                post("/healthChecks")
                .with(httpBasic(username, password))
                .content(jsonBody).contentType(applicationJsonMediaType))
            .andExpect(status().isCreated())
            .andDo(documentationHandler.document(requestFields(
                fieldWithPath("url").description("URL to monitor"),
                fieldWithPath("tags").type("List").optional().description("list of tag names to associate with this HealthCheck"),
                fieldWithPath("checkInterval").type("String").optional().description("frequency with which to check the URL: FIVEMINUTES, FIFTEENMINUTES, HOURLY, DAILY, WEEKLY")
            )))
            .andReturn().getResponse().getHeader("Location")

        assert checkLocation
    }


    @Test
    void healthCheckGetExample() {
        //first create a new check to be retrieved...
        String jsonBody = '{ "url":  "http://www.example.com" }'
        String checkLocation = mockMvc
                .perform(
                post("/healthChecks")
                    .with(httpBasic(username, password))
                    .content(jsonBody).contentType(applicationJsonMediaType))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location")
        assert checkLocation

        //then test and document the retrieval
        mockMvc.perform(get(checkLocation))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("url", is('http://www.example.com')))
            //.andDo(print())
            .andDo(documentationHandler.document(responseFields(
                fieldWithPath("id").description("Unique Id for this HealthCheck"),
                fieldWithPath("url").description("URL to be monitored"),
                fieldWithPath("lastResponseTimeInMs").type("Number").description("number of milliseconds it took to receive last response"),
                fieldWithPath("responseContentType").type("String").description("contentType of the last response"),
                fieldWithPath("checkCount").type("Number").description("total number of times this HealthCheck has been run"),
                fieldWithPath("successfulCheckCount").type("Number").description("total number of times this HealthCheck responded successfully"),
                fieldWithPath("checkInterval").type("Number").description("interval on which this HealthCheck is run"),
                fieldWithPath("lastUpdated").type("Number").description("time of last run (in ms since epoch)"),
                fieldWithPath("success").type("Boolean").description("whether last run returned successfully"),
                fieldWithPath("tags").type("List").description("list of tags associated with this HealthCheck"),
                fieldWithPath("percentSuccessful").type("Number").description("percentage of the runs that were successful")
        )))
    }


    @Test
    void healthCheckDeleteExample() {
        String jsonBody = '{ "url":  "http://www.example.com" }'

        //first create a new record to delete...
        String checkLocation = mockMvc
            .perform(
            post("/healthChecks")
                .with(httpBasic(username, password))
                .content(jsonBody).contentType(applicationJsonMediaType))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location")
        assert checkLocation

        //...then delete it
        mockMvc.perform(delete(checkLocation).with(httpBasic(username, password)))
            .andExpect(status().isNoContent())
            .andDo(documentationHandler.document())
    }

//TODO update check
//TODO remove tag
//TODO get list of tags for a single healthcheck
//TODO add a tag to healthcheck
//TODO remove a tag from healthcheck
//TODO execute a single healthcheck
//TODO get lastResult for healthcheck

    /*
     * helper methods below this point
     */
    void bootstrapTags() {
        tagRepository.save(new Tag(name:'boulder'))
        tagRepository.save(new Tag(name:'arcgis'))
        tagRepository.save(new Tag(name:'wms'))
    }

    void bootstrapArcGISCheck() {
        def boulderTag = tagRepository.findByName('boulder')
        def arcgisTag = tagRepository.findByName('arcgis')
        Set<Tag> tags = new HashSet()
        tags.add(boulderTag)
        tags.add(arcgisTag)

        def healthCheck = new HealthCheck(url:"http://maps.ngdc.noaa.gov/arcgis/rest/services/web_mercator/etopo1_hillshade/MapServer/export?bbox=-120,0,-60,60&bboxSR=4326&format=png&transparent=false&f=image")
        healthCheck.tags = tags
        healthCheckRepository.save(healthCheck)
    }

    void bootstrapWmsCheck() {
        def boulderTag = tagRepository.findByName('boulder')
        def wmsTag = tagRepository.findByName('wms')
        Set<Tag> tags = new HashSet()
        tags.add(boulderTag)
        tags.add(wmsTag)

        def healthCheck = new HealthCheck(url:"http://maps.ngdc.noaa.gov/arcgis/services/etopo1/MapServer/WMSServer?request=GetCapabilities&service=WMS")
        healthCheck.tags = tags
        healthCheckRepository.save(healthCheck)
    }

    void bootstrapCatalogCheck() {
        def boulderTag = tagRepository.findByName('boulder')
        Set<Tag> tags = new HashSet()
        tags = new HashSet()
        tags.add(boulderTag)

        def healthCheck = new HealthCheck(url: "http://maps.ngdc.noaa.gov/arcgis/rest/services?f=json")
        healthCheck.tags = tags
        healthCheckRepository.save(healthCheck)
    }
}
