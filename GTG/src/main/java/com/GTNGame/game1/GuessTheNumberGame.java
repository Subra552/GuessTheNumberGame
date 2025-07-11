package com.GTNGame.game1;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A GUI-based "Guess the Number" game for multiple players and multiple rounds.
 */
public class GuessTheNumberGame extends JFrame {

    // --- NEW: Difficulty Enum ---

    /**
     * Defines the difficulty levels for the game, each with a display name and a max number.
     */
    private enum Difficulty {
        SUPER_EASY("Super-Easy (1-5)", 5),
        SOMEWHAT_EASY("Somewhat-Easy (1-10)", 10),
        EASY("Easy (1-30)", 30),
        SOMEWHAT_MEDIUM("Somewhat-Medium (1-35)", 35),
        MEDIUM("Medium (1-50)", 50),
        SOMEWHAT_HARD("Somewhat-Hard (1-75)", 75),
        EXTREMELY_HARD("Extremely-Hard (1-100)", 100),
        NEXT_TO_IMPOSSIBLE("Next-to-Impossible (1-100,000)", 100000);

        private final String displayName;
        private final int maxNumber;

        Difficulty(String displayName, int maxNumber) {
            this.displayName = displayName;
            this.maxNumber = maxNumber;
        }

        public int getMaxNumber() {
            return maxNumber;
        }

        @Override
        public String toString() {
            // This is what will be displayed in the dropdown menu
            return displayName;
        }
    }

    // --- Game Logic Fields ---
    private final List<Player> players = new ArrayList<>();
    private int totalRounds;
    private int currentRound = 1;
    private int currentPlayerIndex = 0;
    private int numberToGuess;
    private final Random random = new Random();
    private int gameMaxNumber; // MODIFIED: No longer a constant

    // --- GUI Components ---
    private final JLabel titleLabel;
    private final JLabel roundLabel;
    private final JLabel playerTurnLabel;
    private final JLabel instructionLabel;
    private final JTextField guessField;
    private final JButton guessButton;
    private final JTextArea scoreboardArea;

    /**
     * Player class to hold name and score.
     */
    private static class Player {
        final String name;
        int score;

        Player(String name) {
            this.name = name;
            this.score = 0;
        }

        @Override
        public String toString() {
            return name + ": " + score + " points";
        }
    }

    public GuessTheNumberGame() {
        super("Guess The Number Game");

        // --- Initial Game Setup ---
        if (!setupGame()) {
            System.exit(0);
        }

        // --- GUI Initialization ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 450); // Increased height slightly for better layout
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        titleLabel = new JLabel("Guess The Number!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        roundLabel = new JLabel();
        playerTurnLabel = new JLabel();
        instructionLabel = new JLabel(); // MODIFIED: Text is set in startNewTurn()
        guessField = new JTextField();

        centerPanel.add(roundLabel);
        centerPanel.add(playerTurnLabel);
        centerPanel.add(instructionLabel);
        centerPanel.add(guessField);
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        guessButton = new JButton("Submit Guess");
        bottomPanel.add(guessButton);
        add(bottomPanel, BorderLayout.SOUTH);

        scoreboardArea = new JTextArea(10, 15);
        scoreboardArea.setEditable(false);
        scoreboardArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scoreboardArea.setBorder(BorderFactory.createTitledBorder("Scoreboard"));
        add(new JScrollPane(scoreboardArea), BorderLayout.EAST);

        guessButton.addActionListener(e -> processGuess());
        guessField.addActionListener(e -> processGuess());

        // --- Start the First Turn ---
        startNewTurn();
    }

    /**
     * Prompts for players, rounds, and difficulty to set up the game state.
     *
     * @return true if setup was successful, false if the user cancelled.
     */
    private boolean setupGame() {
        // Get number of players
        int numPlayers = getNumberFromDialog("Enter the number of players:", "Number of Players");
        if (numPlayers == -1) return false;

        // Get number of rounds
        totalRounds = getNumberFromDialog("Enter the number of rounds:", "Number of Rounds");
        if (totalRounds == -1) return false;

        // --- NEW: Get difficulty level ---
        Difficulty[] difficulties = Difficulty.values();
        Difficulty selectedDifficulty = (Difficulty) JOptionPane.showInputDialog(
                this,
                "Select a difficulty level:",
                "Difficulty Selection",
                JOptionPane.QUESTION_MESSAGE,
                null, // icon
                difficulties, // selection values (the enum constants)
                difficulties[0] // initial selection
        );

        if (selectedDifficulty == null) {
            return false; // User cancelled
        }
        this.gameMaxNumber = selectedDifficulty.getMaxNumber();
        // --- END NEW ---

        // Create player objects
        for (int i = 0; i < numPlayers; i++) {
            String playerName = JOptionPane.showInputDialog(this, "Enter name for Player " + (i + 1) + ":", "Player Setup", JOptionPane.QUESTION_MESSAGE);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Player " + (i + 1);
            }
            players.add(new Player(playerName));
        }
        return true;
    }

    private int getNumberFromDialog(String message, String title) {
        while (true) {
            String input = JOptionPane.showInputDialog(this, message, title, JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return -1;
            }
            try {
                int number = Integer.parseInt(input);
                if (number > 0) {
                    return number;
                } else {
                    JOptionPane.showMessageDialog(this, "Please enter a positive number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter a number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Starts a new turn for the current player.
     * Generates a new number and updates the UI.
     */
    private void startNewTurn() {
        // MODIFIED: Use the selected difficulty's max number
        numberToGuess = random.nextInt(gameMaxNumber) + 1;
        Player currentPlayer = players.get(currentPlayerIndex);

        // Update UI labels
        roundLabel.setText("Round: " + currentRound + " of " + totalRounds);
        playerTurnLabel.setText("Turn: " + currentPlayer.name);
        // MODIFIED: Set the instruction label text here with the correct range
        instructionLabel.setText("Enter your guess (1-" + gameMaxNumber + "):");
        guessField.setText("");
        guessField.requestFocus();
        updateScoreboard();
    }

    /**
     * Processes the player's guess from the text field.
     */
    private void processGuess() {
        String guessText = guessField.getText();
        int guess;
        try {
            guess = Integer.parseInt(guessText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Guess", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Player currentPlayer = players.get(currentPlayerIndex);
        String resultMessage;

        if (guess == numberToGuess) {
            currentPlayer.score += 5;
            resultMessage = "CORRECT! The number was " + numberToGuess + ".\nYou earned 5 points!";
        } else {
            currentPlayer.score -= 3;
            resultMessage = "WRONG! The number was " + numberToGuess + ".\nYou lost 3 points.";
        }

        JOptionPane.showMessageDialog(this, resultMessage, "Turn Over", JOptionPane.INFORMATION_MESSAGE);
        advanceToNextTurn();
    }

    /**
     * Moves the game to the next player or the next round.
     */
    private void advanceToNextTurn() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
            currentRound++;
        }

        if (currentRound > totalRounds) {
            endGame();
        } else {
            startNewTurn();
        }
    }

    /**
     * Ends the game, displays the final scores, and declares a winner.
     */
    private void endGame() {
        updateScoreboard();

        Player winner = players.get(0);
        boolean isTie = false;
        for (int i = 1; i < players.size(); i++) {
            if (players.get(i).score > winner.score) {
                winner = players.get(i);
                isTie = false;
            } else if (players.get(i).score == winner.score) {
                isTie = true;
            }
        }

        StringBuilder finalMessage = new StringBuilder("Game Over!\n\nFinal Scores:\n");
        for (Player player : players) {
            finalMessage.append(player.toString()).append("\n");
        }

        if (isTie) {
            finalMessage.append("\nIt's a tie!");
        } else {
            finalMessage.append("\nCongratulations, ").append(winner.name).append(" wins!");
        }

        JOptionPane.showMessageDialog(this, finalMessage.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);

        guessField.setEnabled(false);
        guessButton.setEnabled(false);
    }

    /**
     * Updates the scoreboard text area with the current scores.
     */
    private void updateScoreboard() {
        StringBuilder sb = new StringBuilder();
        for (Player player : players) {
            sb.append(player.toString()).append("\n");
        }
        scoreboardArea.setText(sb.toString());
    }

    /**
     * The main entry point for the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GuessTheNumberGame().setVisible(true);
        });
    }
}