package ecommerce.app.inventory.application.port.in;

import ecommerce.app.inventory.domain.Purchase;
import org.springframework.data.domain.Page;

/**
 * Caso de uso: listar compras realizadas (paginado).
 */
public interface ListPurchasesUseCase {

	Page<Purchase> list(int pageNumber, int pageSize);
}
