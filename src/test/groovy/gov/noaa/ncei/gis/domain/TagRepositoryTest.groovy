package gov.noaa.ncei.gis.domain

import gov.noaa.ncei.gis.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import javax.transaction.Transactional

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = Application)
//@WebAppConfiguration
@Transactional
class TagRepositoryTest {
    @Autowired
    TagRepository repository

    @Test
    void testFindAll() {
        def results = repository.findAll()
        assert results.size() == 7
        results.each {
            println it
        }
    }

    @Test
    void testSave() {
        repository.save(new Tag(name: 'testme'))

        def allTags = repository.findAll()
        assert allTags.size() == 8
    }

    @Test
    void testFindByName() {
        repository.save(new Tag(name: 'testme'))
        def tag = repository.findByName("testme")
        assert tag
    }
}
