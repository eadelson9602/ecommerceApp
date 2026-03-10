package ecommerce.app.inventory.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiError {
	private String status;
	private String code;
	private String title;
	private String detail;

	public static JsonApiErrorBuilder builder() {
		return new JsonApiErrorBuilder();
	}
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDetail() { return detail; }
	public void setDetail(String detail) { this.detail = detail; }

	public static class JsonApiErrorBuilder {
		private String status, code, title, detail;
		public JsonApiErrorBuilder status(String status) { this.status = status; return this; }
		public JsonApiErrorBuilder code(String code) { this.code = code; return this; }
		public JsonApiErrorBuilder title(String title) { this.title = title; return this; }
		public JsonApiErrorBuilder detail(String detail) { this.detail = detail; return this; }
		public JsonApiError build() {
			JsonApiError e = new JsonApiError();
			e.setStatus(status);
			e.setCode(code);
			e.setTitle(title);
			e.setDetail(detail);
			return e;
		}
	}
}
