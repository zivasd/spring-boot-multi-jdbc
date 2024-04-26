package com.ziva.spring.boot.jdbc.cfg;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component("com.ziva.spring.boot.jdbc.cfg.DataSourceBeansBuilder")
@ComponentScan({ "com.ziva.spring.boot.jdbc.cfg" })
public class DataSourceBeansBuilder
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private Map<String, DataSourceProperties> dataSources;
    private Environment environment;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        Binder binder = Binder.get(environment);
        dataSources = binder
                .bind("spring.datasources", Bindable.mapOf(String.class, DataSourceProperties.class)).get();

        if (dataSources == null || dataSources.size() == 0)
            return;

        registry.removeBeanDefinition("WillRemovedTempJdbcTemplate");
        boolean primary = true;
        for (Map.Entry<String, DataSourceProperties> entry : dataSources.entrySet()) {
            registerDataSourcePropertiesBeanDefinition(registry, entry.getKey(), primary);
            registerDataSource(registry, entry.getKey(), primary);
            registerJdbcTemplate(registry, entry.getKey(), primary);
            primary = false;
        }
    }

    private void registerDataSource(@NonNull BeanDefinitionRegistry registry, String unitName,
            boolean primary) {
        registerBean(registry, "dataSource", unitName, "DataSource", primary);
    }

    private void registerDataSourcePropertiesBeanDefinition(@NonNull BeanDefinitionRegistry registry,
            String unitName, boolean primary) {
        registerBean(registry, "dataSourceProperties", unitName, "DataSourceProperties", primary);
    }

    private void registerJdbcTemplate(@NonNull BeanDefinitionRegistry registry, String unitName,
            boolean primary) {
        registerBean(registry, "jdbcTemplate", unitName, "JdbcTemplate", primary);
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
        registry.registerBeanDefinition(unit + postfix, beanDefinition);
    }

    @Configuration
    static class WillRemovedTemporarilyBeans {
        @Bean(name = "WillRemovedTempJdbcTemplate")
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
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
