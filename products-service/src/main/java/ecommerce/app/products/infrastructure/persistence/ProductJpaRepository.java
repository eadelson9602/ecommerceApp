package ecommerce.app.products.infrastructure.persistence;

import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA (infraestructura). El adaptador lo usa para implementar el puerto.
 */
interface ProductJpaRepository extends JpaRepository<Product, UUID> {

	boolean existsBySku(String sku);

	@Query("""
		SELECT p FROM Product p
		WHERE (:status IS NULL OR p.status = :status)
		AND (:search IS NULL OR :search = '' OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
		""")
	Page<Product> findAllFiltered(@Param("status") ProductStatus status, @Param("search") String search, Pageable pageable);
}
