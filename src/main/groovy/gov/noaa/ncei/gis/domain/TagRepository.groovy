package gov.noaa.ncei.gis.domain

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository extends CrudRepository<Tag, Long> {
    Tag findByName(String name)
}
