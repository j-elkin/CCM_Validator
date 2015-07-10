package com.example.ccmvalidator;

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
	
	//Conexion con la base de datos remota. 
	private Connection conexionMySQL;
	
	//Se encarga de mostrar el dialogo emergente mientras se procesa la petición al servidor
	private ProgressDialog progresDialogRing;
	
	private Context context;
	
	private AlertDialog.Builder alertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ccmvalidator_main);
		context = this;
		iniComponents();
		
	}
	
	private void iniComponents(){
		btnLeerCodigoQR = (Button) findViewById(R.id.btnQR);
		btnLeerCodigoQR.setOnClickListener( lectura );
		
		txtResultado = (TextView) findViewById(R.id.textView1);
		txtResultado.setVisibility(View.INVISIBLE);
		
		txtNombre = (TextView) findViewById(R.id.txtNombrePersona);
		txtNombre.setVisibility(View.INVISIBLE);
		
		btnValidarRegistro = (Button) findViewById(R.id.btnValidarRegistro);
		btnValidarRegistro.setOnClickListener( asistencia );
		
		btnDarAlmuerzo = (Button) findViewById(R.id.btnDescontarAlmuerzo);
		btnDarAlmuerzo.setOnClickListener( almuerzo );
	}

	//Lectura del código qr
	OnClickListener lectura = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "Me pulsaste!", Toast.LENGTH_SHORT).show();
			Log.d("test", "Me pulsaste!");
			
			String packageString = "com.google.zxing.client.android"; //Lanza directame la app Barcode Scanner
			//String packageString = "com.example.ccmvalidator"; //Lanza directame la app CCM Validator
			//Intent intentQR = new Intent(CCMValidatorMainActivity.this, com.google.zxing.client.android.CaptureActivity.class);
			Intent intentQR = new Intent("com.google.zxing.client.android.SCAN"); 
			intentQR.setPackage(packageString);
			intentQR.putExtra("SCAN_MODE", "QR_CODE_MODE");  
		    startActivityForResult(intentQR, SCANNER_REQUEST_CODE); 
		
		
		}
	};
	
	//Captura del documento de identidad proporcionado por el codigo QR
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SCANNER_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// Handle successful scan
				String capturedQrValue = intent.getStringExtra("SCAN_RESULT");
				// String format =
				intent.getStringExtra("SCAN_RESULT_FORMAT");
				Toast.makeText(CCMValidatorMainActivity.this, "ID:" + capturedQrValue, Toast.LENGTH_SHORT).show();
				
				txtResultado.setText(capturedQrValue);
				txtResultado.setVisibility(View.VISIBLE);
				
				String url = URL_PERSONA + capturedQrValue;
				new consultarPersona().execute( url );
				
				Log.i("test", "resultado:"+capturedQrValue);
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
	
	
	
	OnClickListener asistencia = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "Pronto se validará", Toast.LENGTH_SHORT).show();
			
			String url = URL_PERSONA + txtResultado.getText();
			
			//Log.d("test","url:"+url);
			
			/////new consultarPersona().execute( url );
			
			new hiloAsistencia().execute("1");
			
			
		}
	};
	
	OnClickListener almuerzo = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "Pronto se descontará el almuerzo", Toast.LENGTH_SHORT).show();
			
			String id = ""+txtResultado.getText();
			new hiloAlmuerzo().execute( id );
		}
	};
	
	

	//*************************** hilo ***************************************
	//Clase: AsyncTask para consultar una persona según el documento de identificación (peticion GET)
	private class consultarPersona extends AsyncTask<String, Long, String> {
	    
		//Antes de realizar la petición al servidor se muestra un ProgressDialog 
		@Override
		protected void onPreExecute(){
			progresDialogRing = new ProgressDialog( context );
			progresDialogRing.setTitle("Consultando registro...");
			progresDialogRing.setMessage("Por favor espere.");
			progresDialogRing.setCancelable(false);
			progresDialogRing.setIndeterminate(true);
			progresDialogRing.show();
		}
		
		//realizamos la petición HTTP usando el método get() que proporciona HttpRequest.java
		@Override
		protected String doInBackground(String... urls) {
			Log.d("test", "URL recivida:"+urls[0]);
	        try {
	            return HttpRequest.get(urls[0]).accept("application/json")
	                    .body();//Devuelve el cuerpo de la respuesta en el metodo body()
	        } catch (HttpRequestException exception) {
	            return null;
	        }
	    }
		
		//Se captura la respuesta de la petición
		@Override
	    protected void onPostExecute(String response) {
	    	
	    	
	        Log.i("test", prettyfyJSON(response));
	        //se hace el mapeo
	        
	        boolean nombre = prettyfyJSON(response).contains("nombre");
	        Log.d("test", "tiene nombres:"+nombre);
	       
	        if(nombre){
        
		        Persona persona = getPersona( response );
		        //txtNombre.setText(  prettyfyJSON(response)  );
		        if(persona != null ){
		        	txtNombre.setText( persona.getNombre() +" "+ persona.getApellidos() );
		        	txtNombre.setVisibility(View.VISIBLE);
		        	
		        	/*if( confirmarAsistencia( persona.getDocPersona() )  ){
		        		Log.i("test", "Asistencia confirmada");
		        	}
		        	else{
		        		Log.i("test", "no se pudo confirmar la asistencia");
		        	}*/
		        	
		        }
		    }
	        else{
	        	Log.d("test", "Persona no registrada");
	        	alertDialog = new AlertDialog.Builder( context );
	        	alertDialog.setCancelable( false );
	        	alertDialog.setPositiveButton("Persona no registrada", null);
	        	alertDialog.show();
	        	
	        	txtNombre.setText("No registrado");
	        	
	        }
	        
	        if(progresDialogRing.isShowing()){
	    		progresDialogRing.dismiss();
	    	}
	    	
	        else{
	        	txtNombre.setText("No registrado");
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
			progresDialogRing = new ProgressDialog( context );
			progresDialogRing.setTitle("Confirmando asistencia...");
			progresDialogRing.setMessage("Por favor espere.");
			progresDialogRing.setCancelable(false);
			progresDialogRing.setIndeterminate(true);
			progresDialogRing.show();
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			boolean asistencia = confirmarAsistencia( params[0] );
			if( asistencia ){
        		Log.i("test", "Asistencia confirmada");
        	}
        	else{
        		Log.i("test", "No se pudo confirmar la asistencia");
        	}
			return asistencia;
		}
		
		@Override
		protected void onPostExecute(Boolean asistencia){
			if(progresDialogRing.isShowing()){
				progresDialogRing.dismiss();
			}
			
			if(asistencia){
				alertDialog = new AlertDialog.Builder( context );
		    	alertDialog.setCancelable( false );
		    	alertDialog.setPositiveButton("Asistencia confirmada con éxito", null);
		    	alertDialog.show();
			}
			else{
				alertDialog = new AlertDialog.Builder( context );
		    	alertDialog.setCancelable( false );
		    	alertDialog.setPositiveButton("No se pudo confirmar la asistencia", null);
		    	alertDialog.show();
			}
		}
	}
	
	
	//Metodo que se encarga de actualizar el registro de asistencia de una persona
		public Boolean confirmarAsistencia(String id){
			boolean actualizado = false;
			Log.d("test", "ID person:"+id);
			try {
				//Creacion de HttpClient
				HttpClient httpClient = new DefaultHttpClient();
				//hacer solicitud PUT 
				String url = URL_PERSONA_ASISTENCIA + id;
				Log.e("test",url);
				HttpPut httpPut = new HttpPut( url );
					
				
				List<NameValuePair> parametroAsistencia = new ArrayList<NameValuePair>(2);
				parametroAsistencia.add(new BasicNameValuePair("asistio", "BIEN!") );
				
				httpPut.setEntity(new UrlEncodedFormEntity( parametroAsistencia) );
				
				//Ejecutar la peticion PUT
				HttpResponse httpResponse = httpClient.execute(httpPut);
				
				//actualizado = true;
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
		progresDialogRing = new ProgressDialog( context );
		progresDialogRing.setTitle("Registrando almuerzo...");
		progresDialogRing.setMessage("Por favor espere.");
		progresDialogRing.setCancelable(false);
		progresDialogRing.setIndeterminate(true);
		progresDialogRing.show();
	}
	
	
	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		String respuesta = registrarAlmuerzo( params[0] );
		if( respuesta.equals("true") ){
    		Log.i("test", "Almuerzo registrado");
    	}
    	else{
    		Log.i("test", "NO se puede dar mas almuerzos a esta persona");
    	}
		return respuesta;
	}
	
	@Override
	protected void onPostExecute(String almuerzo){
		if(progresDialogRing.isShowing()){
			progresDialogRing.dismiss();
		}
		
		if(almuerzo.equals("true")){
			alertDialog = new AlertDialog.Builder( context );
	    	alertDialog.setCancelable( false );
	    	alertDialog.setPositiveButton("Almuerzo registrado", null);
	    	alertDialog.show();
		}
		else if( almuerzo.equals("false")){
			alertDialog = new AlertDialog.Builder( context );
	    	alertDialog.setCancelable( false );
	    	alertDialog.setPositiveButton("No se pudo registrar el almuerzo, por favor intente de nuevo.", null);
	    	alertDialog.show();
		}
		else{
			alertDialog = new AlertDialog.Builder( context );
	    	alertDialog.setCancelable( false );
	    	alertDialog.setPositiveButton("La persona ya consumio todos los almuerzos", null);
	    	alertDialog.show();
		}
	}
	
}


//Metodo que se encarga de actualizar el registro de asistencia de una persona
	public String registrarAlmuerzo(String id){
		String almuerzoDado = "false";
		Log.d("test", "ID persona para almuerzo:"+id);
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

				// Add your data
			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			    nameValuePairs.add(new BasicNameValuePair("fecha", "2015-07-09"));
			    nameValuePairs.add(new BasicNameValuePair("hora", "22:45"));
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
