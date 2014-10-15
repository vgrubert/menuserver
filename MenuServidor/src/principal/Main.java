package principal;

import java.io.File;

public class Main {
	/*
	private static String rutaArchivos;
	private static long version;
	private static Set<Path> idiomasDisponibles;
	private static final int MAX_CONEXIONES=50;
	private static ServerSocket mServerSocket;
    private static AtomicInteger conexActuales=new AtomicInteger(0);
	*/
	public static void main(String[] args) {
		/*
		 * Argumentos
		 * 1� ruta archivos
		 * 2� max num conexiones (maximo valor 100, m�nimo 10)
		 * 3� puerto del servidor, (opcional, por defecto 50000)
		 * 
		 */

		String rutaArchivos=args[0];
		String separador=File.separator;
		if (!rutaArchivos.endsWith(separador)){rutaArchivos=rutaArchivos+separador;}
		int maxNumConex;
		try{
			 maxNumConex=Math.max(10, Math.min(Integer.valueOf(args[1]).intValue(),100));
			} catch (Exception e) {
			 maxNumConex=20;
			}
		int puerto=50000;
		if (args.length>2){
			puerto=Integer.valueOf(args[2]).intValue();
		} 
		
		try {
			new Servidor(rutaArchivos,maxNumConex,puerto).arranca();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	 
	 
	 
	 

	
	
	

}
