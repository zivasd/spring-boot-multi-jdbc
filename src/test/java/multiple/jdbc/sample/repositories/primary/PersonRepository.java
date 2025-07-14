package multiple.jdbc.sample.repositories.primary;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.github.zivasd.spring.boot.jdbc.repository.BatchRepository;
import multiple.jdbc.sample.repositories.entities.Person;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long>, BatchRepository<Person, Long> {

}
