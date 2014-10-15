package principal;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

	private  String mRutaArchivos;
	private long mVersionUdadPedido;
	private long mVersionCupones;
	private Set<Path> mIdiomasDisponibles;
	private int mMaxConexiones;
	private ServerSocket mServerSocket;
	private NumberFormat mDivisa;
    private AtomicInteger mConexActuales=new AtomicInteger(0);
    private static final String SEPARADOR=File.separator;
    
    public Servidor(String rutaArch,int maxConex,int puerto) throws Exception{
    	//mRutaArchivos=rutaArch;
    	mRutaArchivos="e:"+SEPARADOR+"pruebasMenuServer"+SEPARADOR;
    	mMaxConexiones=maxConex;
		obtenVersion();
		mIdiomasDisponibles=obtenIdiomas();
		mServerSocket=new ServerSocket(puerto);
		mDivisa=obtenDivisa();
		
		
    }

	public void arranca() throws IOException {
		
		
		while (true){
			
			TrataMensajeClteEnServidor trabajo=new TrataMensajeClteEnServidor(mServerSocket.accept(),mVersionUdadPedido,
					mVersionCupones,mIdiomasDisponibles,this,mDivisa);
			if (mConexActuales.get()<mMaxConexiones){
				Thread tareaCliente=new Thread(trabajo);
				tareaCliente.setName("Cliente"+ mConexActuales.get()+1);
				tareaCliente.start();
			} else {
				trabajo=null;
			}
		}
		
	}
	public void incrementaNumConex(){
		mConexActuales.addAndGet(1);
	}
	
	public void decrementaNumConex(){
		mConexActuales.addAndGet(-1);
	}
	
	private Set<Path> obtenIdiomas() throws IOException{
		HashSet<Path> respuesta=new HashSet<Path>();
		Path rutaIdiomas=Paths.get(mRutaArchivos+"Catalogos");
		
		DirectoryStream<Path> stream = Files.newDirectoryStream(rutaIdiomas); 
		for (Path entrada: stream) {
			if (Files.isDirectory(entrada, LinkOption.NOFOLLOW_LINKS)){
				respuesta.add(entrada);
	        }
		}
		return respuesta;

	}

	private void obtenVersion() throws FileNotFoundException, IOException{
			DataInputStream dais=new DataInputStream(new FileInputStream(mRutaArchivos+"versionUdadPedido"));
			mVersionUdadPedido=dais.readLong();
			dais.close();
			
			dais=new DataInputStream(new FileInputStream(mRutaArchivos+"versionCupones"));
			mVersionCupones=dais.readLong();
			dais.close();
			
	}
	
	private NumberFormat obtenDivisa() throws ClassNotFoundException,FileNotFoundException, IOException{
		
		
			NumberFormat respuesta;
			ObjectInputStream ois=new ObjectInputStream(new FileInputStream(mRutaArchivos+"divisa"));
			respuesta=(NumberFormat) ois.readObject();
			ois.close();
			return respuesta;
		
		
	}
	

}
