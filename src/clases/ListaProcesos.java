package clases;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class ListaProcesos {
    Queue<Proceso> colaProcesos;
    private final char[] OPERACIONES = {'+','-','*','/','%'};
    
    public ListaProcesos(int cantidadProcesos){
        colaProcesos = new LinkedList<>();
        llenarColaProcesos(cantidadProcesos);
    }
    
    private void llenarColaProcesos(int cantidadProcesos){
        for(int i = 1; i <= cantidadProcesos; i++){
            Proceso p = new Proceso();
            p.establecerID(i);
            p.establecerDato1(new Random().nextInt(100)+1);
            p.establecerDato2(new Random().nextInt(100)+1);
            p.establecerOperacion(OPERACIONES[new Random().nextInt(5)]);
            p.establecerTiempoEstimado(new Random().nextInt(12)+7);
            this.colaProcesos.offer(p);
        }
    }
    
    public Queue<Proceso> getColaProcesos(){
        return this.colaProcesos;
    }
}