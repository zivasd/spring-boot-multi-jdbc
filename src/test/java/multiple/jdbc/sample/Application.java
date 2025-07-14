package multiple.jdbc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.zivasd.spring.boot.jdbc.config.EnableMultipleJdbcRepositories;
import io.github.zivasd.spring.boot.jdbc.repository.support.BatchJdbcRepositoryFactoryBean;

@SpringBootApplication
@EnableMultipleJdbcRepositories(repositoryFactoryBeanClass = BatchJdbcRepositoryFactoryBean.class, basePackages = "multiple.jdbc.sample.repositories.primary", jdbcOperationsRef = "primaryNamedParameterJdbcTemplate", transactionManagerRef = "primaryTransactionManager")
@EnableMultipleJdbcRepositories(repositoryFactoryBeanClass = BatchJdbcRepositoryFactoryBean.class, basePackages = "multiple.jdbc.sample.repositories.secondary", jdbcOperationsRef = "secondaryNamedParameterJdbcTemplate", transactionManagerRef = "secondaryTransactionManager")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}