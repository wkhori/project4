package planetwars.strategies;

import planetwars.publicapi.*;

import java.util.*;



public class StrategyOne implements IStrategy{


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

                    Set<IEdge> edges = visiblePlanet.getEdges();
                    Iterator<IEdge> iterator = edges.iterator();

                    while (iterator.hasNext()) {
                        IEdge nextEdge = iterator.next();
                        int nextPlanetID = nextEdge.getDestinationPlanetId();
                        IVisiblePlanet nextPlanet = (IVisiblePlanet) findPlanet(planets, nextPlanetID);
                        if (nextPlanet.getOwner() == Owner.NEUTRAL && visiblePlanet.getPopulation() > 2) {
                            eventsToExecute.add(planetOperations.transferPeople(visiblePlanet, nextPlanet, 1));
                        }
                        if (nextPlanet.getOwner() == Owner.OPPONENT && (visiblePlanet.getPopulation()) > nextPlanet.getPopulation() * 1.6) {
                            eventsToExecute.add(planetOperations.transferPeople(visiblePlanet, nextPlanet, (long) Math.ceil(nextPlanet.getPopulation() * 1.2)));
                        }

                    }


                }


                // int pop = random.nextInt((int)visiblePlanet.getPopulation());
                //IPlanet destination = (IPlanet) planets.toArray()[random.nextInt(planets.size())];
                //eventsToExecute.add(planetOperations.transferPeople(visiblePlanet, destination, pop));
                else if (visiblePlanet.getOwner() == Owner.NEUTRAL) {


                } else if (visiblePlanet.getOwner() == Owner.OPPONENT) {

                }


            }

        }
    }

    public IPlanet findPlanet(List<IPlanet> planets, int id ){
        IPlanet returnPlanet = null;

        for (IPlanet planet : planets) {
            if(planet.getId()==id){

                return planet;
            }
        }
        return returnPlanet;
    }


    @Override
    public String getName () {
        return "Random";
        }

    @Override
    public boolean compete () {
        return false;
        }

}