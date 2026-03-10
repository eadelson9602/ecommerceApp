package ecommerce.app.inventory.application.service;

import ecommerce.app.inventory.application.model.GetInventoryResult;
import ecommerce.app.inventory.application.model.PurchaseResult;
import ecommerce.app.inventory.application.port.out.IdempotencyRecordPort;
import ecommerce.app.inventory.application.port.out.InventoryRepositoryPort;
import ecommerce.app.inventory.application.port.out.ProductsServicePort;
import ecommerce.app.inventory.application.port.out.PurchaseRepositoryPort;
import ecommerce.app.inventory.domain.IdempotencyRecord;
import ecommerce.app.inventory.domain.Inventory;
import ecommerce.app.inventory.domain.Purchase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

	private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Mock
	private InventoryRepositoryPort inventoryRepository;

	@Mock
	private PurchaseRepositoryPort purchaseRepository;

	@Mock
	private IdempotencyRecordPort idempotencyRecordPort;

	@Mock
	private ProductsServicePort productsServicePort;

	@InjectMocks
	private InventoryApplicationService service;

	// --- getInventoryValidatingProduct ---

	@Test
	void getInventory_whenProductNotExists_returnsProductNotFound() {
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.empty());

		GetInventoryResult result = service.getInventoryValidatingProduct(PRODUCT_ID);

		assertThat(result.getType()).isEqualTo(GetInventoryResult.Type.PRODUCT_NOT_FOUND);
		assertThat(result.getInventory()).isNull();
	}

	@Test
	void getInventory_whenProductExistsButNoInventory_returnsInventoryNotFound() {
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.just(PRODUCT_ID));
		when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

		GetInventoryResult result = service.getInventoryValidatingProduct(PRODUCT_ID);

		assertThat(result.getType()).isEqualTo(GetInventoryResult.Type.INVENTORY_NOT_FOUND);
	}

	@Test
	void getInventory_whenProductAndInventoryExist_returnsSuccess() {
		Inventory inv = new Inventory();
		inv.setProductId(PRODUCT_ID);
		inv.setAvailable(10);
		inv.setReserved(0);
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.just(PRODUCT_ID));
		when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inv));

		GetInventoryResult result = service.getInventoryValidatingProduct(PRODUCT_ID);

		assertThat(result.getType()).isEqualTo(GetInventoryResult.Type.SUCCESS);
		assertThat(result.getInventory()).isSameAs(inv);
		assertThat(result.getInventory().getAvailable()).isEqualTo(10);
	}

	// --- setOrUpdateInventory ---

	@Test
	void setInventory_whenNoExistingInventory_createsNew() {
		when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
		Inventory saved = new Inventory();
		saved.setProductId(PRODUCT_ID);
		saved.setAvailable(50);
		saved.setReserved(0);
		when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

		Inventory result = service.setOrUpdateInventory(PRODUCT_ID, 50);

		assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(result.getAvailable()).isEqualTo(50);
		assertThat(result.getReserved()).isZero();
		verify(inventoryRepository).save(any(Inventory.class));
	}

	@Test
	void setInventory_whenInventoryExists_updatesAvailable() {
		Inventory existing = new Inventory();
		existing.setProductId(PRODUCT_ID);
		existing.setAvailable(10);
		existing.setReserved(2);
		when(inventoryRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
		when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

		Inventory result = service.setOrUpdateInventory(PRODUCT_ID, 25);

		assertThat(result.getAvailable()).isEqualTo(25);
		assertThat(result.getReserved()).isEqualTo(2);
		verify(inventoryRepository).save(existing);
	}

	// --- processPurchase ---

	@Test
	void processPurchase_whenProductNotExists_returnsProductNotFound() {
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.empty());

		PurchaseResult result = service.processPurchase(PRODUCT_ID, 1, null);

		assertThat(result.getType()).isEqualTo(PurchaseResult.Type.PRODUCT_NOT_FOUND);
	}

	@Test
	void processPurchase_whenInventoryNotFound_returnsInventoryNotFound() {
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.just(PRODUCT_ID));
		when(inventoryRepository.findByProductIdForUpdate(PRODUCT_ID)).thenReturn(Optional.empty());

		PurchaseResult result = service.processPurchase(PRODUCT_ID, 1, null);

		assertThat(result.getType()).isEqualTo(PurchaseResult.Type.INVENTORY_NOT_FOUND);
	}

	@Test
	void processPurchase_whenInsufficientStock_returnsInsufficientStock() {
		Inventory inv = new Inventory();
		inv.setProductId(PRODUCT_ID);
		inv.setAvailable(2);
		inv.setReserved(0);
		inv.setVersion(0L);
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.just(PRODUCT_ID));
		when(inventoryRepository.findByProductIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(inv));

		PurchaseResult result = service.processPurchase(PRODUCT_ID, 5, null);

		assertThat(result.getType()).isEqualTo(PurchaseResult.Type.INSUFFICIENT_STOCK);
		assertThat(result.getErrorDetail()).contains("2");
	}

	@Test
	void processPurchase_whenIdempotencyKeyRepeated_returnsIdempotentResponse() {
		IdempotencyRecord record = new IdempotencyRecord();
		record.setIdempotencyKey("key-1");
		record.setResponseStatus(201);
		record.setResponseBody("{\"data\":{}}");
		record.setCreatedAt(Instant.now());
		when(idempotencyRecordPort.findByIdempotencyKey("key-1")).thenReturn(Optional.of(record));

		PurchaseResult result = service.processPurchase(PRODUCT_ID, 1, "key-1");

		assertThat(result.getType()).isEqualTo(PurchaseResult.Type.IDEMPOTENT_RESPONSE);
		assertThat(result.getHttpStatus()).isEqualTo(201);
		assertThat(result.getCachedResponseBody()).isEqualTo("{\"data\":{}}");
		verify(productsServicePort, never()).getProductExists(any());
	}

	@Test
	void processPurchase_whenValid_decrementsStockAndSavesPurchase() {
		Inventory inv = new Inventory();
		inv.setProductId(PRODUCT_ID);
		inv.setAvailable(10);
		inv.setReserved(0);
		inv.setVersion(0L);
		when(productsServicePort.getProductExists(PRODUCT_ID)).thenReturn(Mono.just(PRODUCT_ID));
		when(inventoryRepository.findByProductIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(inv));
		when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

		PurchaseResult result = service.processPurchase(PRODUCT_ID, 3, null);

		assertThat(result.getType()).isEqualTo(PurchaseResult.Type.SUCCESS);
		assertThat(result.getPurchase()).isNotNull();
		assertThat(result.getPurchase().getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(result.getPurchase().getQuantity()).isEqualTo(3);
		assertThat(inv.getAvailable()).isEqualTo(7);

		ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
		verify(purchaseRepository).save(purchaseCaptor.capture());
		Purchase saved = purchaseCaptor.getValue();
		assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(saved.getQuantity()).isEqualTo(3);
		assertThat(saved.getProcessedAt()).isNotNull();
	}
}
