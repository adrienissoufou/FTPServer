package impl;

import api.Event;
import api.observer.IObserver;

import java.util.ArrayList;
import java.util.HashMap;

public class ObserverNotificator {

    private HashMap<Event, ArrayList<IObserver>> eventMap;

    private ObserverNotificator() {
        eventMap = new HashMap<>();
        registerEvent(Event.FS);
        registerEvent(Event.LOG);
    }

    private static ObserverNotificator instance;

    public static ObserverNotificator getInstance() {
        if (instance == null) {
            instance = new ObserverNotificator();
        }
        return instance;
    }

    private void registerEvent(Event event) {
        eventMap.put(event, new ArrayList<>());
    }

    private boolean isRegistered(Event event) {
        return eventMap.containsKey(event);
    }

    public void addObserver(Event event, IObserver observer) {
        if (isRegistered(event)) {
            eventMap.get(event).add(observer);
        }
    }

    public void fire(Event event) {
        if (isRegistered(event)) {
            ArrayList<IObserver> observers = eventMap.get(event);
            for (IObserver observer : observers) {
                observer.updateState();
            }
        }
    }

}
