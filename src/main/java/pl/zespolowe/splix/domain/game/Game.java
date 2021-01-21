package pl.zespolowe.splix.domain.game;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import pl.zespolowe.splix.domain.game.player.Player;
import pl.zespolowe.splix.dto.SimpleMove;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Game implements ObservableGame {
    private final static int x_size = 20;
    private final static int y_size = 20;
    private final static int max_players = 20;
    long startTime=-1;

    @Getter
    private final int gameID;

    @Getter
    private final List<GameListener> listeners;
    private final Board board;
    private final Set<Checker> players;
    private int turn;


    public Game(int gameID) {
        this.players = new HashSet<>();
        this.listeners = new ArrayList<>();
        this.gameID = gameID;
        this.board = new Board(x_size, y_size);
        this.turn = 0;
        board.setGls(new GameListenerState(0));
        log.info("NEW GAME: " + gameID);
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                newTurn();
//            }
//        }, 1000, 250);
    }

    //TODO: nie dostaję inforamcji o zmianie statustu pola - jest ok?
    private void publishEvent() {
        listeners.forEach(l -> l.event(board.getGls()));
    }

    private void killPlayer(Checker checker) {
        players.remove(checker);
        board.killPlayer(checker);
    }

    //TODO: brak automatycznego ruchu - to jest aktualne?
    public void move(SimpleMove move, Player player) {
        //TODO: niech się dzieje magia - ok, magia chyba juz dziala c'nie?
        Checker tmpChecker = null;
        for (Checker ch : players) if (ch.getPlayer().equals(player)) tmpChecker = ch;

        board.move(tmpChecker, move.getMove());

        log.info("Player's MOVE: " + player.getUsername());
    }

    public void resign(Player player) {
        List<Checker> ch = players.stream()
                .filter(t -> t.getPlayer().equals(player))
                .collect(Collectors.toList());

        if (ch.size() > 1) killPlayer(ch.get(0));
    }

    public boolean isActive() {
        return !players.isEmpty();
    }

    public boolean containsPlayer(Player player) {
        return players.stream().anyMatch(c -> c.getPlayer().equals(player));
    }

    /***
     * Pelna plansza lub nie ma miejsca na respawn
     * @return
     */
    public boolean isFull() {
        return players.size() >= max_players;
    }

    /***
     * Dołącznie gracza
     * @param p
     * @return
     */
    public boolean join(Player p) {
        if (!isFull()) {
            Checker ch = board.respawnPlayer(x_size, y_size, p);
            if (ch == null) return false;
            players.add(ch); //Mozliwe że nie potrzebe ale u mnie nie działało bez tego
            GameListenerState gls = board.getGls();
            gls.addPlayer(ch);
            board.setGls(gls);

            return true;
        }
        return false;
    }

    /***
     * Obecny stan gry dla nowego gracza który dołączył
     * @return
     */
    public GameCurrentState getGameCurrentState() {
        GameCurrentState gcs = new GameCurrentState();
        for (Checker ch : players) {
            gcs = board.setInfoForNewPlayer(ch, gcs);
        }
        return gcs;
    }

    /**
     * Nstępna tura
     */
    public void newTurn() {
        startTime = (startTime==-1) ? System.currentTimeMillis():-1;
        log.info("NEXT TURN");
        turn++;
        board.printGls();//Marek rutaj sb sprawdz co wysyla server
        publishEvent();
        board.setGls(new GameListenerState(turn));
    }

    @Override
    public void addListener(@NonNull GameListener listener) {
        listeners.add(listener);
    }
}