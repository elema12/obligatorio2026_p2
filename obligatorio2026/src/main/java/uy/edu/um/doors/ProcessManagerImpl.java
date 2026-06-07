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

        // PASO 1: cargar usuarios
        try (BufferedReader br = new BufferedReader(new FileReader(usersCsvPath))) {  // abre el archivo de usuarios
            br.readLine();                                                            // saltea uid;alias;type
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");                                      // parte por cada ;
                int uid = Integer.parseInt(parts[0].trim());                           // "25" → 25
                String alias = parts[1].trim();
                UserType type = UserType.valueOf(parts[2].trim());                     // "ADMIN" → UserType.ADMIN
                User user = new User(uid, alias, type);
                users.put(uid, user);                                                  // lo guarda en el hash
            }
        } catch (IOException e) {
            System.out.println("Error leyendo usuarios: " + e.getMessage());
            return;
        }

        // PASO 2: cargar procesos
        try (BufferedReader br = new BufferedReader(new FileReader(processCsvPath))) {  // abre el archivo de procesos
            br.readLine();                                                              // saltea pid;uid;name;events
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";", 4);                                    // parte en maximo 4 ( si el evento tiene algun ; queda junto igual )
                int pid = Integer.parseInt(parts[0].trim());
                int uid = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                String eventsRaw = parts[3].trim();

                User owner = users.get(uid);                                            // busca el usuario en el hash
                if (owner == null) {
                    System.out.println("Usuario no encontrado: " + uid);
                    continue;                                                           // saltea este proceso
                }

                MyList<Event> events = readEvents(eventsRaw);
                Process process = new Process(pid, name, owner, events);                // crea el proceso
                newProcesses.enqueue(process);                                          // lo mete en la cola NEW
            }
        } catch (IOException e) {
            System.out.println("Error leyendo procesos: " + e.getMessage());
        }
    }
    private MyList<Event> readEvents(String eventsRaw) {
        MyList<Event> events = new MyLinkedListImpl<>();                    // lista donde vamos a guardar los eventos

        String content = eventsRaw.replace("{", "").replace("}", "");      // saca las llaves { y }

        String[] eventParts = content.split("#");                          // separa por #

        for (String eventPart : eventParts) {
            eventPart = eventPart.trim();                                  // saca espacios

            String[] typeAndInstructions = eventPart.split(":\\[");        // separa por ":[" (uso \\ para leer "[" como un simbolo)

            String typeName = typeAndInstructions[0].trim();
            EventType type = EventType.valueOf(typeName);

            String instructionsRaw = typeAndInstructions[1].replace("]", "");

            MyList<String> instructions = new MyLinkedListImpl<>();
            String[] instructionParts = instructionsRaw.split(",");               // separa por ,
            for (String instr : instructionParts) {
                instructions.add(instr.trim());                            // agrega cada instruccion a la lista
            }

            events.add(new Event(type, instructions));
        }

        return events;
    }
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
