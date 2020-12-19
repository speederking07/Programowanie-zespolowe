package pl.zespolowe.splix.domain.game;

import lombok.Getter;
import pl.zespolowe.splix.domain.game.sendElements.AddPlayer;
import pl.zespolowe.splix.domain.game.sendElements.Change;
import pl.zespolowe.splix.domain.game.sendElements.Move;

import java.awt.*;
import java.util.List;

public class GameListenerState{

    public GameListenerState(int turn) {
        this.turnNumer = turn;
    }

    @Getter
    int turnNumer=0;
    //liste stingow(graczy(sama nazwa))
    //liste change'ow - change
    //lista ruchow - moves
    //lista nowych graczy - players

    List<String >players;
    List<Change> changes;
    List<Move> moves;
    List<AddPlayer> addedPlayers;

   /* class Change{//pole ktore zminilo wlasciciela w danej turze.
        int x;
        int y;
        String player;//jak nan nikogo to pusty
    }

    class Move{
        int x;
        int y;
        boolean havePath;
        String player;
    }

    class AddPlayer{
        //kolor z colorsInCsv
        //nazwa String
        //wspolczedne x,y
    }

    */

}