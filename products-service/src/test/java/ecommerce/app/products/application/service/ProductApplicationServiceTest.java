package ecommerce.app.products.application.service;

import ecommerce.app.products.application.model.CreateProductCommand;
import ecommerce.app.products.application.model.ProductFilter;
import ecommerce.app.products.application.port.out.ProductRepositoryPort;
import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

	@Mock
	private ProductRepositoryPort productRepository;

	@InjectMocks
	private ProductApplicationService service;

	@Test
	void create_whenSkuNotExists_savesAndReturnsProduct() {
		CreateProductCommand cmd = new CreateProductCommand("SKU-1", "Product 1", BigDecimal.TEN, ProductStatus.ACTIVE);
		when(productRepository.existsBySku("SKU-1")).thenReturn(false);
		Product saved = new Product();
		saved.setId(UUID.randomUUID());
		saved.setSku("SKU-1");
		saved.setName("Product 1");
		saved.setPrice(BigDecimal.TEN);
		saved.setStatus(ProductStatus.ACTIVE);
		when(productRepository.save(any(Product.class))).thenReturn(saved);

		Product result = service.create(cmd);

		assertThat(result.getSku()).isEqualTo("SKU-1");
		verify(productRepository).save(any(Product.class));
	}

	@Test
	void create_whenSkuExists_throwsSkuAlreadyExistsException() {
		CreateProductCommand cmd = new CreateProductCommand("SKU-1", "Product 1", BigDecimal.TEN, ProductStatus.ACTIVE);
		when(productRepository.existsBySku("SKU-1")).thenReturn(true);

		assertThatThrownBy(() -> service.create(cmd))
				.isInstanceOf(SkuAlreadyExistsException.class)
				.hasMessageContaining("SKU-1");
	}

	@Test
	void getById_whenExists_returnsProduct() {
		UUID id = UUID.randomUUID();
		Product p = new Product();
		p.setId(id);
		p.setSku("SKU-1");
		when(productRepository.findById(id)).thenReturn(Optional.of(p));

		Optional<Product> result = service.getById(id);

		assertThat(result).isPresent();
		assertThat(result.get().getSku()).isEqualTo("SKU-1");
	}

	@Test
	void list_returnsPageFromRepository() {
		ProductFilter filter = new ProductFilter(Optional.of(ProductStatus.ACTIVE), Optional.empty());
		PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"));
		when(productRepository.findAllFiltered(ProductStatus.ACTIVE, null, pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		Page<Product> result = service.list(filter, 1, 20, "createdAt");

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}
}
