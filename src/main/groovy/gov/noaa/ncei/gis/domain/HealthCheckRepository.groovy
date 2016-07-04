package gov.noaa.ncei.gis.domain

import org.springframework.stereotype.Repository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.jpa.repository.*

@Repository
interface HealthCheckRepository extends CrudRepository <HealthCheck, Long> {
    //find all checks with the given tagname
    @Query("select h from HealthCheck h join h.tags t where t.name in (?1)")
    List<HealthCheck> findByTagName(String tagName)

    @Query("select h from HealthCheck h join h.tags t where t.name in (?1) and success = 'false'")
    List<HealthCheck> findByTagNameAndSuccessFalse(String tagName)

    //find all checks currently in failed state
    List<HealthCheck> findBySuccessFalse()

    //find all checks w/o any tag
    List<HealthCheck> findByTagsIsNull()

    List<HealthCheck> findByCheckInterval(CheckIntervalEnum checkInterval)
}
