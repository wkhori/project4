package planetwars.strategies;

import planetwars.publicapi.*;

import java.util.List;
import java.util.Queue;
import java.util.HashMap;

public class BasicStrategy implements IStrategy {
    // How to do this
    // Well... I'm tryna conquor 

    @Override
    public void takeTurn(List<IPlanet> planets, IPlanetOperations planetOperations, Queue<IEvent> eventsToExecute) {
    }

    @Override
    public String getName() {
        return "Rafi and Walid";
    }

    @Override
    public boolean compete() {
        return true;
    }
}
