package gov.noaa.ncei.gis.domain

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

    String url

    Boolean success

    @ManyToMany
    private Set<Tag> tags

//    @OneToMany(cascade=CascadeType.ALL)
//    @JoinColumn(name="id")
//    private Set<Tag> tags

    //explicit getters and setters appear to be required for Spring to create instances from JSON
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public Set<Tag> getTags() {
        return tags;
    }
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

}