package multiple.jdbc.sample.repositories.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("T_COMPANY")
public class Company {

    @Id
    private Long id;
    private String name;

    @MappedCollection(idColumn = "COMPANY_ID")
    private Set<Person> persons = new HashSet<>();

    public Company() {
        /**
         * 
         */
    }

    public Company(String name) {
        this.name = name;
    }

    public Company(Long id, String name) {
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

    public void addPerson(Person person) {
        this.persons.add(person);
    }

    public Set<Person> getPersons() {
        return Collections.unmodifiableSet(this.persons);
    }

}
