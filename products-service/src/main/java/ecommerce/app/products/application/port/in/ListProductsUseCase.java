package ecommerce.app.products.application.port.in;

import ecommerce.app.products.application.model.ProductFilter;
import ecommerce.app.products.domain.Product;
import org.springframework.data.domain.Page;

/**
 * Caso de uso: listar productos con filtros y paginación (puerto de entrada).
 */
public interface ListProductsUseCase {

	Page<Product> list(ProductFilter filter, int pageNumber, int pageSize, String sort);
}
