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
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootTest
@TestConfiguration()
@TestInstance(Lifecycle.PER_CLASS)
class OverideTest {

    @TestConfiguration
    static class DataSouceBuilder {
        @Bean(name = "primaryDataSource")
        @Primary
        DataSource buildDataSource() {
            DataSourceProperties properties = new DataSourceProperties();
            properties.setUrl("jdbc:h2:mem:innerdb1");
            properties.setDriverClassName("org.h2.Driver");
            properties.setUsername("sa");
            return properties.initializeDataSourceBuilder().build();
        }

        @Bean(name = "primaryNamedParameterJdbcTemplate")
        @Primary
        NamedParameterJdbcTemplate buildNPJT(@Qualifier("primaryDataSource") DataSource dataSource) {
            return new NamedParameterJdbcTemplate(dataSource);
        }

        @Bean(name = "primaryJdbcTemplate")
        @Primary
        JdbcTemplate buildJdbcTemplete(
                @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate npjt) {
            return npjt.getJdbcTemplate();
        }

        @Bean(name = "secondaryDataSource")
        DataSource buildDataSource2() {
            DataSourceProperties properties = new DataSourceProperties();
            properties.setUrl("jdbc:h2:mem:innerdb2");
            properties.setDriverClassName("org.h2.Driver");
            properties.setUsername("sa");
            return properties.initializeDataSourceBuilder().build();
        }

        @Bean(name = "secondaryNamedParameterJdbcTemplate")
        NamedParameterJdbcTemplate buildNPJT2(@Qualifier("secondaryDataSource") DataSource dataSource) {
            return new NamedParameterJdbcTemplate(dataSource);
        }

        @Bean(name = "secondaryJdbcTemplate")
        JdbcTemplate buildJdbcTemplete2(
                @Qualifier("secondaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate npjt) {
            return npjt.getJdbcTemplate();
        }
    }

    @Autowired
    @Qualifier("primaryJdbcTemplate")
    private JdbcOperations primaryOperations;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcOperations secondaryOperations;

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

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
        assertEquals("jdbc:h2:mem:innerdb1", name);

        name = assertDoesNotThrow(() -> {
            return secondaryDataSource.getConnection().getMetaData().getURL();
        });
        assertEquals("jdbc:h2:mem:innerdb2", name);
    }
}
