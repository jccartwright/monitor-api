package gov.noaa.ncei.gis.web

import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

import gov.noaa.ncei.gis.domain.*
import gov.noaa.ncei.gis.service.*
import static gov.noaa.ncei.gis.domain.CheckIntervalEnum.*

@Log4j
@RestController
@RequestMapping("/healthChecks")
class HealthCheckController {
    private final HealthCheckRepository healthCheckRepository
    private final TagRepository tagRepository
    private final HealthCheckService healthCheckService


    @Autowired
    HealthCheckController (HealthCheckRepository healthCheckRepository, TagRepository tagRepository, HealthCheckService healthCheckService) {
        this.healthCheckRepository = healthCheckRepository
        this.tagRepository = tagRepository
        this.healthCheckService = healthCheckService
    }


    @RequestMapping(method = RequestMethod.GET)
    List <HealthCheck> readHealthChecks(
        @RequestParam(value="tag", required = false) String tag,
        @RequestParam(value="failedOnly", defaultValue = "false") Boolean failedOnly) {

        log.debug("inside readHealthChecks: tag = ${tag}, failedOnly = ${failedOnly}")

        if (! tag && ! failedOnly) {
            log.debug "finding all checks..."
            return this.healthCheckRepository.findAll()

        } else if (tag && ! failedOnly) {
            log.debug "finding checks with tag ${tag}"
            return this.healthCheckRepository.findByTagName(tag)

        } else if (! tag && failedOnly) {
            log.debug "finding all failed checks"
            return this.healthCheckRepository.findBySuccessFalse()

        } else if (tag && failedOnly) {
            log.debug "finding failed checks with tag ${tag}"
            return this.healthCheckRepository.findByTagNameAndSuccessFalse(tag)
        } else {
            throw new IllegalStateException("Unexpected condition in readHealthChecks")
        }
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    HealthCheck readHealthCheck(@PathVariable Long id) {
        this.validateHealthCheck(id)

        return this.healthCheckRepository.findOne(id)
    }


    @RequestMapping (method = RequestMethod.POST)
    @Secured('ROLE_ADMIN')
    ResponseEntity<?> create(@RequestBody HealthCheck input) {

        if (input.tags) {
            //save each Tag instance or replace w/ existing instance to avoid
            // "object references an unsaved transient" exception
            this.persistTags(input)
        }


        //default checkInterval if not provided
        if (! input.checkInterval) { input.checkInterval = HOURLY }

        this.healthCheckRepository.save(input)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder
            .fromCurrentRequest().path("/{id}")
            .buildAndExpand(input.getId()).toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }


    @RequestMapping (value = "/{id}", method = RequestMethod.PUT)
    @Secured('ROLE_ADMIN')
    ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody HealthCheck input) {
        this.validateHealthCheck(id)

        //TODO validate URL?

        HealthCheck check = this.healthCheckRepository.findOne(id)

        println (input.tags)
        //TODO update tags? alternative to adding/removing tags one at a time?
        if (input.tags) {
            //save each Tag instance or replace w/ existing instance to avoid
            // "object references an unsaved transient" exception
            this.persistTags(input)
        }

        //url, tags, checkInterval only relevant information to update
        check.checkInterval = input.checkInterval
        check.url = input.url
        check.tags = input.tags
        this.healthCheckRepository.save(check)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured('ROLE_ADMIN')
    void delete(@PathVariable Long id) {
        this.validateHealthCheck(id)

        this.healthCheckRepository.delete(id)
        log.info("deleted HealthCheck ${id}")
    }


    @RequestMapping (value = "/{id}/run", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @Secured('ROLE_ADMIN')
    ResponseEntity<?> run(@PathVariable("id") Long id) {
        this.validateHealthCheck(id)

        this.healthCheckService.runCheck(id)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    //TODO handle asynchronously
    @RequestMapping(value = "/run", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured('ROLE_ADMIN')
    runChecks(
            @RequestParam(value="tag", required = false) String tag,
            @RequestParam(value="failedOnly", defaultValue = "false") Boolean failedOnly) {

        if (! tag && ! failedOnly) {
            log.debug "running checks on all services..."
            healthCheckService.runChecks(healthCheckRepository.findAll())

        } else if (tag && ! failedOnly) {
            log.debug "running checks on all services with tag ${tag}..."
            healthCheckService.runChecks(healthCheckRepository.findByTagName(tag))

        } else if (! tag && failedOnly) {
            log.debug "running checks on all failed services..."
            healthCheckService.runChecks(healthCheckRepository.findBySuccessFalse())

        } else if (tag && failedOnly) {
            log.debug "running checks on failed services with with tag ${tag}..."
            healthCheckService.runChecks(healthCheckRepository.findByTagNameAndSuccessFalse(tag))
        } else {
            throw new IllegalStateException("Unexpected condition in runChecks")
        }
    }


    //TODO how to best handle JSON, XML responses
    @RequestMapping(value = "/{id}/lastResponse", method = RequestMethod.GET)
    ResponseEntity<byte[]> getLastResponse(@PathVariable Long id) {
        this.validateHealthCheck(id)

        HealthCheck check = healthCheckRepository.findOne(id)
        if (! check.lastResponse) {
            throw new HealthCheckLastResponseNotFoundException()
        }

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setContentLength(check.lastResponse.size())
        def contentType = check.responseContentType.split('/')
        httpHeaders.setContentType(new MediaType(contentType[0], contentType[1]))
        return new ResponseEntity<>(check.lastResponse, httpHeaders, HttpStatus.OK);
    }


    @RequestMapping(value = "/{id}/tags", method = RequestMethod.GET)
    Set<Tag> readTags(@PathVariable Long id) {
        log.debug("inside readTags: id = ${id}")
        this.validateHealthCheck(id)
        return this.healthCheckRepository.findOne(id).tags
    }


    @RequestMapping(value = "/{id}/tags", method = RequestMethod.POST)
    @Secured('ROLE_ADMIN')
    ResponseEntity<?> addTag(@PathVariable Long id, @RequestBody Tag newTag) {
        log.debug("inside addTag: id = ${id}")
        this.validateHealthCheck(id)

        //TODO validate Tag passed in

        //check whether Tag already associated w/ this check
        HealthCheck check = this.healthCheckRepository.findOne(id)

        def alreadyContainsTag = check.tags.any { existingTag ->
           existingTag.name ==  newTag.name
        }

        if (! alreadyContainsTag) {
            check.tags.add(this.findOrCreateTag(newTag))
        }

        this.healthCheckRepository.save(check)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    @RequestMapping(value = "/{id}/tags/{tagName}", method = RequestMethod.DELETE)
    @Secured('ROLE_ADMIN')
    ResponseEntity<?> deleteTag(@PathVariable Long id, @PathVariable String tagName) {
        log.debug("inside deleteTag: id = ${id}, tagName = ${tagName}")
        this.validateHealthCheck(id)

        HealthCheck check = this.healthCheckRepository.findOne(id)
        check.tags.removeIf { it.name == tagName }

        this.healthCheckRepository.save(check)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    private Tag findOrCreateTag(Tag tag) {
        Tag foundTag = this.tagRepository.findByName(tag.name)
        if (! foundTag) {
            foundTag = this.tagRepository.save(tag)
        }
        return foundTag
    }


    private void validateHealthCheck(Long id) {
        if (! this.healthCheckRepository.findOne(id)) {
            throw(new HealthCheckNotFoundException(id))
        }
    }


    //WARNING: mutates passed instance
    private void persistTags(HealthCheck healthCheck) {
        Set<Tag> persistedTags = new HashSet()

        healthCheck.tags.each {
            Tag tag = this.tagRepository.findByName(it.name)
            if (tag) {
                //add pre-existing instance
                persistedTags.add(tag)
            } else {
                log.info("introducing new Tag: ${it.name}...")
                //TODO should we only allow previously created tags to avoid accidental additions?
                persistedTags.add(this.tagRepository.save(it))
            }
        }
        healthCheck.tags = persistedTags
    }
}