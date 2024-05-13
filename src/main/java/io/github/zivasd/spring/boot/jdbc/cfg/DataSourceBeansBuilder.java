package io.github.zivasd.spring.boot.jdbc.cfg;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

@AutoConfiguration(value = "io.github.zivasd.spring.boot.jdbc.cfg.DataSourceBeansBuilder", before = {
        DataSourceAutoConfiguration.class })
@ComponentScan({ "io.github.zivasd.spring.boot.jdbc.cfg" })
@ConditionalOnClass({ DataSource.class, JdbcTemplate.class })
@ConditionalOnMissingBean(value = { DataSource.class, JdbcTemplate.class }, type = "io.r2dbc.spi.ConnectionFactory")
public class DataSourceBeansBuilder
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceBeansBuilder.class);

    private Map<String, DataSourceProperties> dataSources;
    private Environment environment;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        registry.removeBeanDefinition("WillRemovedTempJdbcTemplate");
        registry.removeBeanDefinition("WillRemovedTempDataSource");
        registry.removeBeanDefinition("WillRemovedTempDataSourceScriptDatabaseInitializer");

        if (applicationContext.getBeanNamesForType(DataSource.class).length > 0) {
            return;
        }

        Binder binder = Binder.get(environment);
        BindResult<Map<String, DataSourceProperties>> bindResult = binder
                .bind("spring.datasources", Bindable.mapOf(String.class, DataSourceProperties.class));
        if (!bindResult.isBound()) {
            LOGGER.warn(
                    "Cannot find any datasource. You should remove this dependency or add at least one datasource in spring.datasources.");
            return;
        }
        dataSources = bindResult.get();

        if (dataSources == null || dataSources.size() == 0)
            return;

        boolean primary = true;
        for (Map.Entry<String, DataSourceProperties> entry : dataSources.entrySet()) {
            String unitName = entry.getKey();
            LOGGER.info("Initialized DataSource: {}.", unitName + "DataSource");
            registerDataSourceProperties(registry, unitName, primary);
            registerDataSource(registry, unitName, primary);
            registerJdbcTemplate(registry, unitName, primary);
            registerSqlDataSourceScriptDatabaseInitializer(registry, unitName, primary);
            primary = false;
        }
    }

    private void registerDataSource(@NonNull BeanDefinitionRegistry registry,
            @NonNull String unitName, boolean primary) {
        registerBean(registry, "dataSource", unitName, "DataSource", primary);
    }

    private void registerDataSourceProperties(@NonNull BeanDefinitionRegistry registry,
            @NonNull String unitName, boolean primary) {
        registerBean(registry, "dataSourceProperties", unitName, "DataSourceProperties", primary);
    }

    private void registerJdbcTemplate(@NonNull BeanDefinitionRegistry registry, @NonNull String unitName,
            boolean primary) {
        registerBean(registry, "jdbcTemplate", unitName, "JdbcTemplate", primary);
    }

    private void registerSqlDataSourceScriptDatabaseInitializer(@NonNull BeanDefinitionRegistry registry,
            @NonNull String unitName,
            boolean primary) {

        Binder binder = Binder.get(environment);
        BindResult<SqlInitializationProperties> bindResult = binder
                .bind("spring.datasources." + unitName + ".initialization-sql",
                        Bindable.of(SqlInitializationProperties.class));
        if (!bindResult.isBound()) {
            return;
        }
        SqlInitializationProperties properties = bindResult.get();

        GenericBeanDefinition beanDefinition = createBasBeanDefinition(registry,
                "sqlDataSourceScriptDatabaseInitializer", unitName,
                "SqlDataSourceScriptDatabaseInitializer", primary);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(properties);
        registry.registerBeanDefinition(unitName + "SqlDataSourceScriptDatabaseInitializer", beanDefinition);
    }

    public DataSourceProperties dataSourceProperties(String name) {
        return dataSources.get(name);
    }

    public DataSource dataSource(String unit) {
        DataSourceProperties properties = this.dataSources.get(unit);
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        String type = properties.getType() != null ? properties.getType().getName() : null;
        if (type == null)
            return dataSource;
        String cfg = DATASOURCE_POOLS.get(type);
        if (cfg == null)
            return dataSource;
        Binder binder = Binder.get(environment);
        String key = "spring.datasources." + unit + "." + cfg;
        BindResult<Map<String, String>> cpProperties = binder.bind(key,
                Bindable.mapOf(String.class, String.class));
        if (!cpProperties.isBound() || cpProperties.get().size() == 0)
            return dataSource;

        return binder.bind(key, Bindable.ofInstance(dataSource)).get();
    }

    public JdbcTemplate jdbcTemplate(String unit) {
        DataSource dataSource = this.applicationContext.getBean(unit + "DataSource", DataSource.class);
        return new JdbcTemplate(dataSource);
    }

    public SqlDataSourceScriptDatabaseInitializer sqlDataSourceScriptDatabaseInitializer(String unit,
            SqlInitializationProperties properties) {
        DataSource dataSource = this.applicationContext.getBean(unit + "DataSource", DataSource.class);
        return new SqlDataSourceScriptDatabaseInitializer(
                determineDataSource(dataSource, properties.getUsername(), properties.getPassword()), properties);
    }

    private static DataSource determineDataSource(DataSource dataSource, String username, String password) {
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            return DataSourceBuilder.derivedFrom(dataSource)
                    .username(username)
                    .password(password)
                    .type(SimpleDriverDataSource.class)
                    .build();
        }
        return dataSource;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void registerBean(@NonNull BeanDefinitionRegistry registry, String factoryMethodName, String unit,
            String postfix, boolean primary) {
        GenericBeanDefinition beanDefinition = createBasBeanDefinition(registry, factoryMethodName, unit, postfix,
                primary);
        registry.registerBeanDefinition(unit + postfix, beanDefinition);
    }

    private GenericBeanDefinition createBasBeanDefinition(@NonNull BeanDefinitionRegistry registry,
            String factoryMethodName, String unit,
            String postfix, boolean primary) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setFactoryBeanName(this.getClass().getName());
        beanDefinition.setPrimary(primary);
        beanDefinition.setAutowireCandidate(true);
        beanDefinition.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);

        beanDefinition.setFactoryMethodName(factoryMethodName);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(unit);
        beanDefinition.setConstructorArgumentValues(constructorArgumentValues);

        beanDefinition.getQualifier(unit + postfix);
        return beanDefinition;
    }

    static class WillRemovedTemporarilyBeans {

        @Bean(name = "WillRemovedTempDataSourceScriptDatabaseInitializer")
        SqlDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource) {
            return null;
        }

        @Bean(name = "WillRemovedTempJdbcTemplate")
        @ConditionalOnMissingBean(value = JdbcTemplate.class)
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return null;
        }

        @Bean(name = "WillRemovedTempDataSource")
        @ConditionalOnMissingBean(value = DataSource.class)
        DataSource dataSource() {
            return null;
        }
    }

    private static final Map<String, String> DATASOURCE_POOLS = new HashMap<>();
    static {
        DATASOURCE_POOLS.put("com.mchange.v2.c3p0.ComboPooledDataSource", "c3p0");
        DATASOURCE_POOLS.put("com.zaxxer.hikari.HikariDataSource", "hikari");
        DATASOURCE_POOLS.put("oracle.ucp.jdbc.PoolDataSourceImpl", "oracle");
        DATASOURCE_POOLS.put("oracle.jdbc.OracleConnection", "oracle");
        DATASOURCE_POOLS.put("com.alibaba.druid.pool.DruidDataSource", "druid");
        DATASOURCE_POOLS.put("org.apache.tomcat.jdbc.pool.DataSource", "tomcat");
        DATASOURCE_POOLS.put("org.apache.commons.dbcp2.BasicDataSource", "dbcp2");
    }
}
