package multiple.jdbc.sample.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Streams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import multiple.jdbc.sample.repositories.entities.Company;
import multiple.jdbc.sample.repositories.entities.Person;
import multiple.jdbc.sample.repositories.primary.CompanyRepository;
import multiple.jdbc.sample.repositories.primary.PersonRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepositoryTest {
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    @Qualifier("primaryNamedParameterJdbcTemplate")
    private NamedParameterJdbcOperations primaryJdbcOperations;

    @Autowired
    @Qualifier("secondaryNamedParameterJdbcTemplate")
    private NamedParameterJdbcOperations secondaryJdbcOperations;

    @BeforeAll
    void init() {
        primaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_person");
        primaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_company");

        primaryJdbcOperations.getJdbcOperations().execute(
                "create table t_person (ID bigint not null auto_increment primary key, NAME varchar(256), COMPANY_ID bigint, VERSION bigint)");
        primaryJdbcOperations.getJdbcOperations().execute(
                "create table t_company (ID bigint not null auto_increment primary key, NAME varchar(256))");
    }

    @Test
    @Transactional
    @Rollback(false)
    @Order(1)
    void testBase() {
        Person person = new Person("ziva1");
        Company company = new Company("global1");

        company.addPerson(person);
        companyRepository.save(company);

        Person fetchPerson = personRepository.findById(1L).get();
        assertEquals(1L, fetchPerson.getId());
        assertEquals(1L, fetchPerson.getCompanyId());

        Company fetchCompany = companyRepository.findById(1L).get();
        assertEquals(1L, fetchCompany.getId());
        assertEquals(1L, fetchCompany.getPersons().size());
    }

    @Test
    @Transactional
    @Rollback(true)
    @Order(2)
    void testModify() {
        Company company = companyRepository.findById(1L).get();
        Person person = new Person("ziva2");

        company.addPerson(person);
        companyRepository.save(company);

        Iterable<Person> persons = personRepository.findAll();
        assertEquals(2, Streams.stream(persons.iterator()).count());
    }

    @Test
    @Transactional
    @Rollback(true)
    @Order(3)
    void testRollback() {
        Iterable<Person> persons = personRepository.findAll();
        assertEquals(1, Streams.stream(persons.iterator()).count());
    }
}
