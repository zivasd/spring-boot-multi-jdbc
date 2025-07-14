package io.github.zivasd.spring.boot.jdbc.repository.support;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.BatchJdbcOperations;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.InsertStrategyFactory;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.SqlGeneratorSource;
import org.springframework.data.jdbc.core.convert.SqlParametersFactory;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

public class BatchJdbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private ApplicationEventPublisher publisher;
    private BeanFactory beanFactory;
    private RelationalMappingContext mappingContext;
    private JdbcConverter converter;
    private DataAccessStrategy dataAccessStrategy;
    private QueryMappingConfiguration queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
    private NamedParameterJdbcOperations operations;
    private EntityCallbacks entityCallbacks;
    private Dialect dialect;

    /**
     * Creates a new {@link BatchJdbcRepositoryFactoryBean} for the given
     * repository
     * interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public BatchJdbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
     * #setApplicationEventPublisher(org.springframework.context.
     * ApplicationEventPublisher)
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        super.setApplicationEventPublisher(publisher);
        this.publisher = publisher;
    }

    /**
     * Creates the actual {@link RepositoryFactorySupport} instance.
     */
    @Override
    @NonNull
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        BatchJdbcRepositoryFactory jdbcRepositoryFactory = new BatchJdbcRepositoryFactory(dataAccessStrategy,
                mappingContext,
                converter, dialect, publisher, operations);
        jdbcRepositoryFactory.setQueryMappingConfiguration(queryMappingConfiguration);
        jdbcRepositoryFactory.setEntityCallbacks(entityCallbacks);
        jdbcRepositoryFactory.setBeanFactory(beanFactory);

        return jdbcRepositoryFactory;
    }

    @Autowired
    public void setMappingContext(RelationalMappingContext mappingContext) {
        Assert.notNull(mappingContext, "MappingContext must not be null");
        super.setMappingContext(mappingContext);
        this.mappingContext = mappingContext;
    }

    @Autowired
    public void setDialect(Dialect dialect) {
        Assert.notNull(dialect, "Dialect must not be null");
        this.dialect = dialect;
    }

    /**
     * @param dataAccessStrategy can be {@literal null}.
     */
    public void setDataAccessStrategy(DataAccessStrategy dataAccessStrategy) {
        Assert.notNull(dataAccessStrategy, "DataAccessStrategy must not be null");
        this.dataAccessStrategy = dataAccessStrategy;
    }

    /**
     * @param queryMappingConfiguration can be {@literal null}.
     *                                  {@link #afterPropertiesSet()} defaults to
     *                                  {@link QueryMappingConfiguration#EMPTY} if
     *                                  {@literal null}.
     */
    @Autowired(required = false)
    public void setQueryMappingConfiguration(QueryMappingConfiguration queryMappingConfiguration) {
        Assert.notNull(queryMappingConfiguration, "QueryMappingConfiguration must not be null");
        this.queryMappingConfiguration = queryMappingConfiguration;
    }

    public void setJdbcOperations(NamedParameterJdbcOperations operations) {
        Assert.notNull(operations, "NamedParameterJdbcOperations must not be null");
        this.operations = operations;
    }

    @Autowired
    public void setConverter(JdbcConverter converter) {
        Assert.notNull(converter, "JdbcConverter must not be null");
        this.converter = converter;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
     * #afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {

        Assert.state(this.mappingContext != null, "MappingContext is required and must not be null!");
        Assert.state(this.converter != null, "RelationalConverter is required and must not be null!");

        if (this.operations == null) {
            Assert.state(beanFactory != null, "If no JdbcOperations are set a BeanFactory must be available.");
            this.operations = beanFactory.getBean(NamedParameterJdbcOperations.class);
        }

        if (this.dataAccessStrategy == null) {
            Assert.state(beanFactory != null, "If no DataAccessStrategy is set a BeanFactory must be available.");
            Assert.state(this.dialect != null, "Dialect is required and must not be null!");

            SqlGeneratorSource sqlGeneratorSource = new SqlGeneratorSource(this.mappingContext,
                    this.converter,
                    this.dialect);
            SqlParametersFactory sqlParametersFactory = new SqlParametersFactory(this.mappingContext,
                    this.converter,
                    this.dialect);
            InsertStrategyFactory insertStrategyFactory = new InsertStrategyFactory(this.operations,
                    new BatchJdbcOperations(this.operations.getJdbcOperations()), this.dialect);

            this.dataAccessStrategy = new DefaultDataAccessStrategy(sqlGeneratorSource, this.mappingContext,
                    this.converter,
                    this.operations, sqlParametersFactory, insertStrategyFactory);

        }

        if (this.queryMappingConfiguration == null) {
            this.queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
        }

        if (beanFactory != null) {
            entityCallbacks = EntityCallbacks.create(beanFactory);
        }

        super.afterPropertiesSet();
    }
}