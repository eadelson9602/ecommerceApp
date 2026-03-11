package ecommerce.app.inventory.infrastructure.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetInventoryRequestTest {

	@Test
	void getAvailableValue_whenDataAttributesAvailableSet_returnsNestedValue() {
		SetInventoryRequest request = new SetInventoryRequest();
		request.setAvailable(10);
		SetInventoryRequest.DataHolder data = new SetInventoryRequest.DataHolder();
		SetInventoryRequest.Attributes attrs = new SetInventoryRequest.Attributes();
		attrs.setAvailable(5);
		data.setAttributes(attrs);
		request.setData(data);

		assertThat(request.getAvailableValue()).isEqualTo(5);
	}

	@Test
	void getAvailableValue_whenDataNull_returnsAvailable() {
		SetInventoryRequest request = new SetInventoryRequest();
		request.setAvailable(7);

		assertThat(request.getAvailableValue()).isEqualTo(7);
	}

	@Test
	void getAvailableValue_whenDataAndAvailableNull_returnsZero() {
		SetInventoryRequest request = new SetInventoryRequest();

		assertThat(request.getAvailableValue()).isZero();
	}

	@Test
	void getAvailableValue_whenDataAttributesNull_returnsTopLevelAvailable() {
		SetInventoryRequest request = new SetInventoryRequest();
		request.setAvailable(3);
		SetInventoryRequest.DataHolder data = new SetInventoryRequest.DataHolder();
		data.setAttributes(null);
		request.setData(data);

		assertThat(request.getAvailableValue()).isEqualTo(3);
	}

	@Test
	void getAvailableValue_whenDataAttributesAvailableNull_returnsTopLevelAvailable() {
		SetInventoryRequest request = new SetInventoryRequest();
		request.setAvailable(2);
		SetInventoryRequest.DataHolder data = new SetInventoryRequest.DataHolder();
		SetInventoryRequest.Attributes attrs = new SetInventoryRequest.Attributes();
		attrs.setAvailable(null);
		data.setAttributes(attrs);
		request.setData(data);

		assertThat(request.getAvailableValue()).isEqualTo(2);
	}
}
