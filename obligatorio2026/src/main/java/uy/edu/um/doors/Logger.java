package uy.edu.um.doors;

import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter formatoTimestamp =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter formatoFechaArchivo =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String nombreArchivo;

    public Logger() {
        // el nombre del archivo lo generamos con la fecha de hoy
        this.nombreArchivo = "DOORS_PROCESS_LOG_" + LocalDate.now().format(formatoFechaArchivo) + ".txt";
    }

    // cuando el proceso pasa de NEW a PENDING
    public void logNewToPending(Process proceso) {
        String linea = obtenerTimestamp() + ": NEW PENDING PROCESS: "
                + "PID=" + proceso.getPid()
                + " | " + proceso.getName()
                + " | USER:" + proceso.getOwner().getAlias()
                + " UID:" + proceso.getOwner().getUid()
                + " | P=" + proceso.getPriority();
        escribirLinea(linea);
    }

    // cuando empieza a ejecutarse
    public void logExecuting(Process proceso) {
        String lineaPrincipal = obtenerTimestamp()
                + ": EXECUTING PROCESS: "
                + "PID=" + proceso.getPid()
                + " | USER:" + proceso.getOwner().getAlias()
                + " UID:" + proceso.getOwner().getUid();

        StringBuilder contenidoCompleto = new StringBuilder();
        contenidoCompleto.append(lineaPrincipal);

        // agregamos cada evento del proceso en una línea aparte
        MyList<Event> listaEventos = proceso.getEvents();
        for (int i = 0; i < listaEventos.size(); i++) {
            Event eventoActual = listaEventos.get(i);
            contenidoCompleto.append(System.lineSeparator());
            contenidoCompleto.append("  EVENT: ").append(eventoActual.getType());
            contenidoCompleto.append(" | Instructions ");
            contenidoCompleto.append(formatearInstrucciones(eventoActual.getInstructions()));
        }

        escribirLinea(contenidoCompleto.toString());
    }

    // cuando termina un proceso, ya sea normal o terminado a la fuerza
    public void logEnding(Process proceso) {
        String lineaBase = obtenerTimestamp()
                + ": ENDING PROCESS: PID=" + proceso.getPid()
                + " | STATE: " + proceso.getFinishType();

        // si fue terminado por un usuario lo agregamos
        if (proceso.getFinishType() == FinishType.TERMINATED && proceso.getTerminatedBy() != null) {
            lineaBase = lineaBase + " by USER:" + proceso.getTerminatedBy().getAlias()
                    + " UID:" + proceso.getTerminatedBy().getUid();
        }

        escribirLinea(lineaBase);
    }

    // cuando la pila de finalizados se llenó, hay que vaciarla y loguear todo
    public void logStackOverflow(MyStack<Process> pilaFinalizados) throws EmptyStackException {
        StringBuilder contenido = new StringBuilder();
        contenido.append(obtenerTimestamp()).append(": Finished process stack overflow");

        while (!pilaFinalizados.isEmpty()) {
            Process procesoActual = pilaFinalizados.pop();
            contenido.append(System.lineSeparator());
            contenido.append("  PID=").append(procesoActual.getPid());
            contenido.append(" ").append(procesoActual.getName());
            contenido.append(" | STATE: ").append(procesoActual.getFinishType());
            contenido.append(" | USER:").append(procesoActual.getOwner().getAlias());
            contenido.append(" UID:").append(procesoActual.getOwner().getUid());
        }

        escribirLinea(contenido.toString());
    }

    // devuelve el timestamp formateado entre corchetes
    private String obtenerTimestamp() {
        return "[" + LocalDateTime.now().format(formatoTimestamp) + "]";
    }

    // convierte la lista de instrucciones a un string legible
    private String formatearInstrucciones(MyList<String> instrucciones) {
        String resultado = "[";
        for (int i = 0; i < instrucciones.size(); i++) {
            if (i > 0) {
                resultado = resultado + ", ";
            }
            resultado = resultado + instrucciones.get(i);
        }
        resultado = resultado + "]";
        return resultado;
    }

    // escribe en el archivo, en modo append para no pisar lo anterior
    private void escribirLinea(String lineaAEscribir) {
        FileWriter escritor = null;
        try {
            escritor = new FileWriter(nombreArchivo, true);
            escritor.write(lineaAEscribir);
            escritor.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("ERROR escribiendo log: " + e.getMessage());
        } finally {
            if (escritor != null) {
                try {
                    escritor.close();
                } catch (IOException e) {
                    System.err.println("ERROR cerrando el archivo: " + e.getMessage());
                }
            }
        }
    }
}