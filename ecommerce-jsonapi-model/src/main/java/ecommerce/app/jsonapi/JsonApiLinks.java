package ecommerce.app.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiLinks {
	private String first;
	private String last;
	private String prev;
	private String next;

	public static JsonApiLinksBuilder builder() {
		return new JsonApiLinksBuilder();
	}
	public String getFirst() { return first; }
	public void setFirst(String first) { this.first = first; }
	public String getLast() { return last; }
	public void setLast(String last) { this.last = last; }
	public String getPrev() { return prev; }
	public void setPrev(String prev) { this.prev = prev; }
	public String getNext() { return next; }
	public void setNext(String next) { this.next = next; }

	public static class JsonApiLinksBuilder {
		private String first, last, prev, next;
		public JsonApiLinksBuilder first(String first) { this.first = first; return this; }
		public JsonApiLinksBuilder last(String last) { this.last = last; return this; }
		public JsonApiLinksBuilder prev(String prev) { this.prev = prev; return this; }
		public JsonApiLinksBuilder next(String next) { this.next = next; return this; }
		public JsonApiLinks build() {
			JsonApiLinks l = new JsonApiLinks();
			l.setFirst(first);
			l.setLast(last);
			l.setPrev(prev);
			l.setNext(next);
			return l;
		}
	}
}
