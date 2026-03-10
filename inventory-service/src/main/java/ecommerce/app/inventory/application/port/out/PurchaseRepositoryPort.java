package ecommerce.app.inventory.application.port.out;

import ecommerce.app.inventory.domain.Purchase;

/**
 * Puerto de salida: persistencia de compras.
 */
public interface PurchaseRepositoryPort {

	Purchase save(Purchase purchase);
}
