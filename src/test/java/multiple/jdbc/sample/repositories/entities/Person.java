package multiple.jdbc.sample.repositories.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("T_PERSON")
public class Person {
    @Id
    private Long id;

    private String name;

    @Version
    private Long version;

    @Column("COMPANY_ID")
    private AggregateReference<Company, Long> company;

    public Person() {
        /**
         * 
         */
    }

    public Person(String name) {
        this.name = name;
    }

    public Person(Long id, String name) {
        this.id = id;
        this.name = name;
    }

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

    public Long getCompanyId() {
        return this.company.getId();
    }

    public void setCompanyId(Long companyId) {
        this.company = AggregateReference.to(companyId);
    }
}
