package io.github.zivasd.spring.boot.jdbc.cfg;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.util.StringUtils;

@AutoConfiguration(value = "io.github.zivasd.spring.boot.jdbc.cfg.DataSourceBeansBuilder", before = {
        DataSourceAutoConfiguration.class })
@ComponentScan({ "io.github.zivasd.spring.boot.jdbc.cfg" })
@ConditionalOnClass({ DataSource.class, JdbcTemplate.class })
@ConditionalOnMissingBean(value = { DataSource.class, JdbcTemplate.class }, type = "io.r2dbc.spi.ConnectionFactory")
public class DataSourceBeansBuilder
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceBeansBuilder.class);

    private static final String DATASOURCE_PREFIX = "DataSource";

    private Map<String, DataSourceProperties> dataSources;
    private Environment environment;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        /**
         * do nothing
         */
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_JDBC_TEMPLATE);
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_NAMEDJDBC_TEMPLATE);
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_DATASOURCE1);
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_DATASOURCE2);
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_DATABASE_INITIALIZER);
        registry.removeBeanDefinition(WillRemovedTemporarilyBeans.class.getName());
        boolean doRegisterTM = registry.containsBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_TRANSACTION_MANAGER);
        if (doRegisterTM)
            registry.removeBeanDefinition(WillRemovedTemporarilyBeans.REMOVED_TRANSACTION_MANAGER);

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
            LOGGER.info("Initialized DataSource: {}{}.", unitName, DATASOURCE_PREFIX);
            registerDataSourceProperties(registry, unitName, primary);
            registerDataSource(registry, unitName, primary);
            registerNamedParameterJdbcTemplate(registry, unitName, primary);
            registerJdbcTemplate(registry, unitName, primary);
            registerSqlDataSourceScriptDatabaseInitializer(registry, unitName, primary);
            if (doRegisterTM)
                registerTransactionManager(registry, unitName, primary);
            primary = false;
        }
    }

    private void registerDataSource(@NonNull BeanDefinitionRegistry registry,
            @NonNull String unitName, boolean primary) {
        registerBean(registry, "dataSource", unitName, DATASOURCE_PREFIX, primary);
    }

    private void registerDataSourceProperties(@NonNull BeanDefinitionRegistry registry,
            @NonNull String unitName, boolean primary) {
        registerBean(registry, "dataSourceProperties", unitName, "DataSourceProperties", primary);
    }

    private void registerNamedParameterJdbcTemplate(@NonNull BeanDefinitionRegistry registry, @NonNull String unitName,
            boolean primary) {
        registerBean(registry, "namedParameterJdbcTemplate", unitName, "NamedParameterJdbcTemplate", primary);
    }

    private void registerJdbcTemplate(@NonNull BeanDefinitionRegistry registry, @NonNull String unitName,
            boolean primary) {
        registerBean(registry, "jdbcTemplate", unitName, "JdbcTemplate", primary);
    }

    private void registerTransactionManager(@NonNull BeanDefinitionRegistry registry, @NonNull String unitName,
            boolean primary) {
        registerBean(registry, "transactionManager", unitName, "TransactionManager", primary);
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

        GenericBeanDefinition beanDefinition = createBasBeanDefinition(
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

    public PlatformTransactionManager transactionManager(String unit,
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        DataSource dataSource = this.applicationContext.getBean(unit + DATASOURCE_PREFIX, DataSource.class);
        DataSourceTransactionManager transactionManager = createTransactionManager(environment, dataSource);
        transactionManagerCustomizers.ifAvailable(customizers -> customizers.customize(transactionManager));
        return transactionManager;
    }

    private DataSourceTransactionManager createTransactionManager(Environment environment, DataSource dataSource) {
        return Boolean.TRUE
                .equals(environment.getProperty("spring.dao.exceptiontranslation.enabled", Boolean.class, Boolean.TRUE))
                        ? new JdbcTransactionManager(dataSource)
                        : new DataSourceTransactionManager(dataSource);
    }

    public JdbcTemplate jdbcTemplate(String unit) {
        NamedParameterJdbcTemplate template = this.applicationContext.getBean(unit + "NamedParameterJdbcTemplate",
                NamedParameterJdbcTemplate.class);
        return template.getJdbcTemplate();
    }

    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(String unit) {
        DataSource dataSource = this.applicationContext.getBean(unit + DATASOURCE_PREFIX, DataSource.class);
        return new NamedParameterJdbcTemplate(dataSource);
    }

    public SqlDataSourceScriptDatabaseInitializer sqlDataSourceScriptDatabaseInitializer(String unit,
            SqlInitializationProperties properties) {
        DataSource dataSource = this.applicationContext.getBean(unit + DATASOURCE_PREFIX, DataSource.class);
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
        GenericBeanDefinition beanDefinition = createBasBeanDefinition(factoryMethodName, unit, postfix,
                primary);
        registry.registerBeanDefinition(unit + postfix, beanDefinition);
    }

    private GenericBeanDefinition createBasBeanDefinition(String factoryMethodName, String unit,
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
        static final String REMOVED_DATABASE_INITIALIZER = "willRemovedTempDataSourceScriptDatabaseInitializer";
        static final String REMOVED_JDBC_TEMPLATE = "willRemovedTempJdbcTemplate";
        static final String REMOVED_NAMEDJDBC_TEMPLATE = "willRemovedTempNamedParameterJdbcTemplate";
        static final String REMOVED_TRANSACTION_MANAGER = "willRemovedTransactionManager";
        static final String REMOVED_DATASOURCE1 = "willRemovedTempDataSource1";
        static final String REMOVED_DATASOURCE2 = "willRemovedTempDataSource2";

        @Bean(name = REMOVED_DATABASE_INITIALIZER)
        SqlDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource) {
            return null;
        }

        @Bean(name = REMOVED_JDBC_TEMPLATE)
        @ConditionalOnMissingBean(value = JdbcTemplate.class)
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return null;
        }

        @Bean(name = REMOVED_NAMEDJDBC_TEMPLATE)
        @ConditionalOnMissingBean(value = NamedParameterJdbcTemplate.class)
        NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
            return null;
        }

        @Bean(name = REMOVED_TRANSACTION_MANAGER)
        @ConditionalOnMissingBean(value = TransactionManager.class)
        @ConditionalOnMissingClass(value = "org.springframework.orm.jpa.JpaVendorAdapter")
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return null;
        }

        @Bean(name = REMOVED_DATASOURCE1)
        @Primary
        @ConditionalOnMissingBean(value = DataSource.class)
        DataSource dataSource1() {
            return null;
        }

        @Bean(name = REMOVED_DATASOURCE2)
        @ConditionalOnBean(name = REMOVED_DATASOURCE1)
        DataSource dataSource2() {
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
