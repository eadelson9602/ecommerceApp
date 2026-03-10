package ecommerce.app.jsonapi;

public class JsonApiErrorSource {
	private String pointer;
	private String parameter;

	public JsonApiErrorSource() {}
	public JsonApiErrorSource(String pointer, String parameter) {
		this.pointer = pointer;
		this.parameter = parameter;
	}
	public String getPointer() { return pointer; }
	public void setPointer(String pointer) { this.pointer = pointer; }
	public String getParameter() { return parameter; }
	public void setParameter(String parameter) { this.parameter = parameter; }
}
