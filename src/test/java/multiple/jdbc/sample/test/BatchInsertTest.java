package multiple.jdbc.sample.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import multiple.jdbc.sample.repositories.entities.Company;
import multiple.jdbc.sample.repositories.entities.Person;
import multiple.jdbc.sample.repositories.primary.CompanyRepository;
import multiple.jdbc.sample.repositories.primary.PersonRepository;

@SpringBootTest
class BatchInsertTest {
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    @Qualifier("primaryNamedParameterJdbcTemplate")
    private NamedParameterJdbcOperations primaryJdbcOperations;

    @BeforeEach
    void init() {
        primaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_person");
        primaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_company");

        primaryJdbcOperations.getJdbcOperations().execute(
                "create table t_person (ID bigint not null auto_increment primary key, NAME varchar(256), COMPANY_ID bigint, VERSION bigint)");
        primaryJdbcOperations.getJdbcOperations().execute(
                "create table t_company (ID bigint not null auto_increment primary key, NAME varchar(256))");
    }

    @Test
    void test_GeneratedId() {
        List<Person> persons = new ArrayList<>();
        Person person = new Person("ziva1");
        persons.add(person);
        person = new Person("ziva2");
        persons.add(person);

        personRepository.batchSave(persons);

        assertEquals(2, personRepository.count());

        Optional<Person> personOptional = personRepository.findById(1L);
        assertTrue(personOptional.isPresent());
    }

    @Test
    void test_ProviderId() {
        List<Person> persons = new ArrayList<>();
        Person person = new Person(5L, "ziva5");
        persons.add(person);
        person = new Person(6L, "ziva6");
        persons.add(person);

        personRepository.batchSave(persons);

        assertEquals(2, personRepository.count());

        Optional<Person> personOptional = personRepository.findById(5L);
        assertTrue(personOptional.isPresent());
    }

    @Test
    void test_invalidEntitiesSize() {
        assertThrows(IllegalArgumentException.class, () -> personRepository.batchSave(null));

        List<Person> persons = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> personRepository.batchSave(persons));
    }

    @Test
    void test_invalidBachSize() {
        List<Person> persons = new ArrayList<>();
        Person person = new Person(5L, "ziva5");
        persons.add(person);

        assertThrows(IllegalArgumentException.class, () -> personRepository.batchSave(persons, -1));
    }

    @Test
    void test_largeSize() {
        List<Person> persons = new ArrayList<>();
        for (long i = 1; i < 10; ++i) {
            Person person = new Person("ziva" + i);
            persons.add(person);
        }
        personRepository.batchSave(persons, 5);
        assertEquals(9, personRepository.count());
    }

    @Test
    void test_mixSave() {
        Person person = new Person("ziva");
        personRepository.save(person);
        assertEquals(1, personRepository.count());
        assertEquals(1L, person.getId());
        assertEquals("ziva", person.getName());

        List<Person> persons = new ArrayList<>();
        persons.add(person);
        person.setName("ziva1");

        for (long i = 2; i < 10; ++i) {
            person = new Person("ziva" + i);
            persons.add(person);
        }
        personRepository.batchSave(persons, 5);
        assertEquals(9, personRepository.count());

        Optional<Person> personOptional = personRepository.findById(1L);
        assertTrue(personOptional.isPresent());
        assertEquals("ziva1", personOptional.get().getName());
    }

    @Test
    void test_noVersion() {
        List<Company> companies = new ArrayList<>();
        for (long i = 1; i < 10; ++i) {
            Company company = new Company("ziva" + i);
            companies.add(company);
        }
        companyRepository.batchSave(companies, 5);
        assertEquals(9, companyRepository.count());
    }
}
