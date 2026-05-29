package uy.edu.um.doors;

import uy.edu.um.tad.list.MyList;

public class Event {
    private final EventType type;
    private final MyList<String> instructions;

    public Event(EventType type, MyList<String> instructions) {
        this.type = type;
        this.instructions = instructions;
    }

    public EventType getType() { return type; }
    public MyList<String> getInstructions() { return instructions; }
}