package ecommerce.app.inventory.application.service;

import ecommerce.app.inventory.application.model.GetInventoryResult;
import ecommerce.app.inventory.application.model.PurchaseResult;
import ecommerce.app.inventory.application.port.in.GetInventoryUseCase;
import ecommerce.app.inventory.application.port.in.ListPurchasesUseCase;
import ecommerce.app.inventory.application.port.in.ProcessPurchaseUseCase;
import ecommerce.app.inventory.application.port.in.SetInventoryUseCase;
import ecommerce.app.inventory.application.port.out.IdempotencyRecordPort;
import ecommerce.app.inventory.application.port.out.InventoryRepositoryPort;
import ecommerce.app.inventory.application.port.out.ProductsServicePort;
import ecommerce.app.inventory.application.port.out.PurchaseRepositoryPort;
import ecommerce.app.inventory.domain.IdempotencyRecord;
import ecommerce.app.inventory.domain.Inventory;
import ecommerce.app.inventory.domain.Purchase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de aplicación: implementa los casos de uso de inventario y compras.
 */
@Service
public class InventoryApplicationService implements GetInventoryUseCase, ProcessPurchaseUseCase, SetInventoryUseCase, ListPurchasesUseCase {

	private static final Logger log = LoggerFactory.getLogger(InventoryApplicationService.class);

	private final InventoryRepositoryPort inventoryRepository;
	private final PurchaseRepositoryPort purchaseRepository;
	private final IdempotencyRecordPort idempotencyRecordPort;
	private final ProductsServicePort productsServicePort;

	public InventoryApplicationService(
			InventoryRepositoryPort inventoryRepository,
			PurchaseRepositoryPort purchaseRepository,
			IdempotencyRecordPort idempotencyRecordPort,
			ProductsServicePort productsServicePort
	) {
		this.inventoryRepository = inventoryRepository;
		this.purchaseRepository = purchaseRepository;
		this.idempotencyRecordPort = idempotencyRecordPort;
		this.productsServicePort = productsServicePort;
	}

	@Override
	@Transactional(readOnly = true)
	public GetInventoryResult getInventoryValidatingProduct(UUID productId) {
		UUID found = productsServicePort.getProductExists(productId).block();
		if (found == null) {
			return GetInventoryResult.productNotFound();
		}
		return inventoryRepository.findById(productId)
				.map(GetInventoryResult::success)
				.orElse(GetInventoryResult.inventoryNotFound());
	}

	@Override
	@Transactional
	public PurchaseResult processPurchase(UUID productId, int quantity, String idempotencyKey) {
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			Optional<IdempotencyRecord> existing = idempotencyRecordPort.findByIdempotencyKey(idempotencyKey);
			if (existing.isPresent()) {
				return PurchaseResult.fromIdempotency(existing.get());
			}
		}

		UUID found = productsServicePort.getProductExists(productId).block();
		if (found == null) {
			return PurchaseResult.productNotFound();
		}

		Optional<Inventory> invOpt = inventoryRepository.findByProductIdForUpdate(productId);
		if (invOpt.isEmpty()) {
			return PurchaseResult.inventoryNotFound();
		}
		Inventory inv = invOpt.get();
		if (inv.getAvailable() < quantity) {
			return PurchaseResult.insufficientStock(inv.getAvailable());
		}

		inv.setAvailable(inv.getAvailable() - quantity);
		try {
			inventoryRepository.saveAndFlush(inv);
		} catch (jakarta.persistence.OptimisticLockException e) {
			log.warn("Optimistic lock conflict for product {}", productId);
			return PurchaseResult.conflict("Concurrencia: otro proceso modificó el inventario. Reintente.");
		}

		Purchase purchase = new Purchase();
		purchase.setId(UUID.randomUUID());
		purchase.setProductId(productId);
		purchase.setQuantity(quantity);
		purchase.setIdempotencyKey(idempotencyKey);
		purchase.setProcessedAt(Instant.now());
		purchaseRepository.save(purchase);

		log.info("InventoryChanged productId={} quantity={} availableAfter={} purchaseId={}",
				productId, quantity, inv.getAvailable(), purchase.getId());

		return PurchaseResult.success(purchase);
	}

	@Override
	@Transactional
	public Inventory setOrUpdateInventory(UUID productId, int available) {
		Inventory inv = inventoryRepository.findById(productId).orElse(null);
		if (inv == null) {
			inv = new Inventory();
			inv.setProductId(productId);
			inv.setAvailable(available);
			inv.setReserved(0);
			inv.setVersion(0L);
		} else {
			inv.setAvailable(available);
		}
		return inventoryRepository.save(inv);
	}

	@Override
	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<Purchase> list(int pageNumber, int pageSize) {
		int size = Math.min(Math.max(1, pageSize), 100);
		int zeroBased = Math.max(0, pageNumber <= 0 ? 0 : pageNumber - 1);
		Pageable pageable = PageRequest.of(zeroBased, size);
		return purchaseRepository.findAll(pageable);
	}
}
