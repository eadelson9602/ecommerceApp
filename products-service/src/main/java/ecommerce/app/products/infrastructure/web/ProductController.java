package ecommerce.app.products.infrastructure.web;

import ecommerce.app.jsonapi.JsonApiDocument;
import ecommerce.app.jsonapi.JsonApiError;
import ecommerce.app.jsonapi.JsonApiLinks;
import ecommerce.app.products.application.model.CreateProductCommand;
import ecommerce.app.products.application.model.ProductFilter;
import ecommerce.app.products.application.model.UpdateProductCommand;
import ecommerce.app.products.application.port.in.*;
import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador HTTP (entrada): expone los casos de uso vía REST en formato JSON:API.
 * Documentación interactiva: Swagger UI (/swagger-ui.html).
 */
@Tag(name = "Products", description = "CRUD de productos y listado con paginación, filtros y ordenación")
@RestController
@RequestMapping("/api/products")
public class ProductController {

	public static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";

	private final ListProductsUseCase listProductsUseCase;
	private final GetProductUseCase getProductUseCase;
	private final CreateProductUseCase createProductUseCase;
	private final UpdateProductUseCase updateProductUseCase;
	private final DeleteProductUseCase deleteProductUseCase;

	public ProductController(
			ListProductsUseCase listProductsUseCase,
			GetProductUseCase getProductUseCase,
			CreateProductUseCase createProductUseCase,
			UpdateProductUseCase updateProductUseCase,
			DeleteProductUseCase deleteProductUseCase
	) {
		this.listProductsUseCase = listProductsUseCase;
		this.getProductUseCase = getProductUseCase;
		this.createProductUseCase = createProductUseCase;
		this.updateProductUseCase = updateProductUseCase;
		this.deleteProductUseCase = deleteProductUseCase;
	}

	@Operation(summary = "Listar productos", description = "Listado paginado con filtro por status, búsqueda por sku/name y orden por price o createdAt (prefijo '-' para DESC).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de productos"),
			@ApiResponse(responseCode = "401", description = "No autenticado"),
			@ApiResponse(responseCode = "403", description = "Acceso denegado")
	})
	@GetMapping(produces = JSON_API_MEDIA_TYPE)
	public JsonApiDocument<List<ProductResource>> list(
			@Parameter(description = "ACTIVE o INACTIVE") @RequestParam(required = false) String status,
			@Parameter(description = "Búsqueda por sku o nombre") @RequestParam(name = "filter[search]", required = false) String search,
			@Parameter(description = "Número de página (base 1)") @RequestParam(name = "page[number]", defaultValue = "1") int pageNumber,
			@Parameter(description = "Tamaño de página (máx 100)") @RequestParam(name = "page[size]", defaultValue = "20") int pageSize,
			@Parameter(description = "Orden: price, -price, createdAt, -createdAt") @RequestParam(required = false) String sort
	) {
		Optional<ProductStatus> statusOpt = Optional.ofNullable(status)
				.filter(s -> !s.isBlank())
				.map(String::toUpperCase)
				.filter(s -> "ACTIVE".equals(s) || "INACTIVE".equals(s))
				.map(ProductStatus::valueOf);
		ProductFilter filter = new ProductFilter(statusOpt, Optional.ofNullable(search));
		var page = listProductsUseCase.list(filter, pageNumber, pageSize, sort);
		String base = "/api/products?page[number]=%d&page[size]=%d";
		if (status != null) base += "&status=" + status;
		if (search != null) base += "&filter[search]=" + search;
		if (sort != null) base += "&sort=" + sort;
		JsonApiLinks links = JsonApiLinks.builder()
				.first(String.format(base, 1, pageSize))
				.last(String.format(base, page.getTotalPages(), pageSize))
				.prev(page.hasPrevious() ? String.format(base, page.getNumber(), pageSize) : null)
				.next(page.hasNext() ? String.format(base, page.getNumber() + 2, pageSize) : null)
				.build();
		List<ProductResource> content = page.getContent().stream().map(ProductResource::from).toList();
		return JsonApiDocument.<List<ProductResource>>builder()
				.data(content)
				.links(links)
				.meta(java.util.Map.of("totalRecords", page.getTotalElements()))
				.build();
	}

	@Operation(summary = "Obtener producto por ID", description = "Devuelve un producto por su UUID.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Producto encontrado"),
			@ApiResponse(responseCode = "404", description = "Producto no encontrado"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping(value = "/{id}", produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<JsonApiDocument<?>> getById(@Parameter(description = "UUID del producto") @PathVariable UUID id) {
		return getProductUseCase.getById(id)
				.map(product -> ResponseEntity.<JsonApiDocument<?>>ok(JsonApiDocument.builder().data(ProductResource.from(product)).build()))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDocument("404", "NOT_FOUND", "Resource not found", "Product not found for id: " + id)));
	}

	@Operation(summary = "Crear producto", description = "Crea un producto. SKU debe ser único; si se repite se devuelve 409 Conflict.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Producto creado"),
			@ApiResponse(responseCode = "409", description = "SKU ya existe"),
			@ApiResponse(responseCode = "422", description = "Validación fallida (ej. price < 0)"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@PostMapping(consumes = JSON_API_MEDIA_TYPE, produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<JsonApiDocument<?>> create(@Valid @RequestBody ProductDataWrapper wrapper) {
		ProductRequest req = wrapper.getAttributes();
		if (req == null) {
			return ResponseEntity.badRequest().body(errorDocument("422", "VALIDATION_FAILED", "Unprocessable Entity", "data.attributes is required"));
		}
		try {
			CreateProductCommand command = new CreateProductCommand(
					req.getSku().trim(),
					req.getName().trim(),
					req.getPrice(),
					ProductStatus.valueOf(req.getStatus())
			);
			Product created = createProductUseCase.create(command);
			return ResponseEntity.status(HttpStatus.CREATED).body(JsonApiDocument.builder().data(ProductResource.from(created)).build());
		} catch (SkuAlreadyExistsException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDocument("409", "SKU_ALREADY_EXISTS", "Conflict", "Ya existe un producto con el SKU indicado."));
		}
	}

	@Operation(summary = "Actualizar producto", description = "Actualiza un producto existente por UUID. SKU único; 409 si se duplica.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Producto actualizado"),
			@ApiResponse(responseCode = "404", description = "Producto no encontrado"),
			@ApiResponse(responseCode = "409", description = "SKU ya existe"),
			@ApiResponse(responseCode = "422", description = "Validación fallida"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@PatchMapping(value = "/{id}", consumes = JSON_API_MEDIA_TYPE, produces = JSON_API_MEDIA_TYPE)
	public ResponseEntity<JsonApiDocument<?>> update(
			@Parameter(description = "UUID del producto") @PathVariable UUID id,
			@Valid @RequestBody ProductDataWrapper wrapper) {
		ProductRequest req = wrapper.getAttributes();
		if (req == null) {
			return ResponseEntity.badRequest().body(errorDocument("422", "VALIDATION_FAILED", "Unprocessable Entity", "data.attributes is required"));
		}
		try {
			UpdateProductCommand command = new UpdateProductCommand(
					req.getSku().trim(),
					req.getName().trim(),
					req.getPrice(),
					ProductStatus.valueOf(req.getStatus())
			);
			return updateProductUseCase.update(id, command)
					.map(product -> ResponseEntity.<JsonApiDocument<?>>ok(JsonApiDocument.builder().data(ProductResource.from(product)).build()))
					.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDocument("404", "NOT_FOUND", "Resource not found", "Product not found for id: " + id)));
		} catch (SkuAlreadyExistsException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDocument("409", "SKU_ALREADY_EXISTS", "Conflict", "Ya existe un producto con el SKU indicado."));
		}
	}

	@Operation(summary = "Eliminar producto", description = "Elimina un producto por UUID. Respuesta 204 sin cuerpo.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Eliminado correctamente"),
			@ApiResponse(responseCode = "404", description = "Producto no encontrado"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<JsonApiDocument<?>> delete(@Parameter(description = "UUID del producto") @PathVariable UUID id) {
		if (!deleteProductUseCase.deleteById(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDocument("404", "NOT_FOUND", "Resource not found", "Product not found for id: " + id));
		}
		return ResponseEntity.noContent().build();
	}

	private JsonApiDocument<?> errorDocument(String status, String code, String title, String detail) {
		return JsonApiDocument.builder()
				.errors(List.of(JsonApiError.builder()
						.status(status)
						.code(code)
						.title(title)
						.detail(detail)
						.build()))
				.build();
	}
}
