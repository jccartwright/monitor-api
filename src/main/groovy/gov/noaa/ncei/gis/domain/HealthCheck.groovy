package gov.noaa.ncei.gis.domain

import com.fasterxml.jackson.annotation.JsonIgnore

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.*


@Entity
class HealthCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id

    //TODO validate as valid URL
    String url

    Double lastResponseTimeInMs

    @JsonIgnore
    String responseChecksum

    @JsonIgnore
    byte[] lastResponse

    String responseContentType

    //number of times this check has been run
    Integer checkCount = 0
    Integer successfulCheckCount = 0

    //TODO should be enum
    CheckIntervalEnum checkInterval = CheckIntervalEnum.DAILY

    Date lastUpdated

    //status of most recent check
    Boolean success

    @ManyToMany
    private Set<Tag> tags

//    @OneToMany(cascade=CascadeType.ALL)
//    @JoinColumn(name="id")
//    private Set<Tag> tags

    //explicit getters and setters appear to be required for Spring to create instances from JSON
    Long getId() {
        return id;
    }
    void setId(Long id) {
        this.id = id;
    }
    String getUrl() {
        return url;
    }
    void setUrl(String url) {
        this.url = url;
    }
    Set<Tag> getTags() {
        return tags;
    }
    void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    String getResponseContentType() {
        return responseContentType
    }

    Integer getPercentSuccessful() {
        if (checkCount && successfulCheckCount) {
            return ((successfulCheckCount / checkCount) * 100) as Integer
        } else {
            return 0
        }
    }
}