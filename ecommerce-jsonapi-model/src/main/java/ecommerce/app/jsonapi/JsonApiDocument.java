package ecommerce.app.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiDocument<T> {
	private T data;
	private List<JsonApiError> errors;
	private Map<String, Object> meta;
	private JsonApiLinks links;

	public static <T> JsonApiDocumentBuilder<T> builder() {
		return new JsonApiDocumentBuilder<>();
	}

	public T getData() { return data; }
	public void setData(T data) { this.data = data; }
	public List<JsonApiError> getErrors() { return errors; }
	public void setErrors(List<JsonApiError> errors) { this.errors = errors; }
	public Map<String, Object> getMeta() { return meta; }
	public void setMeta(Map<String, Object> meta) { this.meta = meta; }
	public JsonApiLinks getLinks() { return links; }
	public void setLinks(JsonApiLinks links) { this.links = links; }

	public static class JsonApiDocumentBuilder<T> {
		private T data;
		private List<JsonApiError> errors;
		private Map<String, Object> meta;
		private JsonApiLinks links;

		public JsonApiDocumentBuilder<T> data(T data) { this.data = data; return this; }
		public JsonApiDocumentBuilder<T> errors(List<JsonApiError> errors) { this.errors = errors; return this; }
		public JsonApiDocumentBuilder<T> meta(Map<String, Object> meta) { this.meta = meta; return this; }
		public JsonApiDocumentBuilder<T> links(JsonApiLinks links) { this.links = links; return this; }
		public JsonApiDocument<T> build() {
			JsonApiDocument<T> d = new JsonApiDocument<>();
			d.setData(data);
			d.setErrors(errors);
			d.setMeta(meta);
			d.setLinks(links);
			return d;
		}
	}
}
