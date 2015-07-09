package com.example.ccmvalidator;

public class Persona {
	
	private String docPersona;
	private String nombre;
	private String apellidos;
	private String genero;
	private String correo_electronico;
	private String telefono;
	private String codigo_qr;
	private String fecha_nacimiento;
	private String asistio;
	private int tipo_doc_idtipo_doc;
	private int pais_procedencia_idpais_procedencia;
	private int institucion_idinstitucion;
	
	public Persona(String docPersona, String nombre, String apellidos, String genero, String correo_electronico,
			String telefono, String codigo_qr, String fecha_nacimiento, String asistio, int tipo_doc_idtipo_doc,
			int pais_procedencia_idpais_procedencia, int institucion_idinstitucion) {
		super();
		this.docPersona = docPersona;
		this.nombre = nombre;
		this.apellidos = apellidos;
		this.genero = genero;
		this.correo_electronico = correo_electronico;
		this.telefono = telefono;
		this.codigo_qr = codigo_qr;
		this.fecha_nacimiento = fecha_nacimiento;
		this.asistio = asistio;
		this.tipo_doc_idtipo_doc = tipo_doc_idtipo_doc;
		this.pais_procedencia_idpais_procedencia = pais_procedencia_idpais_procedencia;
		this.institucion_idinstitucion = institucion_idinstitucion;
	}
	
	public String getDocPersona() {
		return docPersona;
	}
	public void setDocPersona(String docPersona) {
		this.docPersona = docPersona;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getApellidos() {
		return apellidos;
	}
	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}
	public String getGenero() {
		return genero;
	}
	public void setGenero(String genero) {
		this.genero = genero;
	}
	public String getCorreo_electronico() {
		return correo_electronico;
	}
	public void setCorreo_electronico(String correo_electronico) {
		this.correo_electronico = correo_electronico;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getCodigo_qr() {
		return codigo_qr;
	}
	public void setCodigo_qr(String codigo_qr) {
		this.codigo_qr = codigo_qr;
	}
	public String getFecha_nacimiento() {
		return fecha_nacimiento;
	}
	public void setFecha_nacimiento(String fecha_nacimiento) {
		this.fecha_nacimiento = fecha_nacimiento;
	}
	public String getAsistio() {
		return asistio;
	}
	public void setAsistio(String asistio) {
		this.asistio = asistio;
	}
	public int getTipo_doc_idtipo_doc() {
		return tipo_doc_idtipo_doc;
	}
	public void setTipo_doc_idtipo_doc(int tipo_doc_idtipo_doc) {
		this.tipo_doc_idtipo_doc = tipo_doc_idtipo_doc;
	}
	public int getPais_procedencia_idpais_procedencia() {
		return pais_procedencia_idpais_procedencia;
	}
	public void setPais_procedencia_idpais_procedencia(int pais_procedencia_idpais_procedencia) {
		this.pais_procedencia_idpais_procedencia = pais_procedencia_idpais_procedencia;
	}
	public int getInstitucion_idinstitucion() {
		return institucion_idinstitucion;
	}
	public void setInstitucion_idinstitucion(int institucion_idinstitucion) {
		this.institucion_idinstitucion = institucion_idinstitucion;
	}
	
	

}
