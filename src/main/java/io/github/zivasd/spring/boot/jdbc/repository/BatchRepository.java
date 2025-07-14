package io.github.zivasd.spring.boot.jdbc.repository;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface BatchRepository<T, ID> extends Repository<T, ID> {
    <S extends T> List<S> batchSave(List<S> entities);

    <S extends T> List<S> batchSave(List<S> entities, int batchSize);
}
