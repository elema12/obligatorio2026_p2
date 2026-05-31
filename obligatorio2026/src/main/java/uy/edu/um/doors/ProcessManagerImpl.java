package uy.edu.um.doors;

import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
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
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }
}
