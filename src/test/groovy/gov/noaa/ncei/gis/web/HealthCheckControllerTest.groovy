package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.Application
import gov.noaa.ncei.gis.domain.HealthCheck
import gov.noaa.ncei.gis.domain.HealthCheckRepository
import gov.noaa.ncei.gis.domain.Tag
import gov.noaa.ncei.gis.domain.TagRepository
import gov.noaa.ncei.gis.service.HealthCheckService
import org.springframework.test.web.servlet.result.MockMvcResultHandlers

import javax.servlet.RequestDispatcher
import org.junit.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestTemplate
import org.springframework.http.MediaType
import java.nio.charset.Charset
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.hamcrest.Matchers.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.springframework.security.test.context.support.*
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*
import static org.springframework.restdocs.headers.HeaderDocumentation.*
import static org.springframework.restdocs.request.RequestDocumentation.*


@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
@WebAppConfiguration
//@TransactionConfiguration(defaultRollback = true)
@Transactional
class HealthCheckControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext

    @Autowired
    private HealthCheckRepository healthCheckRepository

    @Autowired
    private TagRepository tagRepository

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
        //bootstrap some data
        bootstrapTags()
        bootstrapArcGISCheck()
        bootstrapWmsCheck()
        bootstrapCatalogCheck()

        this.documentationHandler = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));

        this.restTemplate = new RestTemplate()

        //converters don't seem to be required
        //List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        //converters.add(new StringHttpMessageConverter());
        //converters.add(new MappingJackson2HttpMessageConverter());
        //this.restTemplate.setMessageConverters(converters);

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
    public void errorExample() throws Exception {
        def nonExistentId = 9999

        //throws exception so mock returns null body. When called outside of mock, body contains JSON response
        MvcResult mvcResult = mockMvc.perform(get('/healthChecks/{id}', nonExistentId))
        .andExpect(status().isNotFound())
        //.andDo(MockMvcResultHandlers.print())
        .andReturn();

        assert mvcResult.resolvedException instanceof HealthCheckNotFoundException
    }


    @Test
    public void indexExample() throws Exception {
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
    public void healthChecksListExample() throws Exception {
        this.mockMvc
            .perform(get("/healthChecks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(applicationJsonMediaType))
            .andExpect(jsonPath('$', hasSize(3)))
            .andDo(documentationHandler.document(requestParameters(
                parameterWithName("tag").optional().description("find only checks with this tag"),
                parameterWithName("failedOnly").optional().description("find only checks that are currently failed. Defaults to false")
            )))
    }


    @Test
    public void testGetAllHealthChecks() {
        this.mockMvc.perform(get("/healthChecks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(applicationJsonMediaType))
                .andExpect(jsonPath('$', hasSize(3)))
                .andDo(document("healthChecks"))


        //println this.mockMvc.perform(get("/healthChecks")).andReturn().getResponse().contentAsString
    }


    @Test
//    @WithMockUser(username = 'admin', password = 'mypassword', roles = ['ADMIN'])
    public void testCreateHealthChecks() {
        String jsonBody = '{ "url": "http://www.example.com" }'

        MvcResult mvcResult = mockMvc.perform(post("/healthChecks")
                .with(httpBasic('admin','mypassword'))
                .content(jsonBody).contentType(applicationJsonMediaType))
                .andExpect(status().isCreated())
                .andReturn()

        assert mvcResult.getResponse().getHeader('Location')

        mockMvc.perform(get("/healthChecks"))
                .andExpect(jsonPath('$', hasSize(4)))
    }


    @Test
    public void testCreateHealthChecksWithoutCredentials() {
        String jsonBody = '{ "url": "http://www.example.com" }'

        mockMvc.perform(post("/healthChecks")
                .content(jsonBody).contentType(applicationJsonMediaType))
                .andExpect(status().isUnauthorized())

        mockMvc.perform(get("/healthChecks"))
                .andExpect(jsonPath('$', hasSize(3)))
    }


    @Test
    public void testCreateHealthChecksWithInvalidCredentials() {
        String jsonBody = '{ "url": "http://www.example.com" }'

        mockMvc.perform(post("/healthChecks")
                .with(httpBasic('admin','badpassword'))
                .content(jsonBody).contentType(applicationJsonMediaType))
                .andExpect(status().isUnauthorized())

        mockMvc.perform(get("/healthChecks"))
                .andExpect(jsonPath('$', hasSize(3)))
    }

    void bootstrapTags() {
        tagRepository.save(new Tag(name:'boulder'))
        tagRepository.save(new Tag(name:'arcgis'))
        tagRepository.save(new Tag(name:'wms'))
    }

    void bootstrapArcGISCheck() {
        def boulderTag = tagRepository.findByName('boulder')
        def arcgisTag = tagRepository.findByName('arcgis')

        def healthCheck = new HealthCheck(url:"http://maps.ngdc.noaa.gov/arcgis/rest/services/web_mercator/etopo1_hillshade/MapServer/export?bbox=-120,0,-60,60&bboxSR=4326&format=png&transparent=false&f=image")
        Set<Tag> tags = new HashSet()
        tags.add(boulderTag)
        tags.add(arcgisTag)
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

        def healthCheck = new HealthCheck(url: "http://maps.ngdc.noaa.gov/arcgis/rest/services?f=json")
        tags = new HashSet()
        tags.add(boulderTag)
        healthCheck.tags = tags
        healthCheckRepository.save(healthCheck)
    }

}
