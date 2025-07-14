package io.github.zivasd.spring.boot.jdbc.repository.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.Nullable;

public class BatchJdbcRepositoryFactory extends JdbcRepositoryFactory {

    private final RelationalMappingContext context;
    private final JdbcConverter converter;
    private final ApplicationEventPublisher publisher;
    private final DataAccessStrategy accessStrategy;

    @Nullable
    private BeanFactory beanFactory;

    private EntityCallbacks entityCallbacks;

    /**
     * Creates a new {@link BatchJdbcRepositoryFactory} for the given
     * {@link DataAccessStrategy},
     * {@link RelationalMappingContext} and {@link ApplicationEventPublisher}.
     *
     * @param dataAccessStrategy must not be {@literal null}.
     * @param context            must not be {@literal null}.
     * @param converter          must not be {@literal null}.
     * @param dialect            must not be {@literal null}.
     * @param publisher          must not be {@literal null}.
     * @param operations         must not be {@literal null}.
     */
    public BatchJdbcRepositoryFactory(DataAccessStrategy dataAccessStrategy, RelationalMappingContext context,
            JdbcConverter converter, Dialect dialect, ApplicationEventPublisher publisher,
            NamedParameterJdbcOperations operations) {
        super(dataAccessStrategy, context, converter, dialect, publisher, operations);
        this.publisher = publisher;
        this.context = context;
        this.converter = converter;
        this.accessStrategy = dataAccessStrategy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.core.support.RepositoryFactorySupport#
     * getTargetRepository(org.springframework.data.repository.core.
     * RepositoryInformation)
     */
    @Override
    protected Object getTargetRepository(RepositoryInformation repositoryInformation) {

        JdbcAggregateTemplate template = new JdbcAggregateTemplate(publisher, context, converter, accessStrategy);

        if (entityCallbacks != null) {
            template.setEntityCallbacks(entityCallbacks);
        }

        RelationalPersistentEntity<?> persistentEntity = context
                .getRequiredPersistentEntity(repositoryInformation.getDomainType());

        return instantiateClass(repositoryInformation.getRepositoryBaseClass(), accessStrategy, template,
                persistentEntity, converter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.core.support.RepositoryFactorySupport#
     * getRepositoryBaseClass(org.springframework.data.repository.core.
     * RepositoryMetadata)
     */
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        return BatchJdbcRepository.class;
    }
}
