package ecommerce.app.inventory.application.port.in;

import ecommerce.app.inventory.application.model.PurchaseResult;

import java.util.UUID;

/**
 * Caso de uso: procesar una compra (validar producto, stock, descontar, idempotencia).
 */
public interface ProcessPurchaseUseCase {

	PurchaseResult processPurchase(UUID productId, int quantity, String idempotencyKey);
}
