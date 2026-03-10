package ecommerce.app.inventory.application.model;

import ecommerce.app.inventory.domain.IdempotencyRecord;
import ecommerce.app.inventory.domain.Purchase;

/**
 * Resultado del caso de uso "procesar compra".
 */
public class PurchaseResult {
	public enum Type { SUCCESS, PRODUCT_NOT_FOUND, INVENTORY_NOT_FOUND, INSUFFICIENT_STOCK, CONFLICT, IDEMPOTENT_RESPONSE }

	private final Type type;
	private final Purchase purchase;
	private final int httpStatus;
	private final String errorCode;
	private final String errorDetail;
	private final String cachedResponseBody;

	public Type getType() { return type; }
	public Purchase getPurchase() { return purchase; }
	public int getHttpStatus() { return httpStatus; }
	public String getErrorCode() { return errorCode; }
	public String getErrorDetail() { return errorDetail; }
	public String getCachedResponseBody() { return cachedResponseBody; }

	public static PurchaseResult success(Purchase purchase) {
		return new PurchaseResult(Type.SUCCESS, purchase, 201, null, null, null);
	}

	public static PurchaseResult productNotFound() {
		return new PurchaseResult(Type.PRODUCT_NOT_FOUND, null, 404, "NOT_FOUND", "Producto no encontrado en el catálogo.", null);
	}

	public static PurchaseResult inventoryNotFound() {
		return new PurchaseResult(Type.INVENTORY_NOT_FOUND, null, 404, "INVENTORY_NOT_FOUND", "No hay registro de inventario para este producto.", null);
	}

	public static PurchaseResult insufficientStock(int available) {
		return new PurchaseResult(Type.INSUFFICIENT_STOCK, null, 422, "INSUFFICIENT_STOCK", "Stock insuficiente. Disponible: " + available, null);
	}

	public static PurchaseResult conflict(String detail) {
		return new PurchaseResult(Type.CONFLICT, null, 409, "CONFLICT", detail, null);
	}

	public static PurchaseResult fromIdempotency(IdempotencyRecord record) {
		return new PurchaseResult(Type.IDEMPOTENT_RESPONSE, null, record.getResponseStatus(), null, null, record.getResponseBody());
	}

	private PurchaseResult(Type type, Purchase purchase, int httpStatus, String errorCode, String errorDetail, String cachedResponseBody) {
		this.type = type;
		this.purchase = purchase;
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorDetail = errorDetail;
		this.cachedResponseBody = cachedResponseBody;
	}
}
