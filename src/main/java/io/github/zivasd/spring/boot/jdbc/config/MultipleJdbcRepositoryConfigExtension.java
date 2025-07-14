package io.github.zivasd.spring.boot.jdbc.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

public class MultipleJdbcRepositoryConfigExtension extends JdbcRepositoryConfigExtension {

    private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.RepositoryConfigurationExtension#
     * getModuleName()
     */
    @Override
    public String getModuleName() {
        return "JDBC";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * RepositoryConfigurationExtensionSupport#getRepositoryFactoryBeanClassName()
     */
    @Override
    public String getRepositoryFactoryBeanClassName() {
        return JdbcRepositoryFactoryBean.class.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * RepositoryConfigurationExtensionSupport#getModulePrefix()
     */
    @Override
    protected String getModulePrefix() {
        return getModuleName().toLowerCase(Locale.US);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans
     * .factory.support.BeanDefinitionBuilder,
     * org.springframework.data.repository.config.RepositoryConfigurationSource)
     */
    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

        source.getAttribute("jdbcOperationsRef") //
                .filter(StringUtils::hasText) //
                .ifPresent(s -> builder.addPropertyReference("jdbcOperations", s));

        source.getAttribute("dataAccessStrategyRef") //
                .filter(StringUtils::hasText) //
                .ifPresent(s -> builder.addPropertyReference("dataAccessStrategy", s));

        Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
        builder.addPropertyValue("transactionManager",
                transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
    }

    /**
     * In strict mode only domain types having a {@link Table} annotation get a
     * repository.
     */
    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(Table.class);
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.<Class<?>>singleton(Repository.class);
    }
}