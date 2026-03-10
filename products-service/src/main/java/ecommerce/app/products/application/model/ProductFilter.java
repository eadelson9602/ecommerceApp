package ecommerce.app.products.application.model;

import ecommerce.app.products.domain.ProductStatus;

import java.util.Optional;

/**
 * Criterios de filtrado para listado de productos (puerto de entrada).
 */
public class ProductFilter {

	private final Optional<ProductStatus> status;
	private final Optional<String> search;

	public ProductFilter(Optional<ProductStatus> status, Optional<String> search) {
		this.status = status;
		this.search = search;
	}

	public Optional<ProductStatus> getStatus() {
		return status;
	}

	public Optional<String> getSearch() {
		return search;
	}
}
