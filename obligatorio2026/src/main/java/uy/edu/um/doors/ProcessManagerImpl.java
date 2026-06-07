package uy.edu.um.doors;

import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;


public class ProcessManagerImpl implements ProcessManager{

    //EL DISEÑO DE LA ESTRUCTURA DE ALMACENAMIENTO DEBE IMPLEMENTARSE EN ESTA CLASE EN RELACIÓN CON LAS ENTIDADES QUE DEFINA

    /** Cola FIFO de procesos en estado NEW. Esperan que prepareProcesses() les calcule prioridad. */
    private final MyQueue<Process> newProcesses;

    /**
     * Heap de procesos en estado PENDING, ordenados por prioridad.
     * Se usa isHeapMin=true porque Process.compareTo() está invertido:
     * devuelve menor para mayor prioridad, así que el "mínimo" del heap
     * es en realidad el de mayor prioridad.
     */
    private final MyHeap<Process> pendingProcesses;

    /** Proceso actualmente en ejecución. Doors es monotarea: hay como máximo uno. null si no hay ninguno. */
    private Process runningProcess;

    /** Pila LIFO de procesos finalizados, con capacidad MAX_FINISHED_PROCESS_ON_RAM. */
    private final MyStack<Process> finishedProcesses;

    /** Tabla hash de usuarios indexada por UID, para búsqueda O(1) promedio. */
    private final MyHash<Integer, User> users;

    /** Escritor del log del sistema. */
    private final Logger logger;

    public ProcessManagerImpl() {
        this.newProcesses = new MyQueueImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(true, new Process[1000]);
        this.runningProcess = null;
        this.finishedProcesses = new MyStackImpl<>();
        this.users = new MyHashImpl<>();
        this.logger = new Logger();
    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void prepareProcesses() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void executeNextProcess() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessOk() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessError() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void terminateProcess(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");

        // Mostramos el proceso actualmente en ejecución (solo hay uno porque Doors es monotarea)
        System.out.println("EXECUTING:");
        if (runningProcess != null) {
            System.out.println("\tPID=" + runningProcess.getPid() + " | " + runningProcess.getName() +
                    " | USER:" + runningProcess.getOwner().getAlias() +
                    " UID:" + runningProcess.getOwner().getUid() +
                    " | P=" + runningProcess.getPriority());
        }

        // PENDING: el heap no tiene iterador, así que lo vaciamos elemento a elemento
        // Cada elemento sacado se guarda en un heap temporal para poder restaurarlo después.
        System.out.println("PENDING:");
        MyHeap<Process> temp = new MyHeapImpl<>(true, new Process[1000]);
        // Creamos el heap para guardar los procesos mientras recorremos el heap original. Se llama temp.
        while (!pendingProcesses.isEmpty()) {
            Process p = pendingProcesses.remove(); // saca el de mayor prioridad.
            temp.insert(p); // lo guardamos para restaurar
            System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                    " | USER:" + p.getOwner().getAlias() +
                    " UID:" + p.getOwner().getUid() +
                    " | P=" + p.getPriority());
        }

        // Restauramos el heap original
        while (!temp.isEmpty()) {
            pendingProcesses.insert(temp.remove());
        }
        // Sacamos para poner de nuevo

        // FINISHED: la pila tampoco tiene iterador.
        // La volcamos en una pila temporal (invirtiendo el orden)
        // imprimimos, y restauramos el orden original

        System.out.println("FINISHED:");

        MyStack<Process> tempStack = new MyStackImpl<>();

        while (!finishedProcesses.isEmpty()) {
            try { tempStack.push(finishedProcesses.pop()); } catch (EmptyStackException e) {}
        }
        // pop: saca el de arriba. pop: pone el de arriba.
        // es decir, saca el de arriba de finished y meta arriba en tempStack.
        // saca todos los de finishedProcesses y los pone en el stack (orden inverso)

        while (!tempStack.isEmpty()) {
            try {
                Process p = tempStack.pop();
                finishedProcesses.push(p); // restauramos
                String line = "\tPID=" + p.getPid() + " " + p.getName() + " | STATE: " + p.getFinishType();
                // Si fue terminado forzadamente mostramos quién lo terminó
                if (p.getFinishType() == FinishType.TERMINATED && p.getTerminatedBy() != null) {
                    line += " | USER:" + p.getTerminatedBy().getAlias() + " UID:" + p.getTerminatedBy().getUid();
                } else {
                    line += " | USER:" + p.getOwner().getAlias() + " UID:" + p.getOwner().getUid();
                }
                System.out.println(line);
            } catch (EmptyStackException e) {}
            //Si falla, seguir.
        }
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("PROCESS STATUS (VERBOSE)");

        System.out.println("EXECUTING:");

        if (runningProcess != null) {
            System.out.println("\tPID=" + runningProcess.getPid() + " | " + runningProcess.getName() +
                    " | USER:" + runningProcess.getOwner().getAlias() +
                    " UID:" + runningProcess.getOwner().getUid() +
                    " | P=" + runningProcess.getPriority());

            // Iteramos los eventos del proceso en ejecución
            for (int i = 0; i < runningProcess.getEvents().size(); i++) {
                // Recorre todos los eventos del proceso.
                Event ev = runningProcess.getEvents().get(i);
                System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                for (int j = 0; j < ev.getInstructions().size(); j++) {
                    System.out.print(ev.getInstructions().get(j));
                    if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                }
                System.out.println("]");
            }
        }

        // PENDING: mismo truco del heap temporal para no destruir la estructura

        System.out.println("PENDING:");
        MyHeap<Process> temp = new MyHeapImpl<>(true, new Process[1000]);
        while (!pendingProcesses.isEmpty()) {
            Process p = pendingProcesses.remove();
            temp.insert(p); // guardamos para restaurar
            System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                    " | USER:" + p.getOwner().getAlias() +
                    " UID:" + p.getOwner().getUid() +
                    " | P=" + p.getPriority());
            // Detalle de cada evento del proceso pendiente
            for (int i = 0; i < p.getEvents().size(); i++) {
                Event ev = p.getEvents().get(i);
                System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                for (int j = 0; j < ev.getInstructions().size(); j++) {
                    System.out.print(ev.getInstructions().get(j));
                    if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                }
                System.out.println("]");
            }
        }
        // Restauramos el heap.
        while (!temp.isEmpty()) {
            pendingProcesses.insert(temp.remove());
        }

        // FINISHED: mismo truco de la pila temporal
        System.out.println("FINISHED:");
        MyStack<Process> tempStack = new MyStackImpl<>();
        while (!finishedProcesses.isEmpty()) {
            try { tempStack.push(finishedProcesses.pop()); } catch (EmptyStackException e) {}
        }
        while (!tempStack.isEmpty()) {
            try {
                Process p = tempStack.pop();
                finishedProcesses.push(p); // restauramos
                System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                        " | USER:" + p.getOwner().getAlias() +
                        " UID:" + p.getOwner().getUid() +
                        " | P=" + p.getPriority());
                // Detalle de cada evento del proceso finalizado
                for (int i = 0; i < p.getEvents().size(); i++) {
                    Event ev = p.getEvents().get(i);
                    System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                    for (int j = 0; j < ev.getInstructions().size(); j++) {
                        System.out.print(ev.getInstructions().get(j));
                        if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                    }
                    System.out.println("]");
                }
            } catch (EmptyStackException e) {}
        }
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("PROCESS STATUS - USER UID:" + uid);

        // Solo mostramos el proceso en ejecución si pertenece al usuario buscado
        System.out.println("EXECUTING:");
        if (runningProcess != null && runningProcess.getOwner().getUid() == uid) {
            System.out.println("\tPID=" + runningProcess.getPid() + " | " + runningProcess.getName() +
                    " | USER:" + runningProcess.getOwner().getAlias() +
                    " UID:" + runningProcess.getOwner().getUid() +
                    " | P=" + runningProcess.getPriority());
        }

        // PENDING: vaciamos el heap, filtramos por uid, y restauramos
        System.out.println("PENDING:");
        MyHeap<Process> temp = new MyHeapImpl<>(true, new Process[1000]);
        while (!pendingProcesses.isEmpty()) {
            Process p = pendingProcesses.remove();
            temp.insert(p); // guardamos para restaurar
            // Solo imprimimos si el proceso pertenece al usuario buscado
            if (p.getOwner().getUid() == uid) {
                System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                        " | USER:" + p.getOwner().getAlias() +
                        " UID:" + p.getOwner().getUid() +
                        " | P=" + p.getPriority());
            }
        }

        while (!temp.isEmpty()) {
            pendingProcesses.insert(temp.remove());
        }

        // FINISHED: filtramos por uid
        System.out.println("FINISHED:");
        MyStack<Process> tempStack = new MyStackImpl<>();
        while (!finishedProcesses.isEmpty()) {
            try { tempStack.push(finishedProcesses.pop()); } catch (EmptyStackException e) {}
        }
        while (!tempStack.isEmpty()) {
            try {
                Process p = tempStack.pop();
                finishedProcesses.push(p);
                // Solo imprimimos si el proceso pertenece al usuario buscado
                if (p.getOwner().getUid() == uid) {
                    System.out.println("\tPID=" + p.getPid() + " " + p.getName() +
                            " | STATE: " + p.getFinishType() +
                            " | USER:" + p.getOwner().getAlias() +
                            " UID:" + p.getOwner().getUid());
                }
            } catch (EmptyStackException e) {}
        }
    }

    @Override
    public void printStatusByProcess(int pid) {
        // Buscamos en el proceso en ejecución
        if (runningProcess != null && runningProcess.getPid() == pid) {
            System.out.println("\tPID=" + runningProcess.getPid() + " | " + runningProcess.getName() +
                    " | USER:" + runningProcess.getOwner().getAlias() +
                    " UID:" + runningProcess.getOwner().getUid() +
                    " | P=" + runningProcess.getPriority());
            for (int i = 0; i < runningProcess.getEvents().size(); i++) {
                Event ev = runningProcess.getEvents().get(i);
                System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                for (int j = 0; j < ev.getInstructions().size(); j++) {
                    System.out.print(ev.getInstructions().get(j));
                    if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                }
                System.out.println("]");
            }
            return;
        }

        // Buscamos en pendientes
        MyHeap<Process> temp = new MyHeapImpl<>(true, new Process[1000]);
        boolean found = false;
        while (!pendingProcesses.isEmpty()) {
            Process p = pendingProcesses.remove();
            temp.insert(p);
            if (p.getPid() == pid && !found) {
                found = true;
                System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                        " | USER:" + p.getOwner().getAlias() +
                        " UID:" + p.getOwner().getUid() +
                        " | P=" + p.getPriority());
                for (int i = 0; i < p.getEvents().size(); i++) {
                    Event ev = p.getEvents().get(i);
                    System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                    for (int j = 0; j < ev.getInstructions().size(); j++) {
                        System.out.print(ev.getInstructions().get(j));
                        if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                    }
                    System.out.println("]");
                }
            }
        }
        while (!temp.isEmpty()) {
            pendingProcesses.insert(temp.remove());
        }
        if (found) return;

        // Buscamos en finalizados
        MyStack<Process> tempStack = new MyStackImpl<>();
        while (!finishedProcesses.isEmpty()) {
            try { tempStack.push(finishedProcesses.pop()); } catch (EmptyStackException e) {}
        }
        while (!tempStack.isEmpty()) {
            try {
                Process p = tempStack.pop();
                finishedProcesses.push(p);
                if (p.getPid() == pid && !found) {
                    found = true;
                    System.out.println("\tPID=" + p.getPid() + " | " + p.getName() +
                            " | USER:" + p.getOwner().getAlias() +
                            " UID:" + p.getOwner().getUid() +
                            " | P=" + p.getPriority());
                    for (int i = 0; i < p.getEvents().size(); i++) {
                        Event ev = p.getEvents().get(i);
                        System.out.print("\t  EVENT: " + ev.getType() + " | Instructions [");
                        for (int j = 0; j < ev.getInstructions().size(); j++) {
                            System.out.print(ev.getInstructions().get(j));
                            if (j < ev.getInstructions().size() - 1) System.out.print(", ");
                        }
                        System.out.println("]");
                    }
                }
            } catch (EmptyStackException e) {}
        }

        if (!found) {
            System.out.println("Proceso con PID=" + pid + " no encontrado.");
        }
    }
}
