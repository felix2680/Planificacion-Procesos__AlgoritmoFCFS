package clases.ventanas;

import clases.Proceso;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;

public class EjecutarProcesos extends JFrame implements KeyListener {

    Queue<Proceso> colaNuevos;
    Queue<Proceso> colaListos;
    Queue<Proceso> colaBloqueados;
    private static Timer timerAnimation;
    private int contadorGlobal = 0;
    private int numProcesosPendientes = 0;
    private int tiempoRestante = 0;
    private int tiempoEstimado = 0;
    private int tiempoTranscurrido = 0;
    private boolean procesoPausado = false;
    private boolean hayError = false;
    private boolean hayInterrupcion = false;
    private final int MAXIMO_MEMORIA = 3;
    private int contadorBloqueo;

    public EjecutarProcesos() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(Color.decode("#71C5E8"));
        this.setFocusable(true);
        this.addKeyListener(this);
        tblLoteEjecucion.getColumnModel().getColumn(0).setPreferredWidth(5);
        tblColaBloqueados.getColumnModel().getColumn(0).setPreferredWidth(5);
        colaListos = new LinkedList<>();
        colaBloqueados = new LinkedList<>();
    }

    public void inicializarPrograma(Queue<Proceso> listaProceso) {
        colaNuevos = listaProceso;
        numProcesosPendientes = colaNuevos.size();

        if (colaNuevos.size() >= MAXIMO_MEMORIA) {
            for (int i = 0; i < 3; i++) {
                colaListos.offer(colaNuevos.poll());
                numProcesosPendientes--;
            }
        } else {
            for (int i = 0; i < numProcesosPendientes; i++) {
                colaListos.offer(colaNuevos.poll());
                numProcesosPendientes--;
            }
        }
        txtProcesosPendientes.setText("" + (numProcesosPendientes));
    }

    public void actualizarColaListos(Queue<Proceso> p) {
        DefaultTableModel model = (DefaultTableModel) tblLoteEjecucion.getModel();

        // Elimina todas las filas existentes en la tabla
        int rowCount = model.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            model.removeRow(i);
        }

        // Agrega las filas con los nuevos datos del lote
        for (Proceso proceso : p) {
            model.addRow(new Object[]{proceso.obtenerID(), proceso.obtenerTiempoEstimado(), proceso.obtenerTiempoRestante()});
        }
    }

    public void eliminarProcesosEnCola() {
        DefaultTableModel model = (DefaultTableModel) tblLoteEjecucion.getModel();
        model.removeRow(0);

    }

    public void actualizarProcesosBloqueados(Queue<Proceso> p) {
        DefaultTableModel model = (DefaultTableModel) tblColaBloqueados.getModel();
        int rowCount = model.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            model.removeRow(i);
        }

        for (Proceso proceso : p) {
            model.addRow(new Object[]{proceso.obtenerID(), proceso.obtenerTiempoRestante()});
        }
    }

    public void actualizarProcesoEnEjecucion(Proceso p) {
        txtID.setText("" + p.obtenerID());
        txtOperacion.setText("" + p.obtenerOperacion());
        txtTiempoEstimado.setText("" + p.obtenerTiempoEstimado());
    }

    public void actualizarProcesosTerminados(Proceso p) {
        DefaultTableModel model = (DefaultTableModel) tblLotesTerminados.getModel();
        if (p.hayError()) {
            model.addRow(new Object[]{p.obtenerID(), p.obtenerDato1() + " " + p.obtenerOperacion() + " " + p.obtenerDato2(), "Error"});
        } else {
            model.addRow(new Object[]{p.obtenerID(), p.obtenerDato1() + " " + p.obtenerOperacion() + " " + p.obtenerDato2(), p.obtenerResultado()});
        }
    }

    public void iniciarSimulacion() {
        // Cancelar y purgar el timerAnimation si ya existe
        if (timerAnimation != null) {
            timerAnimation.purge();
            timerAnimation.cancel();
        }

        // Crear un nuevo timerAnimation
        EjecutarProcesos.timerAnimation = new Timer();

        TimerTask tareaAnimacion;

        tareaAnimacion = new TimerTask() {

            @Override
            public void run() {
                // Mientras haya procesos pendientes
                while (!colaListos.isEmpty() || numProcesosPendientes > 0) {
                    txtProcesosPendientes.setText("" + (numProcesosPendientes));
                    actualizarColaListos(colaListos);
                    if (colaBloqueados.size() < 3) {
                        Proceso p = colaListos.poll();
                        if (p.obtenerInterrumpido()) {
                            tiempoEstimado = p.obtenerTiempoRestante();
                            tiempoTranscurrido = p.obtenerTiempoEstimado() - tiempoEstimado;
                        } else {
                            tiempoEstimado = p.obtenerTiempoEstimado();
                        }
                        actualizarProcesoEnEjecucion(p);
                        eliminarProcesosEnCola();
                        tiempoRestante = tiempoEstimado;
                        while (tiempoRestante >= 0) {
                            if (!procesoPausado) {
                                actualizarProcesoEnEjecucion(p);
                                txtLotesContador.setText("" + contadorGlobal);
                                txtTiempoRestante.setText("" + tiempoRestante);
                                txtTiempoTranscurrido.setText("" + tiempoTranscurrido);

                                //Si hay error se sale del ciclo
                                if (hayError) {
                                    p.establecerError(true);
                                    hayError = false;
                                    break;
                                }
                                if (hayInterrupcion) {
                                    p.establecerTiempoRestante(tiempoRestante);
                                    p.establecerInterrumpido(true);
                                    colaBloqueados.offer(p);
                                    actualizarProcesosBloqueados(colaBloqueados);
                                    hayInterrupcion = false;
                                    break;
                                }
                                try {
                                    Thread.sleep(900);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(EjecutarProcesos.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                tiempoTranscurrido++;
                                tiempoRestante--;
                                contadorGlobal++;
                            } else {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(EjecutarProcesos.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                        if (!colaListos.contains(p) && !p.obtenerInterrumpido()) {
                            actualizarProcesosTerminados(p);
                            if (colaBloqueados.isEmpty()) {
                                colaListos.offer(colaNuevos.poll());
                                if (numProcesosPendientes > 0) {
                                    numProcesosPendientes--;
                                }
                            }
                        }
                        tiempoTranscurrido = 0;
                    } else {
                        actualizarProcesoEnEjecucion(new Proceso());
                        txtTiempoTranscurrido.setText("" + 0);
                        txtTiempoRestante.setText("" + 0);
                    }
                }
                actualizarProcesoEnEjecucion(new Proceso());
                txtTiempoTranscurrido.setText("" + 0);
                txtTiempoRestante.setText("" + 0);
                timerAnimation.cancel();
                System.out.println("Finalizado");
            }
        };

        // Programar la tarea de animación para ejecutarse cada 1000 ms (1 segundo)
        timerAnimation.scheduleAtFixedRate(tareaAnimacion, 0, 1000);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblLotesPendientes = new javax.swing.JLabel();
        txtProcesosPendientes = new javax.swing.JTextField();
        panelColaListos = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        tblLoteEjecucion = new javax.swing.JTable();
        panelProcesosTerminados = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblLotesTerminados = new javax.swing.JTable();
        lblContadorGlobal = new javax.swing.JLabel();
        txtLotesContador = new javax.swing.JTextField();
        panelProcesoEjecucion = new javax.swing.JPanel();
        lblID = new javax.swing.JLabel();
        lblOperacion = new javax.swing.JLabel();
        lblTiempoEstimado = new javax.swing.JLabel();
        lblTiempoTranscurrido = new javax.swing.JLabel();
        lblTiempoRestante = new javax.swing.JLabel();
        txtOperacion = new javax.swing.JTextField();
        txtTiempoEstimado = new javax.swing.JTextField();
        txtTiempoTranscurrido = new javax.swing.JTextField();
        txtTiempoRestante = new javax.swing.JTextField();
        txtID = new javax.swing.JTextField();
        btnComenzar = new javax.swing.JButton();
        panelColaBloqueados = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblColaBloqueados = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        lblLotesPendientes.setFont(new java.awt.Font("Segoe UI", 3, 18)); // NOI18N
        lblLotesPendientes.setText("Procesos en estado nuevo");

        txtProcesosPendientes.setEditable(false);
        txtProcesosPendientes.setBackground(new java.awt.Color(255, 255, 255));
        txtProcesosPendientes.setFont(new java.awt.Font("Monospaced", 2, 18)); // NOI18N
        txtProcesosPendientes.setText("0");

        panelColaListos.setBackground(new java.awt.Color(255, 255, 255));
        panelColaListos.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2), "Cola de listos", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 3, 14))); // NOI18N

        tblLoteEjecucion.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        tblLoteEjecucion.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tiempo estimado", "Tiempo Restante"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblLoteEjecucion.setToolTipText("");
        tblLoteEjecucion.setGridColor(new java.awt.Color(255, 255, 255));
        tblLoteEjecucion.setRowHeight(30);
        tblLoteEjecucion.setRowSelectionAllowed(false);
        jScrollPane10.setViewportView(tblLoteEjecucion);

        javax.swing.GroupLayout panelColaListosLayout = new javax.swing.GroupLayout(panelColaListos);
        panelColaListos.setLayout(panelColaListosLayout);
        panelColaListosLayout.setHorizontalGroup(
            panelColaListosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
        );
        panelColaListosLayout.setVerticalGroup(
            panelColaListosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
        );

        panelProcesosTerminados.setBackground(new java.awt.Color(255, 255, 255));
        panelProcesosTerminados.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2), "Procesos Terminados", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 3, 14))); // NOI18N

        tblLotesTerminados.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        tblLotesTerminados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Operacion", "Resultado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblLotesTerminados.setGridColor(new java.awt.Color(255, 255, 255));
        tblLotesTerminados.setRowHeight(25);
        tblLotesTerminados.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(tblLotesTerminados);

        javax.swing.GroupLayout panelProcesosTerminadosLayout = new javax.swing.GroupLayout(panelProcesosTerminados);
        panelProcesosTerminados.setLayout(panelProcesosTerminadosLayout);
        panelProcesosTerminadosLayout.setHorizontalGroup(
            panelProcesosTerminadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
        );
        panelProcesosTerminadosLayout.setVerticalGroup(
            panelProcesosTerminadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );

        lblContadorGlobal.setFont(new java.awt.Font("Segoe UI", 3, 18)); // NOI18N
        lblContadorGlobal.setText("Contador global:");

        txtLotesContador.setEditable(false);
        txtLotesContador.setBackground(new java.awt.Color(255, 255, 255));
        txtLotesContador.setFont(new java.awt.Font("Monospaced", 2, 18)); // NOI18N
        txtLotesContador.setText("0");

        panelProcesoEjecucion.setBackground(new java.awt.Color(255, 255, 255));
        panelProcesoEjecucion.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2), "Proceso en ejecución", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 3, 14))); // NOI18N

        lblID.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        lblID.setText("ID:");

        lblOperacion.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        lblOperacion.setText("Operacion:");

        lblTiempoEstimado.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        lblTiempoEstimado.setText("Tiempo estimado:");

        lblTiempoTranscurrido.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        lblTiempoTranscurrido.setText("Tiempo transcurrido:");

        lblTiempoRestante.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        lblTiempoRestante.setText("Tiempo restante:");

        txtOperacion.setEditable(false);
        txtOperacion.setBackground(new java.awt.Color(255, 255, 255));
        txtOperacion.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtTiempoEstimado.setEditable(false);
        txtTiempoEstimado.setBackground(new java.awt.Color(255, 255, 255));
        txtTiempoEstimado.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtTiempoTranscurrido.setEditable(false);
        txtTiempoTranscurrido.setBackground(new java.awt.Color(255, 255, 255));
        txtTiempoTranscurrido.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtTiempoRestante.setEditable(false);
        txtTiempoRestante.setBackground(new java.awt.Color(255, 255, 255));
        txtTiempoRestante.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtID.setEditable(false);
        txtID.setBackground(new java.awt.Color(255, 255, 255));
        txtID.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        javax.swing.GroupLayout panelProcesoEjecucionLayout = new javax.swing.GroupLayout(panelProcesoEjecucion);
        panelProcesoEjecucion.setLayout(panelProcesoEjecucionLayout);
        panelProcesoEjecucionLayout.setHorizontalGroup(
            panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblID)
                    .addComponent(lblOperacion)
                    .addComponent(lblTiempoEstimado)
                    .addComponent(lblTiempoTranscurrido)
                    .addComponent(lblTiempoRestante))
                .addGap(24, 24, 24)
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtID, javax.swing.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                    .addComponent(txtOperacion)
                    .addComponent(txtTiempoEstimado)
                    .addComponent(txtTiempoTranscurrido)
                    .addComponent(txtTiempoRestante))
                .addGap(68, 68, 68))
        );
        panelProcesoEjecucionLayout.setVerticalGroup(
            panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProcesoEjecucionLayout.createSequentialGroup()
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblID)
                    .addComponent(txtID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtOperacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblOperacion))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTiempoEstimado)
                    .addComponent(txtTiempoEstimado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTiempoTranscurrido)
                    .addComponent(txtTiempoTranscurrido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(panelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTiempoRestante)
                    .addComponent(txtTiempoRestante, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(11, Short.MAX_VALUE))
        );

        btnComenzar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnComenzar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/clases/imagenes/start-button.png"))); // NOI18N
        btnComenzar.setPreferredSize(new java.awt.Dimension(32, 32));
        btnComenzar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnComenzarActionPerformed(evt);
            }
        });

        panelColaBloqueados.setBackground(new java.awt.Color(255, 255, 255));
        panelColaBloqueados.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2), "Cola de bloqueados", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 3, 14))); // NOI18N
        panelColaBloqueados.setPreferredSize(new java.awt.Dimension(217, 18));

        tblColaBloqueados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tiempo transcurrido"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tblColaBloqueados);

        javax.swing.GroupLayout panelColaBloqueadosLayout = new javax.swing.GroupLayout(panelColaBloqueados);
        panelColaBloqueados.setLayout(panelColaBloqueadosLayout);
        panelColaBloqueadosLayout.setHorizontalGroup(
            panelColaBloqueadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
        );
        panelColaBloqueadosLayout.setVerticalGroup(
            panelColaBloqueadosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelColaListos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(lblLotesPendientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtProcesosPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(43, 43, 43)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelColaBloqueados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(lblContadorGlobal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtLotesContador, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(panelProcesosTerminados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 9, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(404, 404, 404)
                .addComponent(btnComenzar, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtLotesContador, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblContadorGlobal)))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblLotesPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtProcesosPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelColaBloqueados, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelColaListos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(panelProcesosTerminados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnComenzar, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnComenzarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnComenzarActionPerformed
        iniciarSimulacion();
        this.requestFocus();
    }//GEN-LAST:event_btnComenzarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnComenzar;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblContadorGlobal;
    private javax.swing.JLabel lblID;
    private javax.swing.JLabel lblLotesPendientes;
    private javax.swing.JLabel lblOperacion;
    private javax.swing.JLabel lblTiempoEstimado;
    private javax.swing.JLabel lblTiempoRestante;
    private javax.swing.JLabel lblTiempoTranscurrido;
    private javax.swing.JPanel panelColaBloqueados;
    private javax.swing.JPanel panelColaListos;
    private javax.swing.JPanel panelProcesoEjecucion;
    private javax.swing.JPanel panelProcesosTerminados;
    private javax.swing.JTable tblColaBloqueados;
    private javax.swing.JTable tblLoteEjecucion;
    private javax.swing.JTable tblLotesTerminados;
    private javax.swing.JTextField txtID;
    private javax.swing.JTextField txtLotesContador;
    private javax.swing.JTextField txtOperacion;
    private javax.swing.JTextField txtProcesosPendientes;
    private javax.swing.JTextField txtTiempoEstimado;
    private javax.swing.JTextField txtTiempoRestante;
    private javax.swing.JTextField txtTiempoTranscurrido;
    // End of variables declaration//GEN-END:variables

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == 'c' || e.getKeyChar() == 'C') {
            procesoPausado = false;
        }

        if (e.getKeyChar() == 'e' || e.getKeyChar() == 'E') {
            hayError = true;
        }

        if (e.getKeyChar() == 'i' || e.getKeyChar() == 'I') {
            hayInterrupcion = true;
        }

        if (e.getKeyChar() == 'p' || e.getKeyChar() == 'P') {
            procesoPausado = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
