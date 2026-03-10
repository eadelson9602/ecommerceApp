package ecommerce.app.inventory.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency")
public class IdempotencyRecord {

	@Id
	@Column(name = "idempotency_key")
	private String idempotencyKey;

	@Column(name = "response_status", nullable = false)
	private int responseStatus;

	@Column(name = "response_body", columnDefinition = "TEXT")
	private String responseBody;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public String getIdempotencyKey() { return idempotencyKey; }
	public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
	public int getResponseStatus() { return responseStatus; }
	public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
	public String getResponseBody() { return responseBody; }
	public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
