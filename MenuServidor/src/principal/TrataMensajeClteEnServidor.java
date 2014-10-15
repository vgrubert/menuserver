package principal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Set;

public class TrataMensajeClteEnServidor implements Runnable {

	private Set<Path> mIdiomasDisponibles;
	private long mVersionUdadPedido;
	private Socket mSocket;
	private Servidor mServidor;
	private NumberFormat mDivisa;
	private long mVersionCupones;
	private String mDirectorioImagenes;
	private static final String SEPARADOR=File.separator;

	public TrataMensajeClteEnServidor(Socket accept, long versionUdadPedido,long versionCupones,
			Set<Path> idiomasDisponibles, Servidor servidor,NumberFormat divisa) {
		mSocket=accept;
		mIdiomasDisponibles=idiomasDisponibles;
		mVersionUdadPedido=versionUdadPedido;
		mVersionCupones=versionCupones;
		mServidor=servidor;
		mDivisa=divisa;
		
		
	}

	@Override
	public void run() {
		mServidor.incrementaNumConex();
		
		try {
			
			ObjectOutputStream canalACliente=new ObjectOutputStream(mSocket.getOutputStream());
			ObjectInputStream canalDeCliente = new ObjectInputStream(mSocket.getInputStream());
			String idiomaSolicCliente=canalDeCliente.readUTF();
			long versionClienteUnidadesPedido=canalDeCliente.readLong();
			long versionClienteCupones=canalDeCliente.readLong();
			
			String directorioFicherosCliente=fijaIdiomaADevolverACliente(idiomaSolicCliente);
			
			//Los 3 arrays siguientes almacenan la respuesta de cliente con indicación de las imágenes que necesita actualizar. Iconos tipos, udadPedido y cupones
			
			Object iconos=null;
			Object udPedido=null;
			Object cupones=null;
			
			
			//Comunicamos a cliente divisa del catálogo
			canalACliente.writeObject(mDivisa);
			canalACliente.flush();
			if (mVersionUdadPedido==versionClienteUnidadesPedido){
				//Respondemos al cliente que ud pedido están actualizadas y que pasamos a actualizar cupones
				//1º respondemos cliente
				canalACliente.writeBoolean(false);
				//2º Pasamos a actualizar cupones
				cupones=actualizaCupones(canalDeCliente,canalACliente,directorioFicherosCliente,versionClienteCupones);
			} else{
				//2º Respondemos al cliente que sí que hay Unidades de producto a actualizar
				canalACliente.writeBoolean(true);
				canalACliente.flush();
				//Indicamos a cliente nVersion udadPedido
				canalACliente.writeLong(mVersionUdadPedido);
				canalACliente.flush();
				//Enviamos catálogo actualizado a cliente
				//TODO Penmsarse se sería mejor mandar udad a producto a udad producto individualmente para
				//que en cliente se fueran añadiendo al catálogo en una tarea separada
				
				ObjectInputStream ois=new ObjectInputStream(new FileInputStream(directorioFicherosCliente+"udPedido"));
				Object catCliente=ois.readObject();
				ois.close();
				canalACliente.writeObject(catCliente);
				canalACliente.flush();
				iconos=canalDeCliente.readObject();
				udPedido=canalDeCliente.readObject();
				cupones=actualizaCupones(canalDeCliente,canalACliente,directorioFicherosCliente,versionClienteCupones);
			}
			
			//Enviamos imágenes a actualizar a cliente
			//TODO hay q arreglar pq falta un tipo q es el tipo cupones
			if(iconos!=null){
				String[] icoImagen=(String[]) iconos;
				enviaImagenesCliente(icoImagen,directorioFicherosCliente,canalACliente);
			}
			if (cupones!=null){
				String[] cuponesAniadir=(String[])cupones;
				enviaImagenesCliente(cuponesAniadir,directorioFicherosCliente,canalACliente);
			}
			if (udPedido!=null){
				String[] udPedidoImagen=(String[])udPedido;
				enviaImagenesCliente(udPedidoImagen,directorioFicherosCliente,canalACliente);
			}
			canalDeCliente.close();
			canalACliente.close();
			
		} catch (Exception e) {
			//NO hacemos nada, se cierra el socket y listo
		}
		
		try {
			mSocket.close();
		} catch (IOException e1) {
		}
		mServidor.decrementaNumConex();
		
	}

	

	

	private void enviaImagenesCliente(String[] icoImagen,
			String directorioFicherosCliente, ObjectOutputStream canalACliente) {
		for (int i=0;i<icoImagen.length;i++){
			try {
				canalACliente.writeObject(leeFicheroImagen(mDirectorioImagenes+icoImagen[i]));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	private byte[] leeFicheroImagen(String nombreFichero)  {
		File f=new File(nombreFichero);
		int filesize=(int)f.length();
		byte[] buffer=new byte[filesize];
		BufferedInputStream instream;
		try {
			instream = new BufferedInputStream(new FileInputStream(f));
			for (int i=0; i < filesize; i++) {
				buffer[i]=(byte)instream.read();
			}
			instream.close();
			return buffer;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		
	}

	private Object actualizaCupones(ObjectInputStream canalDeCliente,
			ObjectOutputStream canalACliente,String directorioFicherosCliente, long versionClienteCupones) throws Exception {
		
		if (mVersionCupones==versionClienteCupones){
			//Respondemos al cliente que cupones están actualizados
			canalACliente.writeBoolean(false);
			canalACliente.flush();
			return null;
		}
		
		//Respondemos al cliente que sí que hay cupones a actualizar
		canalACliente.writeBoolean(true);
		canalACliente.flush();
		
		//Indicamos a cliente nVersion cupones
		canalACliente.writeLong(mVersionCupones);
		canalACliente.flush();
		
		//Enviamos catálogo cupones actualizado a cliente
		//TODO Pensarse se sería mejor mandar udad a producto a udad producto individualmente para
		//que en cliente se fueran añadiendo al catálogo en una tarea separada
		ObjectInputStream ois=new ObjectInputStream(new FileInputStream(directorioFicherosCliente+"cupones"));
		Object catCliente=ois.readObject();
		ois.close();
		canalACliente.writeObject(catCliente);
		canalACliente.flush();
		return (String[]) canalDeCliente.readObject();
	}

	private String fijaIdiomaADevolverACliente(String idiomaSolicCliente) throws Exception {
		Iterator<Path> it=mIdiomasDisponibles.iterator();
		String alternativo="en";
		Path alternatPath=null;
		if (idiomaSolicCliente.equals("ca")|idiomaSolicCliente.equals("gl")|idiomaSolicCliente.equals("eu")){
			alternativo="es";
		}
		
		while (it.hasNext()){
			Path p=it.next();
			
			if (p.endsWith(idiomaSolicCliente)){
				mDirectorioImagenes=p.getParent().toString()+SEPARADOR;
				return (p.toString()+SEPARADOR);
			} else if (p.endsWith(alternativo)) {
				alternatPath=p;
			}
		}
		
		if (alternatPath==null){ throw new Exception("Idioma no disponible");}
		mDirectorioImagenes=alternatPath.getParent().toString()+SEPARADOR;
		return (alternatPath.toString()+SEPARADOR);

		
	}

	

}
