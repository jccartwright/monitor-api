package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.Application
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



@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
@WebAppConfiguration
//@TransactionConfiguration(defaultRollback = true)
@Transactional
class HealthCheckControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext


    private RestTemplate restTemplate
    private MockRestServiceServer mockServer
    private MockMvc mockMvc
    private MediaType applicationJsonMediaType =
            new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    @Before
    void setup() {
//        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
//        converters.add(new StringHttpMessageConverter());
//        converters.add(new MappingJackson2HttpMessageConverter());


        this.restTemplate = new RestTemplate()
//        this.restTemplate.setMessageConverters(converters);
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate)
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).apply(springSecurity()).build()
    }

    @Test
    public void testGetAllHealthChecks() {
        this.mockMvc.perform(get("/healthChecks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(applicationJsonMediaType))
                .andExpect(jsonPath('$', hasSize(3)))

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

}
