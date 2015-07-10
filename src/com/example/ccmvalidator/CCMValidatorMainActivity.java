package com.example.ccmvalidator;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.example.ccmvalidator.request.HttpRequest;
import com.example.ccmvalidator.request.HttpRequest.HttpRequestException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.client.android.CaptureActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;


public class CCMValidatorMainActivity extends ActionBarActivity {
	
	//--------------------------- CONSTANTES ----------------------------------
	//Constante que define el identificador de la actividad para la lectura de código QR.
	private static int SCANNER_REQUEST_CODE = 123;
	
	
	//URL para consultar la información de una persona según el parametro dado (id, el cual es la cedula)
	private String URL_PERSONA = "http://ccm2015.specializedti.com/index.php/rest/persona/";
	
	private String URL_PERSONA_ASISTENCIA = "http://ccm2015.specializedti.com/index.php/rest/persona/update/";
	//private String URL_PERSONA_ASISTENCIA = "http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/persona/update/";
	
	//private String URL_NUM_ALMUERZOS = "http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/almuerzo/numeroalmuerzos";
	private String URL_NUM_ALMUERZOS = "http://ccm2015.specializedti.com/index.php/rest/almuerzo/numeroalmuerzos";
	
	//private String URL_REGISTRAR_ALMUERZO = "http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/almuerzo/create";
	private String URL_REGISTRAR_ALMUERZO = "http://ccm2015.specializedti.com/index.php/rest/almuerzo/create";
	
	
	//--------------------------- ATRIBUTOS -------------------------------------
	//Boton que lanza la actividad para la lectura de los códigos qr
	private Button btnLeerCodigoQR;
	
	//Texto donde se muestran los resultados leidos en el codigo qr
	private TextView txtResultado;
	
	//Boton destinado a la validaación de registro de una persona, confirmar asistencia
	private Button btnValidarRegistro;
	
	private TextView txtNombre;
	
	//Boton destinado a la asignación de almuerzos a una persona. Máximo son 4 almuerzos
	private Button btnDarAlmuerzo;
	
	//Se encarga de mostrar el dialogo emergente mientras se procesa la petición al servidor
	private ProgressDialog progresDialogRing;
	
	private Context context;
	
	//Alerta de que se muestra en la interfaz según las respuetas del server
	private AlertDialog.Builder alertDialog;
	
	//Permite saber si la lectura hecha por codigo QR corresponde a un documento de identidad registrado en el web service
	private boolean registroConfirmado = false;
	
	//Formateador para la fecha y hora actual
	private SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ccmvalidator_main);
	
		getSupportActionBar().setIcon( R.drawable.ic_launcher );
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		
		context = this;
		iniComponents();
		
	}
	
	private void iniComponents(){
		btnLeerCodigoQR = (Button) findViewById(R.id.btnQR);
		btnLeerCodigoQR.setOnClickListener( lectura );
		
		txtResultado = (TextView) findViewById(R.id.textView1);
		//txtResultado.setVisibility(View.INVISIBLE);
		
		txtNombre = (TextView) findViewById(R.id.txtNombrePersona);
		//txtNombre.setVisibility(View.INVISIBLE);
		
		btnValidarRegistro = (Button) findViewById(R.id.btnValidarRegistro);
		btnValidarRegistro.setOnClickListener( asistencia );
		
		btnDarAlmuerzo = (Button) findViewById(R.id.btnDescontarAlmuerzo);
		btnDarAlmuerzo.setOnClickListener( almuerzo );
		
		progresDialogRing = new ProgressDialog( context );
		alertDialog = new AlertDialog.Builder( context );
	}

	//Lectura del código qr
	OnClickListener lectura = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			if( hayInternet() ){

				String packageString = "com.google.zxing.client.android"; //Lanza directame la app Barcode Scanner
				//String packageString = "com.example.ccmvalidator"; //Lanza directame la app CCM Validator
				//Intent intentQR = new Intent(CCMValidatorMainActivity.this, com.google.zxing.client.android.CaptureActivity.class);
				Intent intentQR = new Intent("com.google.zxing.client.android.SCAN"); 
				intentQR.setPackage(packageString);
				intentQR.putExtra("SCAN_MODE", "QR_CODE_MODE");  
			    startActivityForResult(intentQR, SCANNER_REQUEST_CODE); 
			}
			else{
				mostrarAlertDialog("Dispositivo sin conexión:", "Por favor verifica la conexión a Internet.");
			}
		
		}
	};
	
	//Captura del documento de identidad proporcionado por el codigo QR
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SCANNER_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// Handle successful scan
				String capturedQrValue = intent.getStringExtra("SCAN_RESULT");
				// String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
				
				Toast.makeText(CCMValidatorMainActivity.this, "ID:" + capturedQrValue, Toast.LENGTH_LONG).show();
				
				txtResultado.setText(capturedQrValue);
				Log.d("test", "ID:"+capturedQrValue);
				//txtResultado.setVisibility(View.VISIBLE);
				
				String url = URL_PERSONA + capturedQrValue;
				
				//Llamado al hilo para consultar el registro de una persona de acuerdo a la lectura del codigo qr, 
				//el cual debe leer un documento de identidad
				new consultarPersona().execute( url );
				
				
			} 
			else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
				Log.e("test", "Cancelado!");
			}

		}
		 else {
			Log.e("test", "No hubo respuesta");
		}
	}
	
	
	//Realiza la confirmación de asistencia de una persona
	OnClickListener asistencia = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
		
			if( hayInternet() ){
				if(registroConfirmado){
					String idPersona = ""+txtResultado.getText();
					new hiloAsistencia().execute( idPersona );
				}
				else{
					mostrarAlertDialog("Un momento!", "Por favor escanea un código QR");
				}
			}
			else{
				mostrarAlertDialog("Dispositivo sin conexión:", "Por favor verifica la conexión a Internet.");
			}
			
			
		}
	};
	
	//Realiza el registro de un almuerzo a la persona dada por documento de identidad (codigo qr)
	OnClickListener almuerzo = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if( hayInternet() ){
				if(registroConfirmado){
					String id = ""+txtResultado.getText();
					new hiloAlmuerzo().execute( id );
				}
				else{
					mostrarAlertDialog("Un momento!", "Por favor escanea un código QR");
				}
			}
			else{
				mostrarAlertDialog("Dispositivo sin conexión:", "Por favor verifica la conexión a Internet.");
			}
		}
	};
	
	
	//Lanza el dialogo de espera de respuesta del servidor
	public void lanzarProgresDialog(String titulo, String mensaje){
		
		progresDialogRing.setTitle( titulo );
		progresDialogRing.setIcon( R.drawable.ic_launcher );
		progresDialogRing.setMessage( mensaje );
		progresDialogRing.setCancelable(false);
		progresDialogRing.setIndeterminate(true);
		progresDialogRing.show();
	}
	
	//Lanza el alert diaglo que muestra las respuetas del servidor
	public void mostrarAlertDialog(String titulo, String mensaje ){
		
		alertDialog.setTitle( titulo );
		alertDialog.setIcon( R.drawable.ic_launcher );
		//alertDialog.setMessage( mensaje );
		alertDialog.setCancelable( false );
    	alertDialog.setPositiveButton( mensaje, null);
    	alertDialog.show();
	}

	//*************************** hilo ***************************************
	//Clase: AsyncTask para consultar una persona según el documento de identificación (peticion GET)
	private class consultarPersona extends AsyncTask<String, Long, String> {
	    
		//Antes de realizar la petición al servidor se muestra un ProgressDialog 
		@Override
		protected void onPreExecute(){
			
			lanzarProgresDialog( "Consultando registro...", "Por favor espere.");
			
		}
		
		//realizamos la petición HTTP usando el método get() que proporciona HttpRequest.java
		@Override
		protected String doInBackground(String... urls) {
			Log.d("test", "URL recivida:"+urls[0]);
	        try {
	            return HttpRequest
	            		.get(urls[0])
	            		.accept("application/json")
	                    .body();//Devuelve el cuerpo de la respuesta en el metodo body()
	        } 
	        catch (HttpRequestException exception) {
	            return "NULL";
	        }
	    }
		
		//Se captura la respuesta de la petición
		@Override
	    protected void onPostExecute(String response) {
	    	
			if(response.equals("NULL")){
				registroConfirmado = false;
				mostrarAlertDialog( "Oops!", "No se pudo confirmar el registro. Intente nuevamente escaneando el código QR.");
			}
			else{
	    	
		        Log.i("test", prettyfyJSON(response));
		        //se hace el mapeo
		        
		        boolean nombre = prettyfyJSON(response).contains("nombre");
		        Log.d("test", "tiene nombre:"+nombre);
		       
		        if(nombre){
		        	//se obtiene un objeto persona
			        Persona persona = getPersona( response );
			        
			        if(persona != null ){
			        	txtNombre.setText( persona.getNombre() +" "+ persona.getApellidos() );
			        	//txtNombre.setVisibility(View.VISIBLE);
			        	registroConfirmado = true;
			        }
			    }
		        else{
		        	Log.d("test", "Persona no registrada");
		        	
		        	mostrarAlertDialog( "Consulta del registro:", "La persona no está registrada.");
		        	
		        	txtNombre.setText("Persona no registrada.");
		        	
		        	registroConfirmado = false;
		        }
		    }
			
			if(progresDialogRing.isShowing()){
	    		progresDialogRing.dismiss();
	    	}
		}
	}
	
	
	//Formatea el objeto JSON consultado por el web service para mostrarlo en pantalla
	private String prettyfyJSON(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json);
        return gson.toJson(element);
	}
	
	
	//Llega un objeto JSON que obtenemos en la respuesta y éste será convertido en un objeto Persona.
	private Persona getPersona(String json) {
	    Gson gson = new Gson();
	    return gson.fromJson(json, Persona.class);
	}
	
	
	
	
	//*************************** fin hilo ***************************************
	
	
	//***************************  hilo Asistencia ***************************************
	
	private class hiloAsistencia extends AsyncTask<String, Long, Boolean> {

		//Antes de realizar la petición al servidor se muestra un ProgressDialog 
		@Override
		protected void onPreExecute(){
			
			lanzarProgresDialog("Confirmando asistencia...", "Por favor espere.");
			
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			
			return  confirmarAsistencia( params[0] );

		}
		
		@Override
		protected void onPostExecute(Boolean asistencia){
			if(progresDialogRing.isShowing()){
				progresDialogRing.dismiss();
			}
			
			if(asistencia){
				mostrarAlertDialog("Confirmación validada:", "Asistencia confirmada con éxito.");
        		registroConfirmado = false;
        		txtResultado.setText( R.string.documento );
        		txtNombre.setText( R.string.nombreCompleto );
			}
			else{
				mostrarAlertDialog("Confirmación rechazada:", "No se pudo confirmar la asistencia. Intente nuevamente.");
			}
		}
	}
	
	
	//Metodo que se encarga de actualizar el registro de asistencia de una persona
	public Boolean confirmarAsistencia(String id){
		boolean actualizado = false;
		//Log.d("test", "ID person:"+id);
		try {
			//Creacion de HttpClient
			HttpClient httpClient = new DefaultHttpClient();
			//hacer solicitud PUT 
			String url = URL_PERSONA_ASISTENCIA + id;
			Log.e("test",url);
			HttpPut httpPut = new HttpPut( url );
			
			List<NameValuePair> parametroAsistencia = new ArrayList<NameValuePair>(2);
			parametroAsistencia.add(new BasicNameValuePair("asistio", "SI") );
			
			httpPut.setEntity(new UrlEncodedFormEntity( parametroAsistencia) );
			
			//Ejecutar la peticion PUT
			HttpResponse httpResponse = httpClient.execute(httpPut);
			
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode == 200 || statusCode == 201){
				actualizado = true;
				Log.e("test", "Status Code:" + statusCode);
			}
			else{
				Log.e("test", "Mensaje Error:" + statusCode);
			}
			
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("test","Excepción ocurrida:"+e.getLocalizedMessage());
		}
		
		return actualizado;
		
		
	}
	//***************************  fin hilo Asistencia ***************************************
	
	//***************************  hilo Almuerzos  ***************************************
private class hiloAlmuerzo extends AsyncTask<String, Long, String> {

	//Antes de realizar la petición al servidor se muestra un ProgressDialog 
	@Override
	protected void onPreExecute(){
	
		lanzarProgresDialog("Registrando almuerzo...", "Por favor espere.");

	}
	
	
	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		String respuesta = registrarAlmuerzo( params[0] );
		if( respuesta.equals("true") ){
    		Log.i("test", "Almuerzo registrado");
    	}
    	else{
    		Log.i("test", "No se puede dar mas almuerzos a esta persona");
    	}
		return respuesta;
	}
	
	@Override
	protected void onPostExecute(String almuerzo){
		if(progresDialogRing.isShowing()){
			progresDialogRing.dismiss();
		}
		
		if(almuerzo.equals("true")){
			mostrarAlertDialog("Registro exitoso:", "El almuerzo fue registrado.");
			registroConfirmado = false;
    		txtResultado.setText( R.string.documento );
    		txtNombre.setText( R.string.nombreCompleto );
			
		}
		else if( almuerzo.equals("false")){
			mostrarAlertDialog("Registro fracasado:", "No se pudo registrar el almuerzo, por favor intente de nuevo.");
			
		}
		else{
			mostrarAlertDialog("Almuerzo no disponible:", "Esta persona ya consumio todos los almuerzos.");
		}
	}
	
}


//Metodo que se encarga de actualizar el registro de asistencia de una persona
	public String registrarAlmuerzo(String id){
		String almuerzoDado = "false";
		try {
			//Creacion de HttpClient
			HttpClient httpClient = new DefaultHttpClient();
			//hacer solicitud PUT 
			
			HttpPost httpPostNumAlmuerzos = new HttpPost(URL_NUM_ALMUERZOS);
			
			String numAlmuerzos = "";
			JSONObject jsonObject = null;
			InputStream inputStream = null;
			
			// Add your data
		    List<NameValuePair> parametroConsulta = new ArrayList<NameValuePair>(2);
		    parametroConsulta.add(new BasicNameValuePair("persona_docPersona", id));
			
		    
		    httpPostNumAlmuerzos.setEntity( new UrlEncodedFormEntity(parametroConsulta));
		    HttpResponse responceA = httpClient.execute( httpPostNumAlmuerzos );
		    HttpEntity entity = responceA.getEntity();
		    inputStream = entity.getContent();
		    BufferedReader reader = new BufferedReader( new InputStreamReader(inputStream, "UTF8") );
		    String linea = reader.readLine();
		    while( linea != null ){
		    	numAlmuerzos = linea;
		    	linea = reader.readLine();
		    }
		    Log.d("test", "Cantidad almuerzos:"+numAlmuerzos);
		    
		    if ( inputStream != null ){
				try {
					inputStream.close();
				} 
				catch (IOException error) {
					Log.i( "IOException finally: ", error.getMessage() );
				}
			}
		    
		    numAlmuerzos = numAlmuerzos.replaceAll("\"", "");
		    
		    if(Integer.valueOf(numAlmuerzos) >= 4){
		    	almuerzoDado = "almuerzosConsumidos";
		    	Log.i("test","Esta persona ya consumio todos los almuerzos.");
		    }
		    else{
		      	Log.i("test","Esta persona todavía tiene almuerzos");
		    
		    	HttpPost httpPost = new HttpPost(URL_REGISTRAR_ALMUERZO);	
		    	
		    	
		    	String fechaHoraActual = sDateFormat.format(new Date());
		    	Log.d("test", "FechaHora actual:"+fechaHoraActual);
		    	
		    	String [] fecha_Hora = fechaHoraActual.split("_");
		    	String fechaActual = fecha_Hora[0];
		    	String horaActual = fecha_Hora[1];
		    	
				// Add your data
			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			    nameValuePairs.add(new BasicNameValuePair("fecha", fechaActual));
			    nameValuePairs.add(new BasicNameValuePair("hora", horaActual));
			    nameValuePairs.add(new BasicNameValuePair("persona_docPersona", id));
			    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			    // Execute HTTP Post Request
			    HttpResponse response = httpClient.execute(httpPost);
			    
			    int statusCode = response.getStatusLine().getStatusCode();
				if(statusCode == 200 || statusCode == 201){
					almuerzoDado = "true";
					Log.e("test", "Status Code:" + statusCode);
				}
				else{
					Log.e("test", "Mensaje Error:" + statusCode);
				}
					
		    }	
			
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("test","Excepción ocurrida Almuerzo:"+e.getLocalizedMessage());
		}
		
		return almuerzoDado;
		
		
	}

	//***************************  fin hilo Almuerzos  ***************************************

	//Métdodo para verificar el estado de Internet
    //IMPORTANTE: Para verificar la conexion a Internet es necerario agregar el siguiente permiso en el Manifest:
    // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    public boolean hayInternet(){
    	ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService( Context.CONNECTIVITY_SERVICE );
 	    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
 	    if ( netInfo == null ){
 	    	return false;
 	    }
 	    else if ( !netInfo.isConnected() ){
 	    	return false;
 	    }
 	    else if ( !netInfo.isAvailable() ){
 	    	return false;
 	    }
 	    else{
 	    	return true;
 	    }
    }
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ccmvalidator_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		/*if (id == R.id.action_settings) {
			return true;
		}*/
		if(id == R.id.btnSalir_app){
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
