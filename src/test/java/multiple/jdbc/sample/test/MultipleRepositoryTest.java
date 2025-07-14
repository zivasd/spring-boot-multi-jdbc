package multiple.jdbc.sample.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import multiple.jdbc.sample.repositories.entities.Person;
import multiple.jdbc.sample.repositories.primary.PersonRepository;
import multiple.jdbc.sample.repositories.secondary.SecondaryPersonRepository;

@SpringBootTest
class MultipleRepositoryTest {
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private SecondaryPersonRepository secondaryPersonRepository;

    @Autowired
    @Qualifier("primaryNamedParameterJdbcTemplate")
    private NamedParameterJdbcOperations primaryJdbcOperations;

    @Autowired
    @Qualifier("secondaryNamedParameterJdbcTemplate")
    private NamedParameterJdbcOperations secondaryJdbcOperations;

    @BeforeEach
    void init() {
        primaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_person");
        secondaryJdbcOperations.getJdbcOperations().execute("drop table if exists t_person");

        primaryJdbcOperations.getJdbcOperations().execute(
                "create table t_person (ID bigint not null auto_increment primary key, NAME varchar(256), COMPANY_ID bigint, VERSION bigint)");

        secondaryJdbcOperations.getJdbcOperations().execute(
                "create table t_person (ID bigint not null auto_increment primary key, NAME varchar(256), COMPANY_ID bigint, VERSION bigint)");
    }

    @Test
    void testBase() {
        Person person = new Person("ziva1");
        personRepository.save(person);

        assertEquals(1, personRepository.count());
        assertEquals(0, secondaryPersonRepository.count());

        person = new Person("ziva1");
        secondaryPersonRepository.save(person);

        assertEquals(1, personRepository.count());
        assertEquals(1, secondaryPersonRepository.count());

    }

}
