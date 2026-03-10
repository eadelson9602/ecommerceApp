package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.inventory.application.model.GetInventoryResult;
import ecommerce.app.inventory.application.model.PurchaseResult;
import ecommerce.app.inventory.application.model.PurchaseResult.Type;
import ecommerce.app.inventory.application.port.in.GetInventoryUseCase;
import ecommerce.app.inventory.application.port.in.ProcessPurchaseUseCase;
import ecommerce.app.inventory.application.port.in.SetInventoryUseCase;
import ecommerce.app.inventory.application.port.out.IdempotencyRecordPort;
import ecommerce.app.inventory.domain.IdempotencyRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador HTTP: inventario y compras en formato JSON:API.
 * Documentación interactiva: Swagger UI (/swagger-ui.html).
 */
@Tag(name = "Inventory", description = "Consultar y actualizar inventario por producto")
@Tag(name = "Purchases", description = "Registrar compras (idempotentes con Idempotency-Key)")
@RestController
@RequestMapping("/api")
public class InventoryController {

	public static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";
	public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	private final GetInventoryUseCase getInventoryUseCase;
	private final ProcessPurchaseUseCase processPurchaseUseCase;
	private final SetInventoryUseCase setInventoryUseCase;
	private final IdempotencyRecordPort idempotencyRecordPort;
	private final ObjectMapper objectMapper;

	public InventoryController(
			GetInventoryUseCase getInventoryUseCase,
			ProcessPurchaseUseCase processPurchaseUseCase,
			SetInventoryUseCase setInventoryUseCase,
			IdempotencyRecordPort idempotencyRecordPort,
			ObjectMapper objectMapper) {
		this.getInventoryUseCase = getInventoryUseCase;
		this.processPurchaseUseCase = processPurchaseUseCase;
		this.setInventoryUseCase = setInventoryUseCase;
		this.idempotencyRecordPort = idempotencyRecordPort;
		this.objectMapper = objectMapper;
	}

	@Operation(summary = "Consultar inventario", description = "Devuelve el inventario de un producto. Valida que el producto exista en el Products Service.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Inventario encontrado"),
			@ApiResponse(responseCode = "404", description = "Producto o inventario no encontrado"),
			@ApiResponse(responseCode = "503", description = "Products Service no disponible")
	})
	@GetMapping(value = "/inventory/{productId}", produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<JsonApiDocument<?>> getInventory(
			@Parameter(description = "UUID del producto") @PathVariable UUID productId) {
		GetInventoryResult result = getInventoryUseCase.getInventoryValidatingProduct(productId);
		GetInventoryResult.Type type = result.getType();
		if (type == GetInventoryResult.Type.SUCCESS) {
			return ResponseEntity
					.ok(JsonApiDocument.builder().data(InventoryResource.from(result.getInventory())).build());
		}
		if (type == GetInventoryResult.Type.PRODUCT_NOT_FOUND) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					errorDocument("404", result.getErrorCode(), "Producto no encontrado", result.getErrorDetail()));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(errorDocument("404", result.getErrorCode(), "Inventario no encontrado", result.getErrorDetail()));
	}

	@Operation(summary = "Crear o actualizar inventario", description = "Crea o actualiza el stock disponible de un producto. Body: {\"available\": <número >= 0}.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Inventario actualizado"),
			@ApiResponse(responseCode = "422", description = "available debe ser >= 0"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@PutMapping(value = "/inventory/{productId}", consumes = JSON_API_MEDIA_TYPE, produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<JsonApiDocument<?>> setInventory(
			@Parameter(description = "UUID del producto") @PathVariable UUID productId,
			@RequestBody java.util.Map<String, Object> body) {
		Object availableObj = body != null && body.containsKey("available") ? body.get("available") : null;
		int available = availableObj instanceof Number ? ((Number) availableObj).intValue() : 0;
		if (available < 0) {
			return ResponseEntity.badRequest()
					.body(errorDocument("422", "VALIDATION_FAILED", "available must be >= 0", null));
		}
		var inventory = setInventoryUseCase.setOrUpdateInventory(productId, available);
		return ResponseEntity.ok(JsonApiDocument.builder().data(InventoryResource.from(inventory)).build());
	}

	@Operation(summary = "Registrar compra", description = "Descuenta stock del inventario. Valida producto existe y stock suficiente. Idempotente: repetición con el mismo Idempotency-Key devuelve la misma respuesta sin descontar de nuevo.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Compra registrada"),
			@ApiResponse(responseCode = "404", description = "Producto o inventario no encontrado"),
			@ApiResponse(responseCode = "422", description = "Stock insuficiente"),
			@ApiResponse(responseCode = "409", description = "Conflicto de concurrencia (reintentar)"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@PostMapping(value = "/purchases", consumes = JSON_API_MEDIA_TYPE, produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<?> purchase(
			@Valid @RequestBody PurchaseRequest request,
			@Parameter(description = "Clave idempotente para evitar doble descuento en reintentos") @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
		UUID productId = request.getProductId();
		Integer quantity = request.getQuantity();
		if (productId == null || quantity == null) {
			return ResponseEntity.badRequest()
					.body(errorDocument("422", "VALIDATION_FAILED", "productId y quantity son requeridos.", null));
		}
		PurchaseResult result = processPurchaseUseCase.processPurchase(productId, quantity, idempotencyKey);
		Type resultType = result.getType();

		if (resultType == Type.SUCCESS) {
			if (idempotencyKey != null && !idempotencyKey.isBlank()) {
				PurchaseResource resource = PurchaseResource.from(result.getPurchase());
				String bodyJson = toJson(JsonApiDocument.builder().data(resource).build());
				IdempotencyRecord record = new IdempotencyRecord();
				record.setIdempotencyKey(idempotencyKey);
				record.setResponseStatus(201);
				record.setResponseBody(bodyJson);
				record.setCreatedAt(Instant.now());
				idempotencyRecordPort.save(record);
			}
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(JsonApiDocument.builder().data(PurchaseResource.from(result.getPurchase())).build());
		}
		if (resultType == Type.IDEMPOTENT_RESPONSE) {
			try {
				Object body = objectMapper.readValue(result.getCachedResponseBody(), JsonApiDocument.class);
				return ResponseEntity.status(result.getHttpStatus()).body(body);
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(errorDocument("500", "IDEMPOTENCY_ERROR", "Error recovering idempotent response", null));
			}
		}
		if (resultType == Type.PRODUCT_NOT_FOUND) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					errorDocument("404", result.getErrorCode(), "Producto no encontrado", result.getErrorDetail()));
		}
		if (resultType == Type.INVENTORY_NOT_FOUND) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					errorDocument("404", result.getErrorCode(), "Inventario no encontrado", result.getErrorDetail()));
		}
		if (resultType == Type.INSUFFICIENT_STOCK) {
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body(errorDocument("422", result.getErrorCode(), "Stock insuficiente", result.getErrorDetail()));
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(errorDocument("409", result.getErrorCode(), "Conflicto", result.getErrorDetail()));
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			return "{}";
		}
	}

	private JsonApiDocument<?> errorDocument(String status, String code, String title, String detail) {
		JsonApiError err = JsonApiError.builder().status(status).code(code).title(title).detail(detail).build();
		return JsonApiDocument.builder().errors(List.of(err)).build();
	}
}
