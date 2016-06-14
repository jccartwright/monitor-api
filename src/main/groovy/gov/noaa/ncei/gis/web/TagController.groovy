package gov.noaa.ncei.gis.web

import gov.noaa.ncei.gis.domain.HealthCheck
import gov.noaa.ncei.gis.domain.HealthCheckRepository
import gov.noaa.ncei.gis.domain.Tag
import gov.noaa.ncei.gis.domain.TagRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/*
 API requirements:
 get list of tags for a single healthcheck
 add a tag to healthcheck
 remove a tag from healthcheck
 get a list of all tags
 */
//TODO use Groovy logger annotation
@RestController
@RequestMapping("/tags")
class TagController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TagRepository tagRepository


    @Autowired
    TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository
    }


    @RequestMapping(method = RequestMethod.GET)
    List <Tag> readTags() {
        logger.debug("inside readTags")
        return null
    }
}