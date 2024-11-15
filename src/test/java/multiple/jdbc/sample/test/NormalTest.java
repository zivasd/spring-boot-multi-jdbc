package multiple.jdbc.sample.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class NormalTest {
    @Autowired
    @Qualifier("primaryJdbcTemplate")
    private JdbcOperations primaryOperations;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcOperations secondaryOperations;

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

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
    void testFirst() {
        List<Map<String, Object>> persons = primaryOperations.queryForList("select id, name from t_person where id=1");
        assertEquals(1, persons.size());
        assertEquals("bob", persons.get(0).get("name"));
    }

    @Test
    void testSecond() {
        List<Map<String, Object>> persons = secondaryOperations
                .queryForList("select id, name from t_user where id=1");
        assertEquals(1, persons.size());
        assertEquals("李明", persons.get(0).get("name"));
    }

    @Test
    void testDataSource() {
        String name = assertDoesNotThrow(() -> {
            return primaryDataSource.getConnection().getMetaData().getURL();
        });
        assertEquals("jdbc:h2:mem:testdb", name);

        name = assertDoesNotThrow(() -> {
            return secondaryDataSource.getConnection().getMetaData().getURL();
        });
        assertEquals("jdbc:h2:mem:testdb2", name);
    }
}
