package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.service.HealthCheckService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

import gov.noaa.ncei.gis.domain.*
import gov.noaa.ncei.gis.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 API requirements:
 get a list of healthchecks
 get a single healthcheck
 create a healthcheck
 update a healthcheck
 delete a healthcheck
 get list of tags for a single healthcheck
 add a tag to healthcheck
 remove a tag from healthcheck
 execute a single healthcheck
 execute all healthchecks
 */

//TODO use Groovy logger annotation
@RestController
@RequestMapping("/healthChecks")
class HealthCheckController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final HealthCheckRepository healthCheckRepository
    private final TagRepository tagRepository
    private final HealthCheckService healthCheckService


    @Autowired
    HealthCheckController (HealthCheckRepository healthCheckRepository, TagRepository tagRepository, HealthCheckService) {
        this.healthCheckRepository = healthCheckRepository
        this.tagRepository = tagRepository
        this.healthCheckService = healthCheckService
    }


    @RequestMapping(method = RequestMethod.GET)
    List <HealthCheck> readHealthChecks(
        @RequestParam(value="tag", required = false) String tag,
        @RequestParam(value="failedOnly", defaultValue = "false") Boolean failedOnly) {

        logger.debug("inside readHealthChecks: tag = ${tag}, failedOnly = ${failedOnly}")


        if (! tag && ! failedOnly) {
            println "finding all checks"
            return this.healthCheckRepository.findAll()

        } else if (tag && ! failedOnly) {
            println "finding checks with tag ${tag}"
            return this.healthCheckRepository.findByTagName(tag)

        } else if (! tag && failedOnly) {
            println "finding all failed checks"
            return this.healthCheckRepository.findBySuccessFalse()

        } else if (tag && failedOnly) {
            println "finding failed checks with tag ${tag}"
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
    //@Secured('ROLE_ADMIN')
    ResponseEntity<?> create(@RequestBody HealthCheck input) {

        if (input.tags) {
            //save each Tag instance or replace w/ existing instance to avoid
            // "object references an unsaved transient" exception
            this.persistTags(input)
        }
        this.healthCheckRepository.save(input)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(input.getId()).toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }


    @RequestMapping (value = "/{id}", method = RequestMethod.PUT)
    //@Secured('ROLE_ADMIN')
    ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody HealthCheck input) {
        this.validateHealthCheck(id)

        //TODO validate input

        HealthCheck check = this.healthCheckRepository.findOne(id)

        //TODO introspect instead of hard-code fields?
        check.url = input.url
        check.success = input.success
        this.healthCheckRepository.save(check)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        this.healthCheckRepository.delete(id)
        logger.info("deleted HealthCheck ${id}")
    }


    @RequestMapping (value = "/{id}/run", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    //@Secured('ROLE_ADMIN')
    ResponseEntity<?> execute(@PathVariable("id") Long id) {
        this.validateHealthCheck(id)

        this.healthCheckService.run(id)

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    //TODO accept query params to refine list of checks to run
    @RequestMapping (value = "/run", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    //@Secured('ROLE_ADMIN')
    ResponseEntity<?> executeAll() {
        this.healthCheckService.runChecks()

        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }


    @RequestMapping(value = "/{id}/tags", method = RequestMethod.GET)
    Set<Tag> readTags(@PathVariable Long id) {
        logger.debug("inside readTags: id = ${id}")
        this.validateHealthCheck(id)
        return this.healthCheckRepository.findOne(id).tags
    }


    @RequestMapping(value = "/{id}/tags", method = RequestMethod.POST)
    ResponseEntity<?> addTag(@PathVariable Long id, @RequestBody Tag newTag) {
        logger.debug("inside createTag: id = ${id}")
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
    ResponseEntity<?> deleteTag(@PathVariable Long id, @PathVariable String tagName) {
        logger.debug("inside deleteTag: id = ${id}, tagName = ${tagName}")
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
        //this.healthCheckRepository.findById(id).orElseThrow(new HealthCheckNotFoundException(id))
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
                logger.info("introducing new Tag: ${it.name}...")
                //TODO should we only allow previously created tags to avoid accidental additions?
                persistedTags.add(this.tagRepository.save(it))
            }
        }
        healthCheck.tags = persistedTags
    }
}