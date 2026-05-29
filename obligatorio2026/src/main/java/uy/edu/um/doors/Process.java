package uy.edu.um.doors;

import uy.edu.um.tad.list.MyList;

public class Process implements Comparable<Process> {
    private final int pid;
    private final String name;
    private final User owner;
    private final MyList<Event> events;
    private int priority;
    private ProcessState state;
    private FinishType finishType;
    private User terminatedBy;

    public Process(int pid, String name, User owner, MyList<Event> events) {
        this.pid = pid;
        this.name = name;
        this.owner = owner;
        this.events = events;
        this.state = ProcessState.NEW;
    }

    public int getPid() { return pid; }
    public String getName() { return name; }
    public User getOwner() { return owner; }
    public MyList<Event> getEvents() { return events; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }

    public FinishType getFinishType() { return finishType; }
    public void setFinishType(FinishType finishType) { this.finishType = finishType; }

    public User getTerminatedBy() { return terminatedBy; }
    public void setTerminatedBy(User terminatedBy) { this.terminatedBy = terminatedBy; }

    @Override
    public int compareTo(Process other) {
        return Integer.compare(other.priority, this.priority);
    }
}
