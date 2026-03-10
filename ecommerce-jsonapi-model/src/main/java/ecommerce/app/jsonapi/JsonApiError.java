package ecommerce.app.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiError {
	private String id;
	private String status;
	private String code;
	private String title;
	private String detail;
	private JsonApiErrorSource source;
	private Object meta;

	public static JsonApiErrorBuilder builder() {
		return new JsonApiErrorBuilder();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDetail() { return detail; }
	public void setDetail(String detail) { this.detail = detail; }
	public JsonApiErrorSource getSource() { return source; }
	public void setSource(JsonApiErrorSource source) { this.source = source; }
	public Object getMeta() { return meta; }
	public void setMeta(Object meta) { this.meta = meta; }

	public static class JsonApiErrorBuilder {
		private String id, status, code, title, detail;
		private JsonApiErrorSource source;
		private Object meta;
		public JsonApiErrorBuilder id(String id) { this.id = id; return this; }
		public JsonApiErrorBuilder status(String status) { this.status = status; return this; }
		public JsonApiErrorBuilder code(String code) { this.code = code; return this; }
		public JsonApiErrorBuilder title(String title) { this.title = title; return this; }
		public JsonApiErrorBuilder detail(String detail) { this.detail = detail; return this; }
		public JsonApiErrorBuilder source(JsonApiErrorSource source) { this.source = source; return this; }
		public JsonApiErrorBuilder meta(Object meta) { this.meta = meta; return this; }
		public JsonApiError build() {
			JsonApiError e = new JsonApiError();
			e.setId(id);
			e.setStatus(status);
			e.setCode(code);
			e.setTitle(title);
			e.setDetail(detail);
			e.setSource(source);
			e.setMeta(meta);
			return e;
		}
	}
}
