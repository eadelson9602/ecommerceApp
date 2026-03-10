package ecommerce.app.inventory.application.model;

import ecommerce.app.inventory.domain.Inventory;

/**
 * Resultado del caso de uso "consultar inventario validando producto".
 */
public class GetInventoryResult {
	public enum Type { SUCCESS, PRODUCT_NOT_FOUND, INVENTORY_NOT_FOUND }

	private final Type type;
	private final Inventory inventory;
	private final int httpStatus;
	private final String errorCode;
	private final String errorDetail;

	public Type getType() { return type; }
	public Inventory getInventory() { return inventory; }
	public int getHttpStatus() { return httpStatus; }
	public String getErrorCode() { return errorCode; }
	public String getErrorDetail() { return errorDetail; }

	public static GetInventoryResult success(Inventory inventory) {
		return new GetInventoryResult(Type.SUCCESS, inventory, 200, null, null);
	}

	public static GetInventoryResult productNotFound() {
		return new GetInventoryResult(Type.PRODUCT_NOT_FOUND, null, 404, "NOT_FOUND", "Producto no encontrado en el catálogo.");
	}

	public static GetInventoryResult inventoryNotFound() {
		return new GetInventoryResult(Type.INVENTORY_NOT_FOUND, null, 404, "INVENTORY_NOT_FOUND", "No hay registro de inventario para este producto.");
	}

	private GetInventoryResult(Type type, Inventory inventory, int httpStatus, String errorCode, String errorDetail) {
		this.type = type;
		this.inventory = inventory;
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorDetail = errorDetail;
	}
}
