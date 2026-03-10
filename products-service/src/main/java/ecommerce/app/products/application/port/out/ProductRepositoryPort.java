package ecommerce.app.products.application.port.out;

import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (out): persistencia de productos.
 * La infraestructura implementa este puerto con el adaptador JPA.
 */
public interface ProductRepositoryPort {

	Page<Product> findAllFiltered(ProductStatus status, String search, Pageable pageable);

	Optional<Product> findById(UUID id);

	boolean existsBySku(String sku);

	Product save(Product product);

	void deleteById(UUID id);
}
