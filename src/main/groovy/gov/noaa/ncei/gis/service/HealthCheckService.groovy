package gov.noaa.ncei.gis.service

import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import gov.noaa.ncei.gis.domain.*
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream


@Log4j
@Service
class HealthCheckService {
    private HealthCheckRepository healthCheckRepository

    @Autowired
    HealthCheckService(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository
    }


    void runCheck(Long id) {
        HealthCheck check = healthCheckRepository.findOne(id)
        if (check) {
            runCheck(check)
        } else {
            throw new IllegalArgumentException("No HealthCheck with id ${id}")
        }
    }


    void runCheck(HealthCheck healthCheck) {
        log.debug "running check for HealthCheck ${healthCheck.id}..."

        URL url = new URL(healthCheck.getUrl())
        log.debug "checking ${url}..."

        def result = getUrl(url)

        //update HealthCheck state
        healthCheck.lastUpdated = new Date()
        healthCheck.success = result.success

        //potentially override lastSuccess if fail checksum comparison
        if (healthCheck.responseChecksum && healthCheck.success && healthCheck.responseChecksum != result.md5) {
            log.warn("successful URL connection but failed to match MD5")
            healthCheck.success = false
        }

        if (result.elapsedTime) {
            healthCheck.lastResponseTimeInMs = result.elapsedTime
        }
        if (result.response) {
            healthCheck.lastResponse = result.response
            healthCheck.responseContentType = result.responseContentType
            //HACK - work around incorrect contenttype in ArcGIS JSON response
            if (healthCheck.url ==~ /.*f=[json|pjson].*/) {
                healthCheck.responseContentType = 'application/json'
            }
        } else {
            healthCheck.lastResponse = null
            healthCheck.responseContentType = null
        }
        healthCheck.checkCount++
        if (healthCheck.success) {
            healthCheck.successfulCheckCount++
        }

        healthCheckRepository.save(healthCheck)
    }


    void runChecks(List<HealthCheck> checks) {
        if (checks.size() == 0) {
            log.warn "Empty list of HeathChecks received"
            return
        }
        checks.each {
            runCheck(it.id)
        }
    }


    def getUrl(URL url) {
        def result = [:]
        try {
            def startTime = new Date()
            def conn = url.openConnection()
            conn.setReadTimeout(60*1000)
            conn.setConnectTimeout(5*1000)
            def bytes = conn.inputStream.bytes
            def endTime = new Date()
            result.elapsedTime = endTime.time - startTime.time

            if (conn.responseCode != 200) {
                result.success = false
                log.warn "ERROR on ${url}. ${conn.responseCode}: ${conn.responseMessage}"
            } else {
                result.success = true
                result.size = bytes.size()
                result.md5 = getMd5(bytes)
                result.response = bytes
                result.responseContentType = conn.getContentType().split(';')[0]
                log.debug "read ${bytes.size()} bytes from ${url}"
            }
        } catch (Exception e) {
            result.success = false
            log.warn "Exception reading URL ${url}\nException: ${e}"
        }

        return result
    }


    def getMd5(bytes) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(bytes);
        new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
    }


    //taken from https://community.oracle.com/thread/1263871
    String determineImageFormat( byte[] imageBytes ) throws IOException {
        final ByteArrayInputStream bStream = new ByteArrayInputStream( imageBytes )

        final ImageInputStream imgStream = ImageIO.createImageInputStream( bStream )
        final Iterator<ImageReader> iter = ImageIO.getImageReaders( imgStream )

        final ImageReader imgReader = iter.next()

        return imgReader.getFormatName()
    }
}
