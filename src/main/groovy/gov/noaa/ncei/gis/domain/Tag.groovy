package gov.noaa.ncei.gis.domain

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.ManyToMany
import groovy.transform.*
import javax.persistence.OneToMany
import javax.persistence.OrderBy


@Entity
@Canonical
class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id

    @Column(nullable = false, unique = true)
    String name

    @ManyToMany(mappedBy = "tags")
    private Set<HealthCheck> healthChecks

    //explicit getters and setters appear to be required for Spring to create instances from JSON
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}