package planetwars.core;

import planetwars.publicapi.*;
import planetwars.strategies.RandomMoveStrategy;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;

final class PlanetWars implements IPlanetLookup {
    private static final int MOVE_TIMEOUT = 1;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private Map<Integer, Planet> planetMap = new HashMap<>();
    private IStrategy player1;
    private IStrategy player2;
    private PlanetWarsFrame observer;
    private PlanetOperations player1Operations;
    private PlanetOperations player2Operations;
    private boolean player1Turn;
    private boolean gameOver;

    private ExecutorService moveExecutor; // Handles timeouts
    private InternalPlayer winner;

    public PlanetWars(IStrategy player1, IStrategy player2, String graph) throws FileNotFoundException {
        this.player1 = player1;
        this.player2 = player2;
        this.player1Operations = new PlanetOperations(this, InternalPlayer.PLAYER1);
        this.player2Operations = new PlanetOperations(this, InternalPlayer.PLAYER2);
        this.player1Turn = true;
        this.gameOver = false;

        this.moveExecutor = Executors.newSingleThreadExecutor();
        this.loadGraph(graph);
    }

    private void loadGraph(String graph) throws FileNotFoundException {
        this.planetMap = SystemLoader.load(graph, this);
    }

    public void gameTick() {
        if (this.gameOver) {
            return;
        }

        // Time passes on each planet
        for (Planet planet : this.planetMap.values()) {
            planet.grow();
            planet.processShuttles();
            planet.shrink();
        }

        // Check for win conditions
        if (hasWon(InternalPlayer.PLAYER1) || hasWon(InternalPlayer.PLAYER2)) {
            this.gameOver = true;
            return;
        }

        IStrategy player;
        InternalPlayer playerToken;
        IPlanetOperations operations;
        if (this.player1Turn) {
            player = this.player1;
            playerToken = InternalPlayer.PLAYER1;
            operations = this.player1Operations;
        } else {
            player = this.player2;
            playerToken = InternalPlayer.PLAYER2;
            operations = this.player2Operations;
        }

        Queue<IEvent> eventsToProcess = new ArrayDeque<>();
        List<IPlanet> snapshot = getPlanetsSnapshot(playerToken);

        // Let the player make their moves
        Future<?> turn = moveExecutor.submit(new Runnable() {
            @Override
            public void run() {
                player.takeTurn(snapshot, operations, eventsToProcess);
            }
        });
        try {
            turn.get(MOVE_TIMEOUT, TIME_UNIT);
        } catch (InterruptedException | TimeoutException e) {
            // Turn skips, multiple catches in case we want to do something else
            // due to timeouts
        } catch (ExecutionException e) {
            // welp, ok then
        }

        // Process the player's moves
        for (IEvent event : eventsToProcess) {
            if (event instanceof Shuttle) {
                Shuttle shuttle = (Shuttle) event;
                if (this.lookupPlanet(shuttle.getSourcePlanetId()).checkAndLaunchShuttle(shuttle)) {
                    this.lookupPlanet(shuttle.getDestinationPlanetId()).addIncomingShuttle(shuttle);
                    if (this.observer != null) {
                        this.observer.notifyNewShuttle(shuttle);
                    }
                }
            }
        }

        this.player1Turn = !this.player1Turn;
    }

    public boolean hasWon(InternalPlayer player) {
        for (Planet planet : this.planetMap.values()) {
            // If the other player owns a planet, the game is still going
            if (planet.getOwnerFromViewer(player) == Owner.OPPONENT) {
                return false;
            }

            // It isn't over until the last shuttle says it's over
            for (IShuttle shuttle : planet.getIncomingIShuttles(player)) {
                if (shuttle.getOwner() == Owner.OPPONENT) {
                    return false;
                }
            }
        }
        this.moveExecutor.shutdownNow();
        winner = player;
        return true;
    }

    public boolean isOver() {
        return gameOver;
    }

    public InternalPlayer getWinner() {
        return gameOver ? winner : InternalPlayer.NEUTRAL;
    }

    @Override
    public Planet lookupPlanet(int id) {
        return planetMap.getOrDefault(id, null);
    }

    @Override
    public Collection<Planet> getPlanets() {
        return this.planetMap.values();
    }

    public List<IPlanet> getPlanetsSnapshot(InternalPlayer viewer) {
        // Record which planets are visible
        HashMap<Integer, Planet> visiblePlanets = new HashMap<>();
        for (Planet planet : this.planetMap.values()) {
            Owner owner = planet.getOwnerFromViewer(viewer);
            if (owner == Owner.SELF) {
                visiblePlanets.put(planet.getId(), planet);
                for (IEdge edge : planet.getIEdges()) {
                    visiblePlanets.put(edge.getDestinationPlanetId(), this.lookupPlanet(edge.getDestinationPlanetId()));
                }
            }
        }

        // The remaining planets are not visible
        HashMap<Integer, Planet> nonvisiblePlanets = new HashMap<>();
        for (Planet planet : this.planetMap.values()) {
            if (!visiblePlanets.containsKey(planet.getId())) {
                nonvisiblePlanets.put(planet.getId(), planet);
            }
        }

        // Add VisiblePlanetSnapshots for visible planets
        List<IPlanet> snapshots = new ArrayList<>();
        for (Planet planet : visiblePlanets.values()) {
            snapshots.add(planet.getVisiblePlanetSnapshot(viewer));
        }

        // Add PlanetSnapshots for other planets
        for (Planet planet : nonvisiblePlanets.values()) {
            snapshots.add(planet.getPlanetSnapshot(viewer));
        }

        return snapshots;
    }

    @Override
    public String toString() {
        return planetMap.toString();
    }

    public void setPlayer1(IStrategy player) {
        this.player1 = player;
    }

    public void setPlayer2(IStrategy player) {
        this.player2 = player;
    }

    public void setObserver(PlanetWarsFrame observer) {
        this.observer = observer;
    }

    public static void main(String[] args) throws FileNotFoundException {
        IStrategy strategy1 = new RandomMoveStrategy();
        IStrategy strategy2 = new RandomMoveStrategy();
        PlanetWars planetWars = new PlanetWars(strategy1, strategy2, "rings");

        int rounds = 0;
        while (!planetWars.hasWon(InternalPlayer.PLAYER1) && !planetWars.hasWon(InternalPlayer.PLAYER2)) {
            planetWars.gameTick();
            rounds++;
        }

        String winner = planetWars.hasWon(InternalPlayer.PLAYER1) ? "Player One" : "Player Two";
        System.out.print(String.format("%s has won in %d rounds!", winner, rounds));
    }
}
