package ecommerce.app.products.application.service;

import ecommerce.app.products.application.model.CreateProductCommand;
import ecommerce.app.products.application.model.ProductFilter;
import ecommerce.app.products.application.model.UpdateProductCommand;
import ecommerce.app.products.application.port.in.*;
import ecommerce.app.products.application.port.out.ProductRepositoryPort;
import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de aplicación (casos de uso). Implementa los puertos de entrada
 * y usa el puerto de salida para persistencia.
 */
@Service
public class ProductApplicationService implements
		ListProductsUseCase,
		GetProductUseCase,
		CreateProductUseCase,
		UpdateProductUseCase,
		DeleteProductUseCase {

	private final ProductRepositoryPort productRepository;

	public ProductApplicationService(ProductRepositoryPort productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Product> list(ProductFilter filter, int pageNumber, int pageSize, String sort) {
		Sort order = parseSort(sort);
		Pageable pageable = PageRequest.of(pageNumber - 1, Math.min(pageSize, 100), order);
		return productRepository.findAllFiltered(
				filter.getStatus().orElse(null),
				filter.getSearch().map(s -> s.trim()).filter(s -> !s.isEmpty()).orElse(null),
				pageable
		);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Product> getById(UUID id) {
		return productRepository.findById(id);
	}

	@Override
	@Transactional
	public Product create(CreateProductCommand command) {
		if (productRepository.existsBySku(command.getSku())) {
			throw new SkuAlreadyExistsException(command.getSku());
		}
		Product product = new Product();
		product.setSku(command.getSku().trim());
		product.setName(command.getName().trim());
		product.setPrice(command.getPrice());
		product.setStatus(command.getStatus());
		return productRepository.save(product);
	}

	@Override
	@Transactional
	public Optional<Product> update(UUID id, UpdateProductCommand command) {
		return productRepository.findById(id)
				.map(p -> {
					if (!p.getSku().equals(command.getSku().trim()) && productRepository.existsBySku(command.getSku())) {
						throw new SkuAlreadyExistsException(command.getSku());
					}
					p.setSku(command.getSku().trim());
					p.setName(command.getName().trim());
					p.setPrice(command.getPrice());
					p.setStatus(command.getStatus());
					return productRepository.save(p);
				});
	}

	@Override
	@Transactional
	public boolean deleteById(UUID id) {
		if (productRepository.findById(id).isEmpty()) return false;
		productRepository.deleteById(id);
		return true;
	}

	private Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.ASC, "createdAt");
		}
		String field = sort.startsWith("-") ? sort.substring(1) : sort;
		Sort.Direction direction = sort.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, field);
	}
}
