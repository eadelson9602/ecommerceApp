package ecommerce.app.inventory.application.port.out;

import ecommerce.app.inventory.domain.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida: persistencia de compras.
 */
public interface PurchaseRepositoryPort {

	Purchase save(Purchase purchase);

	Page<Purchase> findAll(Pageable pageable);
}
