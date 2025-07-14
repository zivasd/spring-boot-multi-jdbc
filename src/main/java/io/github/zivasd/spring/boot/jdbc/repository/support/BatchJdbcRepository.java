package io.github.zivasd.spring.boot.jdbc.repository.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.InsertSubject;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.support.SimpleJdbcRepository;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.conversion.IdValueSource;
import org.springframework.data.relational.core.conversion.RelationalEntityVersionUtils;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import io.github.zivasd.spring.boot.jdbc.repository.BatchRepository;

@Transactional(readOnly = true)
public class BatchJdbcRepository<T, ID> extends SimpleJdbcRepository<T, ID> implements BatchRepository<T, ID> {
    private final DataAccessStrategy accessStrategy;
    private final RelationalPersistentEntity<T> persistentEntity;
    private final JdbcConverter converter;

    public BatchJdbcRepository(DataAccessStrategy accessStrategy, JdbcAggregateOperations entityOperations,
            RelationalPersistentEntity<T> persistentEntity,
            JdbcConverter converter) {
        super(entityOperations, persistentEntity, converter);
        Assert.notNull(accessStrategy, "DataAccessStrategy must not be null.");
        this.accessStrategy = accessStrategy;
        this.persistentEntity = persistentEntity;
        this.converter = converter;
    }

    @Transactional
    @Override
    public <S extends T> List<S> batchSave(List<S> entities) {
        return batchSave(entities, 0);
    }

    @Transactional
    @Override
    public <S extends T> List<S> batchSave(List<S> entities, int batchSize) {
        Assert.notEmpty(entities, "Batch save must contain at least one entity");
        Assert.isTrue(batchSize >= 0, "batch size must not be negative.");

        batchSize = batchSize == 0 ? entities.size() : batchSize;
        List<InsertSubject<T>> insertSubjects = new ArrayList<>(entities.size());
        List<T> updateSubjects = new ArrayList<>(batchSize);
        for (T entity : entities) {
            if (persistentEntity.isNew(entity)) {
                submitInsertSubject(insertSubjects, entity, batchSize);
            } else {
                updateSubjects.add(entity);
            }
        }
        if (!insertSubjects.isEmpty()) {
            batchInsert(insertSubjects);
        }
        if (!updateSubjects.isEmpty()) {
            this.saveAll(updateSubjects);
        }
        return entities;
    }

    private <S extends T> void submitInsertSubject(List<InsertSubject<T>> entities, T entity, int batchSize) {
        entity = prepareVersionForInsert(entity);
        entities.add(InsertSubject.<T>describedBy(entity, Identifier.empty()));
        if (entities.size() == batchSize) {
            batchInsert(entities);
            entities.clear();
        }
    }

    private void batchInsert(List<InsertSubject<T>> entities) {
        IdValueSource idValueSource = IdValueSource.forInstance(entities.get(0).getInstance(),
                persistentEntity);

        Object[] ids = accessStrategy.insert(entities, persistentEntity.getType(), idValueSource);
        if (idValueSource == IdValueSource.GENERATED) {
            for (int i = 0; i < ids.length; ++i) {
                setId(entities.get(i).getInstance(), ids[i]);
            }
        }
    }

    private T setId(T instance, Object id) {
        PersistentPropertyAccessor<T> propertyAccessor = converter.getPropertyAccessor(persistentEntity, instance);
        RelationalPersistentProperty idProperty = persistentEntity.getRequiredIdProperty();
        propertyAccessor.setProperty(idProperty, id);
        return propertyAccessor.getBean();
    }

    private T prepareVersionForInsert(T instance) {
        T preparedInstance = instance;
        if (persistentEntity.hasVersionProperty()) {
            RelationalPersistentProperty versionProperty = persistentEntity.getRequiredVersionProperty();
            long initialVersion = versionProperty.getActualType().isPrimitive() ? 1L : 0;
            preparedInstance = RelationalEntityVersionUtils.setVersionNumberOnEntity( //
                    instance, initialVersion, persistentEntity, converter);
        }
        return preparedInstance;
    }
}
