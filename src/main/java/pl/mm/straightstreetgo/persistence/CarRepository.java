package pl.mm.straightstreetgo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.mm.straightstreetgo.api.dto.CarType;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByTypeOrderById(CarType type);
}
