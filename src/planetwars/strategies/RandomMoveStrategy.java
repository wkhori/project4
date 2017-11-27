package planetwars.strategies;

import planetwars.publicapi.*;

import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Pick a move randomly for each planet. It doesn't even validate that the destination and source planets are connected!
 */
public class RandomMoveStrategy implements IStrategy {
    private Random random = new Random();

    @Override
    public void takeTurn(List<IPlanet> planets, IPlanetOperations planetOperations, Queue<IEvent> eventsToExecute) {
        // For every planet that we own, pick a random number of people to send to another planet.
        // We don't check to make sure that the planet is connected to the current one; the result
        // is that often the strategy will generate invalid requests. The game engine ignores them,
        // so nothing negative happens (other than people are not moved).
        for (IPlanet planet : planets) {
            if (planet instanceof IVisiblePlanet) {
                IVisiblePlanet visiblePlanet = (IVisiblePlanet) planet;
                if (visiblePlanet.getOwner() == Owner.SELF) {
                    int pop = random.nextInt((int)visiblePlanet.getPopulation());
                    IPlanet destination = (IPlanet) planets.toArray()[random.nextInt(planets.size())];
                    eventsToExecute.add(planetOperations.transferPeople(visiblePlanet, destination, pop));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Random";
    }

    @Override
    public boolean compete() {
        return false;
    }
}
