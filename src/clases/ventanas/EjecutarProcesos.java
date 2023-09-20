package clases.ventanas;

import clases.Proceso;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;

public class EjecutarProcesos extends JFrame implements KeyListener {

    List<List<Proceso>> lotes;
    private static Timer timerAnimation;
    private int contadorGlobal = 0;
    private int numLotesPendientes = 0;
    private int tiempoRestante = 0;
    private int tiempoEstimado = 0;
    private int tiempoTranscurrido = 0;
    private boolean procesoPausado = false;
    private boolean hayError = false;
    private boolean hayInterrupcion = false;

    public EjecutarProcesos() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(Color.decode("#71C5E8"));
        this.setFocusable(true);
        this.addKeyListener(this);
        tblLoteEjecucion.getColumnModel().getColumn(0).setPreferredWidth(5);
        tblColaBloqueados.getColumnModel().getColumn(0).setPreferredWidth(5);
    }

    public void inicializarPrograma(List<Proceso> listaProceso) {
        this.lotes = dividirPorLotes(listaProceso);

        numLotesPendientes = lotes.size();
        txtLotesPendientes.setText("" + numLotesPendientes);
    }

    public void actualizarLoteEnEjecucion(List<Proceso> p) {
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

    public void eliminarProcesosEnLote() {
        DefaultTableModel model = (DefaultTableModel) tblLoteEjecucion.getModel();
        model.removeRow(0);

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

    public List<List<Proceso>> dividirPorLotes(List<Proceso> listaProceso) {
        int tamanioLotes = 4;
        List<List<Proceso>> ListaLotes = new ArrayList<>();
        List<Proceso> loteActual = new ArrayList<>();

        for (Proceso proceso : listaProceso) {
            loteActual.add(proceso);
            if (loteActual.size() == tamanioLotes) {
                ListaLotes.add(loteActual);
                loteActual = new ArrayList<>();
            }
        }

        if (!loteActual.isEmpty()) {
            ListaLotes.add(loteActual);
        }
        return ListaLotes;
    }

    public void iniciarSimulacion() {
        // Cancelar y purgar el timerAnimation si ya existe
        if (timerAnimation != null) {
            timerAnimation.purge();
            timerAnimation.cancel();
        }

        // Crear un nuevo timerAnimation
        EjecutarProcesos.timerAnimation = new Timer();

        // Actualizar el número de lotes pendientes
        txtLotesPendientes.setText("" + (--numLotesPendientes));

        TimerTask tareaAnimacion;
        tareaAnimacion = new TimerTask() {

            @Override
            public void run() {
                // Mientras haya lotes en la lista
                while (!lotes.isEmpty()) {
                    List<Proceso> lista = lotes.get(0);
                    // Actualizar el lote en ejecución
                    actualizarLoteEnEjecucion(lista);

                    // Mientras haya procesos en el lote
                    while (!lista.isEmpty()) {
                        Proceso proceso = lista.get(0);
                        if (proceso.obtenerInterrumpido()) {
                            tiempoEstimado = proceso.obtenerTiempoRestante();
                            tiempoTranscurrido = proceso.obtenerTiempoEstimado() - tiempoEstimado;
                        } else {
                            tiempoEstimado = proceso.obtenerTiempoEstimado();
                        }
                        tiempoRestante = tiempoEstimado;

                        // Ejecutar el proceso mientras haya tiempo restante
                        while (tiempoRestante >= 0) {
                            if (!procesoPausado) {
                                actualizarProcesoEnEjecucion(proceso);
                                txtLotesContador.setText("" + contadorGlobal);
                                txtTiempoRestante.setText("" + tiempoRestante);
                                txtTiempoTranscurrido.setText("" + tiempoTranscurrido);

                                //Si hay error se sale del ciclo
                                if (hayError) {
                                    proceso.establecerError(true);
                                    hayError = false;
                                    break;
                                }
                                //si hay interrupcion se sale del ciclo
                                if (hayInterrupcion) {
                                    proceso.establecerTiempoRestante(tiempoRestante + 1);
                                    proceso.establecerInterrumpido(true);
                                    lista.add(proceso);
                                    actualizarLoteEnEjecucion(lista);
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

                        // Eliminar el proceso de la lista y actualizar procesos terminados
                        lista.remove(proceso);
                        if (!lista.contains(proceso)) {
                            actualizarProcesosTerminados(proceso);
                        }
                        eliminarProcesosEnLote();
                        tiempoTranscurrido = 0;
                    }

                    // Actualizar el número de lotes pendientes y eliminar el lote procesado
                    if (numLotesPendientes > 0) {
                        numLotesPendientes--;
                    }
                    lotes.remove(lista);
                    txtLotesPendientes.setText("" + numLotesPendientes);
                }

                // Finalizar la simulación y limpiar la interfaz
                actualizarProcesoEnEjecucion(new Proceso());
                actualizarLoteEnEjecucion(new ArrayList<>());
                txtLotesContador.setText("" + contadorGlobal);
                txtTiempoTranscurrido.setText("" + 0);
                txtTiempoRestante.setText("" + 0);
                EjecutarProcesos.timerAnimation.cancel();

            }
        };

        // Programar la tarea de animación para ejecutarse cada 1000 ms (1 segundo)
        timerAnimation.scheduleAtFixedRate(tareaAnimacion, 0, 1000);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblLotesPendientes = new javax.swing.JLabel();
        txtLotesPendientes = new javax.swing.JTextField();
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

        txtLotesPendientes.setEditable(false);
        txtLotesPendientes.setBackground(new java.awt.Color(255, 255, 255));
        txtLotesPendientes.setFont(new java.awt.Font("Monospaced", 2, 18)); // NOI18N
        txtLotesPendientes.setText("0");

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
                        .addComponent(txtLotesPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                        .addComponent(txtLotesPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
    private javax.swing.JTextField txtLotesPendientes;
    private javax.swing.JTextField txtOperacion;
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
