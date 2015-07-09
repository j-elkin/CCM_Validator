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
	//Constante que define el identificador de la actividad para la lectura de c�digo QR.
	private static int SCANNER_REQUEST_CODE = 123;
	
	
	//URL para consultar la informaci�n de una persona seg�n el parametro dado (id, el cual es la cedula)
	private String URL_PERSONA = "http://ccm2015.specializedti.com/index.php/rest/persona/";
	
	//private String URL_PERSONA_ASISTENCIA = "http://ccm2015.specializedti.com/index.php/rest/persona/";
	private String URL_PERSONA_ASISTENCIA = "http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/persona/update/";
	
	//--------------------------- ATRIBUTOS -------------------------------------
	//Boton que lanza la actividad para la lectura de los c�digos qr
	private Button btnLeerCodigoQR;
	
	//Texto donde se muestran los resultados leidos en el codigo qr
	private TextView txtResultado;
	
	//Boton destinado a la validaaci�n de registro de una persona, confirmar asistencia
	private Button btnValidarRegistro;
	
	private TextView txtNombre;
	
	//Boton destinado a la asignaci�n de almuerzos a una persona. M�ximo son 4 almuerzos
	private Button btnDarAlmuerzo;
	
	//Conexion con la base de datos remota. 
	private Connection conexionMySQL;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ccmvalidator_main);
		
		iniComponents();
		
	}
	
	private void iniComponents(){
		btnLeerCodigoQR = (Button) findViewById(R.id.btnQR);
		btnLeerCodigoQR.setOnClickListener( lectura );
		
		txtResultado = (TextView) findViewById(R.id.textView1);
		
		txtNombre = (TextView) findViewById(R.id.txtNombrePersona);
		
		btnValidarRegistro = (Button) findViewById(R.id.btnValidarRegistro);
		btnValidarRegistro.setOnClickListener( asistencia );
		
		btnDarAlmuerzo = (Button) findViewById(R.id.btnDescontarAlmuerzo);
		btnDarAlmuerzo.setOnClickListener( almuerzo );
	}

	//Lectura del c�digo qr
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
				Toast.makeText(CCMValidatorMainActivity.this, "Scan Result:" + capturedQrValue, Toast.LENGTH_SHORT).show();
				
				txtResultado.setText(capturedQrValue);
				
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
			Toast.makeText(getApplicationContext(), "Pronto se validar�", Toast.LENGTH_SHORT).show();
			
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
			Toast.makeText(getApplicationContext(), "Pronto se descontar� el almuerzo", Toast.LENGTH_SHORT).show();
			
			new hiloAlmuerzo().execute("12");
		}
	};
	
	

	//*************************** hilo ***************************************
	//Clase: AsyncTask para consultar una persona seg�n el documento de identificaci�n (peticion GET)
	private class consultarPersona extends AsyncTask<String, Long, String> {
	    
		//realizamos la petici�n HTTP usando el m�todo get() que proporciona HttpRequest.java
		protected String doInBackground(String... urls) {
	        try {
	            return HttpRequest.get(urls[0]).accept("application/json")
	                    .body();//Devuelve el cuerpo de la respuesta en el metodo body()
	        } catch (HttpRequestException exception) {
	            return null;
	        }
	    }
		
		//Se captura la respuesta de la petici�n
	    protected void onPostExecute(String response) {
	        Log.i("test", prettyfyJSON(response));
	        //se hace el mapeo
	        
	        boolean nombre = prettyfyJSON(response).contains("nombre");
	        Log.d("test", "tiene nombress:"+nombre);
	       
	        Persona persona = null;
	        persona = getPersona( response );
	        //txtNombre.setText(  prettyfyJSON(response)  );
	        if(persona != null ){
	        	txtNombre.setText( persona.getNombre() +" : "+ persona.getDocPersona() );
	        	
	        	if( confirmarAsistencia( persona.getDocPersona() )  ){
	        		Log.i("test", "Asistencia confirmada");
	        	}
	        	else{
	        		Log.i("test", "no se pudo confirmar la asistencia");
	        	}
	        	
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
	
	
	//Llega un objeto JSON que obtenemos en la respuesta y �ste ser� convertido en un objeto Persona.
	private Persona getPersona(String json) {
	    Gson gson = new Gson();
	    return gson.fromJson(json, Persona.class);
	}
	
	
	
	
	//*************************** fin hilo ***************************************
	
	
	//***************************  hilo Asistencia ***************************************
	
	private class hiloAsistencia extends AsyncTask<String, Long, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			if( confirmarAsistencia( params[0] ) ){
        		Log.i("test", "Asistencia confirmada");
        	}
        	else{
        		Log.i("test", "No se pudo confirmar la asistencia");
        	}
			return null;
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
				Log.e("test","Excepci�n ocurrida:"+e.getLocalizedMessage());
			}
			
			return actualizado;
			
			
		}
	//***************************  fin hilo Asistencia ***************************************
	
	//***************************  hilo Almuerzos  ***************************************
private class hiloAlmuerzo extends AsyncTask<String, Long, String> {

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		if( registrarAlmuerzo( params[0] ) ){
    		Log.i("test", "Almuerzo registrado");
    	}
    	else{
    		Log.i("test", "NO se puede dar mas almuerzos a esta persona");
    	}
		return null;
	}
	
}


//Metodo que se encarga de actualizar el registro de asistencia de una persona
	public Boolean registrarAlmuerzo(String id){
		boolean almuerzoDado = false;
		Log.d("test", "ID persona para almuerzo:"+id);
		try {
			//Creacion de HttpClient
			HttpClient httpClient = new DefaultHttpClient();
			//hacer solicitud PUT 
			
			HttpPost httpPostNumAlmuerzos = new HttpPost("http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/almuerzo/numeroalmuerzos");
			
			String numAlmuerzos = "";
			JSONObject jsonObject = null;
			InputStream inputStream = null;
			
			// Add your data
		    List<NameValuePair> parametroConsulta = new ArrayList<NameValuePair>(2);
		    parametroConsulta.add(new BasicNameValuePair("persona_docPersona", "105936862"));
			
		    
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
		    	almuerzoDado = false;
		    	Log.i("test","Esta persona ya consumio todos los almuerzos.");
		    }
		    else{
		      	Log.i("test","Esta persona todav�a tiene almuerzos");
		    
		    	HttpPost httpPost = new HttpPost("http://192.168.173.1/Yii_CCM_WebService/web/index.php/rest/almuerzo/create");	

				// Add your data
			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			    nameValuePairs.add(new BasicNameValuePair("fecha", "2015-07-09"));
			    nameValuePairs.add(new BasicNameValuePair("hora", "16:25"));
			    nameValuePairs.add(new BasicNameValuePair("persona_docPersona", "105936862"));
			    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			    // Execute HTTP Post Request
			    HttpResponse response = httpClient.execute(httpPost);
			    
			    int statusCode = response.getStatusLine().getStatusCode();
				if(statusCode == 200 || statusCode == 201){
					almuerzoDado = true;
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
			Log.e("test","Excepci�n ocurrida Almuerzo:"+e.getLocalizedMessage());
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