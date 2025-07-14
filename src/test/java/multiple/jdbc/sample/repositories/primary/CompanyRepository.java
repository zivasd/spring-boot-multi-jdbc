package multiple.jdbc.sample.repositories.primary;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.github.zivasd.spring.boot.jdbc.repository.BatchRepository;
import multiple.jdbc.sample.repositories.entities.Company;

@Repository
public interface CompanyRepository extends CrudRepository<Company, Long>, BatchRepository<Company, Long> {

}
