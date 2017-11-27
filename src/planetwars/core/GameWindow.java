package planetwars.core;

import planetwars.publicapi.IStrategy;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

public final class GameWindow extends JFrame {

    // Main panels
    private JPanel main; // Hosts the window
    private JPanel game; // Hosts the actual game

    // Control related swings
    private JPanel control;
    private JPanel selectorPane;
    private JPanel labelPane;
    private JPanel selectionPane;
    // Buttons
    private JButton startGameButton;
    private JButton newGameButton;
    private JButton exitButton;
    private JButton pauseGameButton;
    private JPanel playerPane;
    private JPanel buttonPane;
    private JPanel fpsPane;
    private JSpinner fpsSpinner;
    // Selectors
    private JComboBox player1Selector;
    private JComboBox player2Selector;
    private JComboBox graphSelector;

    private Class<? extends IStrategy> strategy1Class;
    private Class<? extends IStrategy> strategy2Class;

    private PlanetWarsFrame gameFrame;

    public static final int GAME_WINDOW_WIDTH = 1200;
    public static final int GAME_WINDOW_HEIGHT = 1000;

    public static final int PLANET_WARS_WIDTH = 1000;
    public static final int PLANET_WARS_HEIGHT = 1000;

    public GameWindow() throws FileNotFoundException {
        setSize(GAME_WINDOW_WIDTH, GAME_WINDOW_HEIGHT);
        add(main);
        initButtons();
        initSpinner();
        initSelectors();

        initGame();
    }

    public GameWindow(Class<? extends IStrategy> strategy1Class, Class<? extends IStrategy> strategy2Class) throws FileNotFoundException {
        this.strategy1Class = strategy1Class;
        this.strategy2Class = strategy2Class;

        setSize(GAME_WINDOW_WIDTH, GAME_WINDOW_HEIGHT);
        add(main);
        initButtons();
        initSpinner();
        initSelectors();
        disablePlayerSelectors();

        initGame();
    }

    private void initGame() throws FileNotFoundException {
        IStrategy player1;
        IStrategy player2;

        if (this.strategy1Class == null || this.strategy2Class == null) {
            final String jar1 = String.valueOf(player1Selector.getSelectedItem());
            final String jar2 = String.valueOf(player2Selector.getSelectedItem());

            player1 = Assets.loadPlayer(jar1);
            player2 = Assets.loadPlayer(jar2);
        } else {
            player1 = Assets.loadPlayer(strategy1Class);
            player2 = Assets.loadPlayer(strategy2Class);
        }

        String graph = String.valueOf(graphSelector.getSelectedItem());
        PlanetWars wars = new PlanetWars(player1, player2, graph);

        // If there is an existing gameFrame, we are restarting and need to cancel the old one.
        // If we don't cancel it, it will continue trying to draw and crash when it is removed from the window.
        if (gameFrame != null) {
            gameFrame.cancel();
            game.remove(gameFrame);
        }

        gameFrame = new PlanetWarsFrame(PLANET_WARS_WIDTH, PLANET_WARS_HEIGHT, "PlanetWars", wars);
        game.add(gameFrame);
        gameFrame.pause();
    }


    private void initButtons() {
        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameFrame.start();
                graphSelector.setEnabled(false);
            }
        });

        pauseGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameFrame.pause();
                graphSelector.setEnabled(true);
            }
        });

        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    initGame();
                } catch (FileNotFoundException e1) {
                    // TODO: How do we give this as feedback?
                    e1.printStackTrace();
                }
                gameFrame.pause();
                gameFrame.repaint();
                graphSelector.setEnabled(true);
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });
    }

    private void initSpinner() {
        fpsSpinner.setModel(new SpinnerNumberModel(60, 1, 60, 1));
        fpsSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gameFrame.setFPS((Integer) fpsSpinner.getValue());
            }
        });
    }

    private void disablePlayerSelectors() {
        player1Selector.setEnabled(false);
        player2Selector.setEnabled(false);
    }

    private void initSelectors() {
        initJarSelectors();
        initMapSelector();
    }

    private void initJarSelectors() {
        String[] strategies = Assets.getStrategies();

        assert strategies != null;
        for (String strategy : strategies) {
            String clean = strategy.replaceAll(".jar", "");
            player1Selector.addItem(clean);
            player2Selector.addItem(clean);
        }

        player1Selector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IStrategy player1 = Assets.loadPlayer(String.valueOf(player1Selector.getSelectedItem()));
                gameFrame.setPlayer1(player1);
            }
        });

        player2Selector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IStrategy player2 = Assets.loadPlayer(String.valueOf(player2Selector.getSelectedItem()));
                gameFrame.setPlayer2(player2);
            }
        });
    }

    private void initMapSelector() {
        String[] maps = Assets.getGraphs();

        for (String map : maps) {
            String clean = map.replaceAll(".dot", "");
            graphSelector.addItem(clean);
        }

        graphSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    initGame();
                    gameFrame.repaint();
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }
}
