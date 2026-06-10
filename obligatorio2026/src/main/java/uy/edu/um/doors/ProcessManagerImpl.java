package uy.edu.um.doors;

import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.queue.EmptyQueueException;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import uy.edu.um.tad.list.MyLinkedListImpl;


public class ProcessManagerImpl implements ProcessManager{

    // cola de procesos nuevos, esperan que prepareProcesses() les calcule la prioridad
    private final MyQueue<Process> newProcesses;

    // heap de pendientes ordenado por prioridad
    // usamos isHeapMin=true porque compareTo() está invertido: menor valor = mayor prioridad
    private final MyHeap<Process> pendingProcesses;

    // proceso que está corriendo ahora, null si no hay ninguno (doors es monotarea)
    private Process runningProcess;

    // pila de procesos que ya terminaron
    private final MyStack<Process> finishedProcesses;

    // tabla hash de usuarios, la indexamos por uid para buscar en O(1)
    private final MyHash<Integer, User> users;

    // el logger
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
            String[] instructionParts = instructionsRaw.split(",");        // separa por ,
            for (String instr : instructionParts) {
                instructions.add(instr.trim());                            // agrega cada instruccion a la lista
            }

            events.add(new Event(type, instructions));
        }

        return events;
    }

    public void printCargados() {
        System.out.println("Usuarios cargados: " + users.size());
        System.out.println("Procesos en cola NEW: " + newProcesses.size());
    }

    @Override
    public void prepareProcesses() {
        while ( !newProcesses.isEmpty() ) {
            Process p = null;
            try {
                p = newProcesses.dequeue();
            } catch (EmptyQueueException e) {
                throw new RuntimeException(e);
            }
            p.setPriority(calculatePriority(p));      // calcular y set prioridad
            p.setState(ProcessState.PENDING);         // cambiar de estado
            pendingProcesses.insert(p);               // entra al heap
            logger.logNewToPending(p);                // lo registra en log
        }

    }

    private int calculatePriority(Process p) {
        MyList<Event> events = p.getEvents();
        int nTotal = events.size();

        if (nTotal == 0) return 0;  // proceso sin eventos: prioridad mínima

        int nCpu = 0;
        int nRam = 0;
        int nDisk = 0;

        for (int i = 0; i < nTotal; i++) {
            switch (events.get(i).getType()) {
                case CPU  -> nCpu++;
                case RAM  -> nRam++;
                case DISK -> nDisk++;
            }
        }

        int weight = (p.getOwner().getType() == UserType.ADMIN) ? 32 : 16;

        int priority = (8 * nCpu + 2 * nRam + 2 * nDisk) / nTotal
                + weight * nTotal;

        return priority;
    }

    @Override
    public void executeNextProcess() {
        if (runningProcess != null) {
            System.out.println("Ya hay un proceso en ejecución (PID=" + runningProcess.getPid() + "). Doors es monotarea.");
            return;
        }
        if (pendingProcesses.isEmpty()) {
            System.out.println("No hay procesos pendientes para ejecutar.");
            return;
        }
        try {
            Process p = pendingProcesses.remove();
            p.setState(ProcessState.RUNNING);
            runningProcess = p;
            logger.logExecuting(p);
        } catch (EmptyHeapException e) {
            throw new RuntimeException(e);
        }
    }

    private void finishCurrent(FinishType type, User by) {
        if (runningProcess == null) {
            System.out.println("No hay proceso en ejecución para finalizar.");
            return;
        }
        runningProcess.setFinishType(type);
        if (by != null) {
            runningProcess.setTerminatedBy(by);
        }
        runningProcess.setState(ProcessState.FINISHED);
        logger.logEnding(runningProcess);

        if (finishedProcesses.size() == MAX_FINISHED_PROCESS_ON_RAM) {
            try {
                logger.logStackOverflow(finishedProcesses);
            } catch (EmptyStackException e) {
                // No debería ocurrir: size == MAX > 0 garantiza pila no vacía
            }
        }
        finishedProcesses.push(runningProcess);
        runningProcess = null;
    }

    @Override
    public void finishProcessOk() {
        finishCurrent(FinishType.OK, null);
    }

    @Override
    public void finishProcessError() {
        finishCurrent(FinishType.ERROR, null);
    }

    @Override
    public void terminateProcess(int uid) {
        if (!users.contains(uid)) {
            System.out.println("UID " + uid + " no existe en el sistema.");
            return;
        }
        User u = users.get(uid);
        finishCurrent(FinishType.TERMINATED, u);
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
        // Como queremos mostrar el más reciente primero (orden inverso al de finalización),
        // imprimimos cuando sacamos de finishedProcesses (el top es el último que finalizó).
        // Vamos guardando en tempStack para después restaurar el orden original.

        System.out.println("FINISHED:");

        MyStack<Process> tempStack = new MyStackImpl<>();

        while (!finishedProcesses.isEmpty()) {
            try {
                Process p = finishedProcesses.pop();
                tempStack.push(p); // lo guardamos para restaurar
                // Siempre mostramos el dueño del proceso (no quién lo terminó).
                // El "by USER" aparece en el log de ENDING PROCESS, no en pstatus.
                String line = "\tPID=" + p.getPid() + " " + p.getName() +
                        " | STATE: " + p.getFinishType() +
                        " | USER:" + p.getOwner().getAlias() +
                        " UID:" + p.getOwner().getUid();
                System.out.println(line);
            } catch (EmptyStackException e) {}
        }

        // Restauramos finishedProcesses al orden original
        while (!tempStack.isEmpty()) {
            try { finishedProcesses.push(tempStack.pop()); } catch (EmptyStackException e) {}
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

        // FINISHED: mismo truco, pero imprimiendo al sacar de finishedProcesses
        // para que el más reciente aparezca primero.
        System.out.println("FINISHED:");
        MyStack<Process> tempStack = new MyStackImpl<>();
        while (!finishedProcesses.isEmpty()) {
            try {
                Process p = finishedProcesses.pop();
                tempStack.push(p); // guardamos para restaurar
                // Los procesos finalizados se muestran con STATE, no con prioridad.
                System.out.println("\tPID=" + p.getPid() + " " + p.getName() +
                        " | STATE: " + p.getFinishType() +
                        " | USER:" + p.getOwner().getAlias() +
                        " UID:" + p.getOwner().getUid());
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
        // Restauramos finishedProcesses
        while (!tempStack.isEmpty()) {
            try { finishedProcesses.push(tempStack.pop()); } catch (EmptyStackException e) {}
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
        // Imprimimos al sacar de finishedProcesses para mostrar el más reciente primero
        while (!finishedProcesses.isEmpty()) {
            try {
                Process p = finishedProcesses.pop();
                tempStack.push(p); // guardamos para restaurar
                // Solo imprimimos si el proceso pertenece al usuario buscado
                if (p.getOwner().getUid() == uid) {
                    System.out.println("\tPID=" + p.getPid() + " " + p.getName() +
                            " | STATE: " + p.getFinishType() +
                            " | USER:" + p.getOwner().getAlias() +
                            " UID:" + p.getOwner().getUid());
                }
            } catch (EmptyStackException e) {}
        }
        // Restauramos finishedProcesses
        while (!tempStack.isEmpty()) {
            try { finishedProcesses.push(tempStack.pop()); } catch (EmptyStackException e) {}
        }
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("PROCESS STATUS - PID:" + pid);
        // Buscamos en el proceso en ejecución
        if (runningProcess != null && runningProcess.getPid() == pid) {
            System.out.println("EXECUTING:");
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
                System.out.println("PENDING:");
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
                    System.out.println("FINISHED:");
                    System.out.println("\tPID=" + p.getPid() + " " + p.getName() +
                            " | STATE: " + p.getFinishType() +
                            " | USER:" + p.getOwner().getAlias() +
                            " UID:" + p.getOwner().getUid());
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