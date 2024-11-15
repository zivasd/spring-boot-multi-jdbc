package multiple.jdbc.sample.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class TransactionTest {
    @Autowired
    @Qualifier("primaryJdbcTemplate")
    private JdbcOperations primaryOperations;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcOperations secondaryOperations;

    @BeforeAll
    void init() {
        primaryOperations.execute("create table t_person (id varchar(64), name varchar(64))");
        primaryOperations.execute("insert into t_person (id, name) values('1','bob')");
        primaryOperations.execute("insert into t_person (id, name) values('2','tom')");

        secondaryOperations.execute("create table t_user (id varchar(64), name varchar(64))");
        secondaryOperations.execute("insert into t_user (id, name) values('1','李明')");
        secondaryOperations.execute("insert into t_user (id, name) values('2','赵三')");
    }

    @Test
    @Transactional
    @Rollback(true)
    @Order(1)
    void rollbackTestStep1() {
        primaryOperations.execute("insert into t_person (id, name) values('3','jack')");
        List<Map<String, Object>> persons = primaryOperations.queryForList("select id, name from t_person where id=3");
        assertEquals(1, persons.size());
        assertEquals("jack", persons.get(0).get("name"));
    }

    @Test
    @Order(2)
    void rollbackTestStep2() {
        List<Map<String, Object>> persons = primaryOperations.queryForList("select id, name from t_person where id=3");
        assertEquals(0, persons.size());
    }

    @Test
    @Transactional
    @Rollback(false)
    @Order(3)
    void commitTestStep1() {
        primaryOperations.execute("insert into t_person (id, name) values('3','jack')");
        List<Map<String, Object>> persons = primaryOperations.queryForList("select id, name from t_person where id=3");
        assertEquals(1, persons.size());
    }

    @Test
    @Order(4)
    void commitTestStep2() {
        List<Map<String, Object>> persons = primaryOperations.queryForList("select id, name from t_person where id=3");
        assertEquals(1, persons.size());
        assertEquals("jack", persons.get(0).get("name"));
    }

    @Test
    @Transactional("secondaryTransactionManager")
    @Rollback(true)
    @Order(1)
    void rollback2TestStep1() {
        secondaryOperations.execute("insert into t_user (id, name)  values('3','王五')");
        List<Map<String, Object>> persons = secondaryOperations.queryForList("select id, name from t_user where id=3");
        assertEquals(1, persons.size());
        assertEquals("王五", persons.get(0).get("name"));
    }

    @Test
    @Order(2)
    void rollback2TestStep2() {
        List<Map<String, Object>> persons = secondaryOperations.queryForList("select id, name from t_user where id=3");
        assertEquals(0, persons.size());
    }

}
