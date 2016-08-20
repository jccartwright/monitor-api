package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.Application
import gov.noaa.ncei.gis.domain.HealthCheck
import gov.noaa.ncei.gis.domain.HealthCheckRepository
import gov.noaa.ncei.gis.domain.Tag
import gov.noaa.ncei.gis.domain.TagRepository
import gov.noaa.ncei.gis.service.HealthCheckService
import org.springframework.beans.factory.annotation.Value

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.security.test.context.support.*


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

    @Value('${security.user.name}')
    private String username
    @Value('${security.user.password}')
    private String password

    @Before
    void setup() {
        //bootstrap some data
        bootstrapTags()
        bootstrapArcGISCheck()
        bootstrapWmsCheck()
        bootstrapCatalogCheck()

        this.restTemplate = new RestTemplate()

        //converters don't seem to be required
        //List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        //converters.add(new StringHttpMessageConverter());
        //converters.add(new MappingJackson2HttpMessageConverter());
        //this.restTemplate.setMessageConverters(converters);

        this.mockServer = MockRestServiceServer.createServer(this.restTemplate)

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
            .apply(springSecurity())
            .build();
    }


    @Test
    void exceptionShouldBeThrownWhenInvalidIdRequested() {
        def nonExistentId = 9999

        //throws exception so mock returns null body. When called outside of mock, body contains JSON response
        MvcResult mvcResult = mockMvc.perform(get('/healthChecks/{id}', nonExistentId))
        .andExpect(status().isNotFound())
        //.andDo(MockMvcResultHandlers.print())
        .andReturn();

        assert mvcResult.resolvedException instanceof HealthCheckNotFoundException
    }


    @Test
    void listAllHealthChecks() {
        this.mockMvc.perform(get("/healthChecks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(applicationJsonMediaType))
            .andExpect(jsonPath('$', hasSize(3)))
    }


    @Test
    void createHealthCheck() {
        String jsonBody = '{ "url":  "http://www.example.com" }'

        String checkLocation = mockMvc.perform(post("/healthChecks")
                .with(httpBasic(username, password))
                .content(jsonBody).contentType(applicationJsonMediaType))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location")

        assert checkLocation

        //test that newly created HealthCheck can be retrieved
        mockMvc.perform(get(checkLocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("url", is('http://www.example.com')))
            //.andDo(print())

        //test that one more HealthCheck exists than was bootstrapped
        mockMvc.perform(get("/healthChecks"))
                .andExpect(jsonPath('$', hasSize(4)))
    }


    @Test
    void getIndividualHealthCheck() {

        //first create a new check to be retrieved...
        String jsonBody = '{ "url":  "http://www.example.com" }'
        String checkLocation = mockMvc.perform(post("/healthChecks")
                .with(httpBasic(username, password))
                .content(jsonBody).contentType(applicationJsonMediaType))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location")
        assert checkLocation

        //then test and document the retrieval
        mockMvc.perform(get(checkLocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("url", is('http://www.example.com')))
            //.andDo(print())
    }


    @Test
    void shouldFailWhenCreateHealthChecksWithoutCredentials() {
        String jsonBody = '{ "url": "http://www.example.com" }'

        mockMvc.perform(post("/healthChecks")
                .content(jsonBody).contentType(applicationJsonMediaType))
                .andExpect(status().isUnauthorized())

        //test that still have same number of HealthChecks as were bootstrapped
        mockMvc.perform(get("/healthChecks"))
                .andExpect(jsonPath('$', hasSize(3)))
    }


    @Test
    void shouldFailWhenCreateHealthChecksWithInvalidCredentials() {
        String jsonBody = '{ "url": "http://www.example.com" }'

        mockMvc.perform(post("/healthChecks")
            .with(httpBasic(username,'badpassword'))
            .content(jsonBody).contentType(applicationJsonMediaType))
            .andExpect(status().isUnauthorized())

        //test that still have same number of HealthChecks as were bootstrapped
        mockMvc.perform(get("/healthChecks"))
            .andExpect(jsonPath('$', hasSize(3)))
    }


    @Test
    void executeAllHealthChecks() {
        this.mockMvc.perform(post("/healthChecks/run")
                .with(httpBasic(username, password)))
            .andExpect(status().isNoContent())
    }

//TODO delete check
//TODO update check
//TODO add tag
//TODO remove tag
//TODO get list of tags for a single healthcheck
//TODO add a tag to healthcheck
//TODO remove a tag from healthcheck
//TODO execute a single healthcheck

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
