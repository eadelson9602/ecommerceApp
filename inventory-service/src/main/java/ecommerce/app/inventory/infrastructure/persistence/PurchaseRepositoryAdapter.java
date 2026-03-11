package ecommerce.app.inventory.infrastructure.persistence;

import ecommerce.app.inventory.application.port.out.PurchaseRepositoryPort;
import ecommerce.app.inventory.domain.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRepositoryAdapter implements PurchaseRepositoryPort {

	private final PurchaseJpaRepository jpaRepository;

	public PurchaseRepositoryAdapter(PurchaseJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Purchase save(Purchase purchase) {
		return jpaRepository.save(purchase);
	}

	@Override
	public Page<Purchase> findAll(Pageable pageable) {
		return jpaRepository.findAllByOrderByProcessedAtDesc(pageable);
	}
}
