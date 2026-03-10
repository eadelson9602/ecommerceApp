package ecommerce.app.products.infrastructure.persistence;

import ecommerce.app.products.application.port.out.ProductRepositoryPort;
import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia: implementa el puerto de salida usando Spring Data JPA.
 */
@Component
public class ProductRepositoryAdapter implements ProductRepositoryPort {

	private final ProductJpaRepository jpaRepository;

	public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Page<Product> findAllFiltered(ProductStatus status, String search, Pageable pageable) {
		return jpaRepository.findAllFiltered(status, search, pageable);
	}

	@Override
	public Optional<Product> findById(UUID id) {
		return jpaRepository.findById(id);
	}

	@Override
	public boolean existsBySku(String sku) {
		return jpaRepository.existsBySku(sku);
	}

	@Override
	public Product save(Product product) {
		return jpaRepository.save(product);
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
