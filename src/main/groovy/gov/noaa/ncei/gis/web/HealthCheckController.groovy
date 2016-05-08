package gov.noaa.ncei.gis.web

import org.springframework.http.HttpStatus
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import gov.noaa.ncei.gis.domain.*


@RestController
class HealthCheckController {
    @RequestMapping(value = "/healthChecks", method = RequestMethod.GET)
    public List<HealthCheck> findByTag(@RequestParam(value="tag", required = false) String tag) {
        println "finding HealthChecks with tag ${tag}..."
        return [];
    }

    @RequestMapping (value = "/healthChecks", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @Secured('ROLE_ADMIN')
    public HealthCheck create() {
        println "adding new HealthCheck..."
        return null
    }

    @RequestMapping (value = "/healthChecks/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @Secured('ROLE_ADMIN')
    public HealthCheck update(@PathVariable("id") final Long id) {
        println "updating HealthCheck..."
        return null
    }

    @RequestMapping (value = "/healthChecks/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured('ROLE_ADMIN')
    public HealthCheck delete(@PathVariable("id") final Long id) {
        println "deleting HealthCheck ${id}..."
        return null
    }

}