package ecommerce.app.products.application.service;

import ecommerce.app.products.application.model.CreateProductCommand;
import ecommerce.app.products.application.model.ProductFilter;
import ecommerce.app.products.application.model.UpdateProductCommand;
import ecommerce.app.products.application.port.out.ProductRepositoryPort;
import ecommerce.app.products.domain.Product;
import ecommerce.app.products.domain.ProductStatus;
import ecommerce.app.products.domain.SkuAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
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
	void create_trimsSkuAndName() {
		CreateProductCommand cmd = new CreateProductCommand("  SKU-X  ", "  Product X  ", BigDecimal.ONE, ProductStatus.ACTIVE);
		when(productRepository.existsBySku("  SKU-X  ")).thenReturn(false);
		ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		Product result = service.create(cmd);

		verify(productRepository).save(captor.capture());
		assertThat(captor.getValue().getSku()).isEqualTo("SKU-X");
		assertThat(captor.getValue().getName()).isEqualTo("Product X");
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

	@Test
	void getById_whenNotExists_returnsEmpty() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		Optional<Product> result = service.getById(id);

		assertThat(result).isEmpty();
	}

	@Test
	void list_withSortDescending_usesDescOrder() {
		ProductFilter filter = new ProductFilter(Optional.empty(), Optional.empty());
		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));
		when(productRepository.findAllFiltered(eq(null), eq(null), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		Page<Product> result = service.list(filter, 1, 10, "-name");

		assertThat(result.getContent()).isEmpty();
		ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
		verify(productRepository).findAllFiltered(eq(null), eq(null), captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void list_capsPageSizeAt100() {
		ProductFilter filter = new ProductFilter(Optional.empty(), Optional.empty());
		when(productRepository.findAllFiltered(eq(null), eq(null), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

		service.list(filter, 1, 500, "name");

		ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
		verify(productRepository).findAllFiltered(eq(null), eq(null), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(100);
	}

	@Test
	void list_withNullSort_usesDefaultAscCreatedAt() {
		ProductFilter filter = new ProductFilter(Optional.empty(), Optional.empty());
		when(productRepository.findAllFiltered(eq(null), eq(null), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		service.list(filter, 1, 20, null);

		ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
		verify(productRepository).findAllFiltered(eq(null), eq(null), captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void list_withBlankSort_usesDefaultAscCreatedAt() {
		ProductFilter filter = new ProductFilter(Optional.empty(), Optional.empty());
		when(productRepository.findAllFiltered(eq(null), eq(null), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		service.list(filter, 1, 20, "   ");

		ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
		verify(productRepository).findAllFiltered(eq(null), eq(null), captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void list_withSearchFilter_trimsAndFiltersEmpty() {
		ProductFilter filter = new ProductFilter(Optional.empty(), Optional.of("  "));
		when(productRepository.findAllFiltered(eq(null), eq(null), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		service.list(filter, 1, 20, "");

		verify(productRepository).findAllFiltered(eq(null), eq(null), any(PageRequest.class));
	}

	@Test
	void list_withStatusAndSearch_callsRepositoryWithBoth() {
		ProductFilter filter = new ProductFilter(Optional.of(ProductStatus.INACTIVE), Optional.of("term"));
		when(productRepository.findAllFiltered(eq(ProductStatus.INACTIVE), eq("term"), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		Page<Product> result = service.list(filter, 1, 20, "-updatedAt");

		assertThat(result.getContent()).isEmpty();
		verify(productRepository).findAllFiltered(eq(ProductStatus.INACTIVE), eq("term"), any(PageRequest.class));
	}

	@Test
	void update_whenExistsAndSameSku_updatesProduct() {
		UUID id = UUID.randomUUID();
		Product existing = new Product();
		existing.setId(id);
		existing.setSku("SKU-1");
		existing.setName("Old");
		existing.setPrice(BigDecimal.ONE);
		existing.setStatus(ProductStatus.ACTIVE);
		UpdateProductCommand cmd = new UpdateProductCommand("SKU-1", "New Name", BigDecimal.valueOf(99), ProductStatus.INACTIVE);
		when(productRepository.findById(id)).thenReturn(Optional.of(existing));
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		Optional<Product> result = service.update(id, cmd);

		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("New Name");
		assertThat(result.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
		assertThat(result.get().getStatus()).isEqualTo(ProductStatus.INACTIVE);
		verify(productRepository).save(existing);
	}

	@Test
	void update_whenExistsAndNewSkuNotTaken_updatesProduct() {
		UUID id = UUID.randomUUID();
		Product existing = new Product();
		existing.setId(id);
		existing.setSku("SKU-1");
		UpdateProductCommand cmd = new UpdateProductCommand("SKU-2", "Name", BigDecimal.TEN, ProductStatus.ACTIVE);
		when(productRepository.findById(id)).thenReturn(Optional.of(existing));
		when(productRepository.existsBySku("SKU-2")).thenReturn(false);
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		Optional<Product> result = service.update(id, cmd);

		assertThat(result).isPresent();
		assertThat(result.get().getSku()).isEqualTo("SKU-2");
	}

	@Test
	void update_whenExistsAndNewSkuTaken_throwsSkuAlreadyExistsException() {
		UUID id = UUID.randomUUID();
		Product existing = new Product();
		existing.setId(id);
		existing.setSku("SKU-1");
		UpdateProductCommand cmd = new UpdateProductCommand("SKU-OTHER", "Name", BigDecimal.TEN, ProductStatus.ACTIVE);
		when(productRepository.findById(id)).thenReturn(Optional.of(existing));
		when(productRepository.existsBySku("SKU-OTHER")).thenReturn(true);

		assertThatThrownBy(() -> service.update(id, cmd))
				.isInstanceOf(SkuAlreadyExistsException.class)
				.hasMessageContaining("SKU-OTHER");
	}

	@Test
	void update_whenNotExists_returnsEmpty() {
		UUID id = UUID.randomUUID();
		UpdateProductCommand cmd = new UpdateProductCommand("SKU-1", "Name", BigDecimal.TEN, ProductStatus.ACTIVE);
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		Optional<Product> result = service.update(id, cmd);

		assertThat(result).isEmpty();
		verify(productRepository).findById(id);
	}

	@Test
	void deleteById_whenExists_returnsTrueAndDeletes() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.of(new Product()));

		boolean result = service.deleteById(id);

		assertThat(result).isTrue();
		verify(productRepository).deleteById(id);
	}

	@Test
	void deleteById_whenNotExists_returnsFalse() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		boolean result = service.deleteById(id);

		assertThat(result).isFalse();
		verify(productRepository).findById(id);
	}
}
