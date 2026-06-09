package uy.edu.um.doors;

import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


// Escribe los eventos del sistema operativo en un archivo.
// cada llamada agrega al final sin sobrescribir lo anterior.

public class Logger {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String fileName;

    public Logger() {
        // El archivo se nombra con la fecha del día en que arranca el sistema
        this.fileName = "DOORS_PROCESS_LOG_" + LocalDate.now().format(FILE_DATE_FORMAT) + ".txt";
    }

    // Log cuando un proceso pasa de NEW a PENDING (durante pprepare).
    // NEW PENDING PROCESS: PID=123 | notepad.exe | USER:admin UID:525 | P=8541

    public void logNewToPending(Process p) {
        String line = timestamp() + ": NEW PENDING PROCESS: "
                + "PID=" + p.getPid()
                + " | " + p.getName()
                + " | USER:" + p.getOwner().getAlias()
                + " UID:" + p.getOwner().getUid()
                + " | P=" + p.getPriority();
        writeLine(line);
    }

    // cuando un proceso comienza a ejecutarse (durante pexecute).

    public void logExecuting(Process p) {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp())
                .append(": EXECUTING PROCESS: ")
                .append("PID=").append(p.getPid())
                .append(" | USER:").append(p.getOwner().getAlias())
                .append(" UID:").append(p.getOwner().getUid());

        // Anexar cada evento del proceso en una línea aparte.
        MyList<Event> events = p.getEvents();
        for (int i = 0; i < events.size(); i++) {
            Event ev = events.get(i);
            sb.append(System.lineSeparator())
                    .append("  EVENT: ").append(ev.getType())
                    .append(" | Instructions ")
                    .append(formatInstructions(ev.getInstructions()));
        }

        writeLine(sb.toString());
    }


    // cuando un proceso finaliza (durante pfinish).
    // Si fue terminado por un usuario, se incluye su alias y uid.

    public void logEnding(Process p) {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp())
                .append(": ENDING PROCESS: PID=").append(p.getPid())
                .append(" | STATE: ").append(p.getFinishType());

        if (p.getFinishType() == FinishType.TERMINATED && p.getTerminatedBy() != null) {
            sb.append(" by USER:").append(p.getTerminatedBy().getAlias())
                    .append(" UID:").append(p.getTerminatedBy().getUid());
        }

        writeLine(sb.toString());
    }

    // cuando la pila de procesos finalizados se desborda.
    // Vuelca todos los procesos en orden inverso al de finalización

    public void logStackOverflow(MyStack<Process> stack) throws EmptyStackException {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp()).append(": Finished process stack overflow");

        while (!stack.isEmpty()) {
            Process p = stack.pop();
            sb.append(System.lineSeparator())
                    .append("  PID=").append(p.getPid())
                    .append(" ").append(p.getName())
                    .append(" | STATE: ").append(p.getFinishType())
                    .append(" | USER:").append(p.getOwner().getAlias())
                    .append(" UID:").append(p.getOwner().getUid());
        }

        writeLine(sb.toString());
    }

    // Devuelve el timestamp actual entre corchetes
    private String timestamp() {
        return "[" + LocalDateTime.now().format(TS_FORMAT) + "]";
    }


    // Formatea una lista de instrucciones .

    private String formatInstructions(MyList<String> instr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < instr.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(instr.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    // Escribe una línea al archivo en modo append.

    private void writeLine(String line) {
        try (FileWriter fw = new FileWriter(fileName, true)) {
            fw.write(line);
            fw.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("ERROR escribiendo log: " + e.getMessage());
        }
    }
}