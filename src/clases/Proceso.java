package clases;

public class Proceso {
    private int ID;
    private char operacion;
    private double dato1;
    private double dato2;
    private int tiempoEstimado;
    private double resultado;
    private boolean error;
    private boolean interrumpido;
    private int contadorBloqueado;
    private int tiempoRestante;
    public  Proceso(){
        this.ID = 0;
        this.operacion = ' ';
        this.dato1 = 0;
        this.dato2 = 0;
        this.tiempoEstimado = 0;
        this.resultado = 0;
        this.error = false;
        this.tiempoRestante = 0;
        this.contadorBloqueado = 0;
        this.interrumpido = false;
    }
    public void establecerID(int ID){
        this.ID = ID;
    }
    public void establecerOperacion(char operacion){
        this.operacion = operacion;
    }
    public void establecerDato1(double dato1){
        this.dato1 = dato1;
    }
    public void establecerDato2(double dato2){
        this.dato2 = dato2;
    }
    public void establecerTiempoEstimado(int tiempoEstimado){
        this.tiempoEstimado = tiempoEstimado;
    }
    public void establecerError(boolean error){
        this.error = error;
    }
    
    public void establecerInterrumpido(boolean interrumpido){
        this.interrumpido = interrumpido;
    }
    public void establecerTiempoRestante(int tiempoRestante){
        this.tiempoRestante = tiempoRestante;
    }
    public void incrementarContador(){
        this.contadorBloqueado++;
    }
    public int obtenerID(){
        return ID;
    }
    public char obtenerOperacion(){
        return operacion;
    }
    public double obtenerDato1(){
        return dato1;
    } 
    public double obtenerDato2(){
        return dato2;
    }
    public int obtenerTiempoEstimado(){
        return tiempoEstimado;
    }
    public double obtenerResultado(){
        return realizarOperacon();
    }
    public boolean hayError(){
        return error;
    }
    public int obtenerTiempoRestante(){
        return this.tiempoRestante;
    }
    
    public boolean obtenerInterrumpido(){
        return this.interrumpido;
    }
    
    public int obtenerContador(){
        return this.contadorBloqueado;
    }
    public void restablecerContador(){
         this.contadorBloqueado = 0;
    }
    private double realizarOperacon(){        
        switch(this.obtenerOperacion()){   
            case '+' -> resultado = dato1 + dato2;
            case '-' -> resultado = dato1 - dato2;
            case '*' -> resultado = dato1 * dato2;
            case '/' -> resultado = dato1 / dato2;
            case '%' -> resultado = dato1 % dato2;
            case '^' -> resultado = Math.pow(dato1,dato2);
        }       
        return resultado;
    }
}
