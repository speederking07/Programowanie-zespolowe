package pl.zespolowe.splix.domain.game;


import lombok.Getter;
import lombok.Setter;
import pl.zespolowe.splix.domain.game.overtakeElements.OverTake;
import pl.zespolowe.splix.domain.game.player.Player;
import pl.zespolowe.splix.dto.AddPlayer;
import pl.zespolowe.splix.dto.Move;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
/***
 *
 * @author KalinaMichal
 * Klasa odpowiedzialna za logikę planszy
 */
class Board {
    @Getter
    private final Map<Point, Checker> fields;
    @Getter
    private final Map<Point, Checker> paths;
    private final Map<Checker, List<Direction>> BotMoves = new HashMap<>();

    protected int x_size;
    protected int y_size;

    @Getter
    @Setter
    private GameListenerState gls;

    Board(int x, int y) {
        this.x_size = x;
        this.y_size = y;
        this.fields = new HashMap<>();
        this.paths = new LinkedHashMap<>();//musi byc linked abym wiedzial gdzie on doklasnie chodzil
        this.gls = new GameListenerState(0);
    }

    /***
     * Czyszczenie sladu
     * @param checker
     */
    private void clearPlayersSign(Checker checker) {

        ArrayList<Point> arrayPaths = new ArrayList<>();
        paths.forEach((k, v) -> {
            if (v.equals(checker)) {
                arrayPaths.add(k);
            }
        });
        for (Point p : arrayPaths) {
            paths.remove(p);
        }
    }

    /***
     * Przejmowanie nowych pol przez gracza
     * @param checker
     */
    private void overtake(Checker checker) {
        Point p2 = checker.getPoint();
        Point p1 = checker.getPath();
        Set<Point> myFields = new LinkedHashSet<>();
        Set<Point> finalMyFields = myFields;
        Set<Point> finalMyPaths = myFields;
        fields.forEach((k, v) -> {
            if (v.equals(checker)) finalMyFields.add(k);
        });
        paths.forEach((k, v) -> {
            if (v.equals(checker)) finalMyPaths.add(k);
        });
        //zdobywa obwod jako Set<Point> uporzadkowany
        myFields = OverTake.getCircuit(finalMyFields);
        Set<Point> myPath;//myPath to obwod ktory nalezy zamalowac
        //teraz szukamy sciezki z tego obwodu(szukamy jej szybko, nie szukam najkrotszej)
        myPath = OverTake.findPath(p1, p2, myFields, new LinkedHashSet<>());
        //polacz sciezke z tym co zwroci find path
        myPath.addAll(finalMyPaths);
        //teraz mam liste pol ktore nalezy zamalowac
        //maluje je
        Set<Point> taken = OverTake.paintPolygon2(myPath);
        //na prosbe Pana Marka biore tez liste samych zakretow do wyslania
        ArrayList<Point> curves = OverTake.getCurves(myPath);
        //gls.changeFields(checker,curves);
        for (Point tmp : taken) {
            fields.put(tmp, checker);
            gls.changeField(checker.getPlayer().getUsername(), tmp);
        }
        clearPlayersSign(checker);
    }

    /***
     * Zabujstwo gracza checker
     * @param checker
     */
    public void killPlayer(Checker checker) {
        clearPlayersSign(checker);
        Iterator<Map.Entry<Point, Checker>> iterator = fields.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Point, Checker> entry = iterator.next();
            if (entry.getValue().equals(checker)) {
                gls.killPlayer(entry.getValue());
                iterator.remove();
            }
        }
        if (fields.size() > 1) {
            fields.forEach((k, v) -> {
                if (v == checker) {
                    fields.remove(k);
                    gls.killPlayer(v);
                }
            });
        }
    }

    /***
     * Pojawienie sie nowego gracza na planszy
     * @param size_x
     * @param size_y
     * @param p
     * @return
     */
    public Checker respawnPlayer(int size_x, int size_y, Player p) {
        Point point0 = new Point();
        for (int i = 0; i < size_x * size_y / 5; i++) {
            //tu dalem od 2 do x-1 zeby sie nie respil przy scianie tak bardzo
            point0.x = ThreadLocalRandom.current().nextInt(2, size_x - 1);
            point0.y = ThreadLocalRandom.current().nextInt(2, size_y - 1);
            if (!fields.containsKey(point0)) {
                Checker ch = new Checker(p, point0);
                fields.put(new Point(point0.x, point0.y), ch);
                //gls.changeField(ch,point0);

                String name = ch.getPlayer().getUsername();
                fields.put(new Point(point0.x, point0.y + 1), ch);
                gls.changeField(name, new Point(point0.x, point0.y + 1));

                fields.put(new Point(point0.x + 1, point0.y + 1), ch);
                gls.changeField(name, new Point(point0.x + 1, point0.y + 1));

                fields.put(new Point(point0.x, point0.y + 2), ch);
                gls.changeField(name, new Point(point0.x, point0.y + 2));

                fields.put(new Point(point0.x + 1, point0.y + 2), ch);
                gls.changeField(name, new Point(point0.x + 1, point0.y + 2));

                gls.addPlayer(ch);
                return ch;
            }
        }
        return null;
    }

    /***
     * Ruch gracza ch w kierunku dir
     * @param ch
     * @param dir
     * @return
     */
    public Checker move(Checker ch, Direction dir) {
        Checker rtr = null;
        Point oldPoint = ch.getPoint();
        int x = oldPoint.x;
        int y = oldPoint.y;
        switch (dir) {
            case EAST -> x++;
            case WEST -> x--;
            case NORTH -> y--;
            case SOUTH -> y++;
        }
        Point p = new Point();
        p.x = x;
        p.y = y;
        if (p.getX() < 0 || p.getY() < 0 || p.getX() >= x_size || p.getY() >= y_size) {
            killPlayer(ch);
            return ch;
        }
        //System.out.println(p);
        if (paths.containsKey(p)) {
            //drobna uwaga: zabijam tego ktorego slad zostal najechany
            killPlayer(paths.get(p));
            rtr = paths.get(p);
        } else {
            //jesli byl u siebie i nie jest to nowy path
            //jesli byl u siebie i jest to nic
            if (fields.get(oldPoint) != null && fields.get(oldPoint).equals(ch)) {//byl u siebie
                ch.setPath(oldPoint);//ustawiam Patha jakby stąd zaczynal wyjazd z w next turn to bede wiedzial skad wyjechal
                if (fields.get(p) != null && fields.get(p).equals(ch)) {//i jest u siebie
                    ch.setPoint(p);
                    gls.playerMove(ch, false);
                } else {
                    ch.setPoint(p);
                    gls.playerMove(ch, true);
                    paths.put(ch.getPoint(), ch);
                }
            }

            //jesli nie byl u siebie i jest to overtake
            //jesli nie byl u siebie i jest to kolejny path
            else {//nie byl u siebie
                if (fields.get(p) != null && fields.get(p).equals(ch)) {// i jest u siebie
                    ch.setPoint(p);
                    overtake(ch);
                    ch.setPath(p);
                    gls.playerMove(ch, false);
                } else {//i nie jest u siebie
                    ch.setPoint(p);
                    paths.put(ch.getPoint(), ch);
                    gls.playerMove(ch, true);
                }
            }
        }
        return rtr;
    }

    /***
     * Zwraca informacja o stanie gry dla gracza ktory dolaczyl
     * @param ch
     * @param gcs
     * @return
     */
    public GameCurrentState setInfoForNewPlayer(Checker ch, GameCurrentState gcs) {
        ArrayList<Point> points = new ArrayList<>();
        Set<Point> localPath;
        fields.forEach((k, v) -> {
            if (v == ch) points.add(k);
        });
        Set<Point> finalLocalPath = new LinkedHashSet<>();
        paths.forEach((k, v) -> {
            if (v == ch) finalLocalPath.add(k);
        });
        if (ch.getPath() != null) {
            localPath = OverTake.findPath(ch.getPath(), ch.getPoint(), finalLocalPath, new LinkedHashSet<>());
            ArrayList<Point> curves = OverTake.getCurves(localPath);
            gcs.addPlayer(ch.getPlayer(), points, curves, ch.getPoint());
        }
        return gcs;
    }

    /***
     * dodaje gracza do gamelistenerstate
     * @param ch1
     */
    public void addGlsPlayer(Checker ch1) {
        gls.addPlayer(ch1);

    }

    /*public void printGls() {
        int turn = gls.getTurn();
        List<String> killedPlayers = gls.getKilledPlayers();
        Map<String, ArrayList<int[]>> changes = gls.getChanges();
        List<Move> moves = gls.getMoves();
        List<AddPlayer> addedPlayers = gls.getAddedPlayers();
        System.out.println("###################################");
        System.out.println("info co zostalo wyslane w "+turn+" turze");
        System.out.println("killed players: "+killedPlayers);
        System.out.println("moves: ");
        for(Move m: moves) {
            System.out.println("x:" + m.getX()+" y:"+m.getY()+"name: "+m.getPlayer());
        }
        System.out.println("addedPlayers"+addedPlayers);
        System.out.println("field's changes"+changes);
        System.out.println("###################################");
    }*/

    /***
     * Ruch botow
     * @param ch
     * @return
     */
    public Checker botMove(Checker ch) {
        Checker rtr = null;
        Point oldPoint = ch.getPoint();
        int x = (int) oldPoint.getX();
        int y = (int) oldPoint.getY();
        switch (ch.getDirection()) {
            case EAST -> x++;
            case WEST -> x--;
            case NORTH -> y--;
            case SOUTH -> y++;
        }
        Point newPoint = new Point(x, y);
        if (ch.getPlayer().isRoadSet()) {//wiem gdzie isc
            if (fields.get(oldPoint) != null && fields.get(oldPoint).equals(ch)) {//jestem u siebie
                rtr = move(ch, ch.getDirection());
                if (!(fields.get(newPoint) != null && fields.get(newPoint).equals(ch))) {//w next turn nie bedzie u siebie
                    ch.getPlayer().isRoadSet(false);
                }
            } else {//nie jestem u siebie ale wiem gdzie isc
                rtr = move(ch, ch.getDirection());
                List<Direction> dirs = new ArrayList<>();
                List<Direction> finalDirs = dirs;
                if (BotMoves != null) {
                    BotMoves.forEach((k, v) -> {
                        if (k == ch) finalDirs.addAll(v);
                    });
                    if (!finalDirs.isEmpty()) {
                        ch.setDirection(finalDirs.get(0));
                        finalDirs.remove(0);
                        BotMoves.put(ch, finalDirs);
                    }
                }
                //dodaje do piona kolejny ruch z listy
                //usuwam element z listy
                if (fields.get(newPoint) != null && fields.get(newPoint).equals(ch)) {// w nastepnej turze jestem u siebie
                    Direction localDir;
                    Direction forbidden = ch.getDirection();
                    switch (ch.getDirection()) {
                        case EAST -> forbidden = Direction.WEST;
                        case WEST -> forbidden = Direction.EAST;
                        case NORTH -> forbidden = Direction.SOUTH;
                        case SOUTH -> forbidden = Direction.NORTH;
                    }
                    do {//losuje kierunek
                        localDir = Direction.values()[(int) (Math.random() * Direction.values().length)];
                    }
                    while (localDir == null || localDir == forbidden);
                    ch.setDirection(localDir);//losuje kierunek i oprozniam liste
                }
            }
        } else {//nie wiem gdzie isc
            //nie jestem u siebie i nie wiem gdzie isc
            //trzeba wyznaczyc droge
            Random random = new Random();
            Direction localDir;
            Direction forbidden = ch.getDirection();
            switch (ch.getDirection()) {
                case EAST -> forbidden = Direction.WEST;
                case WEST -> forbidden = Direction.EAST;
                case NORTH -> forbidden = Direction.SOUTH;
                case SOUTH -> forbidden = Direction.NORTH;
            }
            do {//losuje kierunek
                localDir = Direction.values()[(int) (Math.random() * Direction.values().length)];
            }
            while (localDir == null || localDir == forbidden);
            List<Point> movesList;
            List<Direction> directionsList;
            //sprawdz czy wykoanie ruchu (3x ten kier)&&(2x obrot 90o || 2x obrot 2 wersja 90o)&& find path to nasza baza nie przekroczy planszy
            movesList = getMovesList(ch, localDir);//jesli nie mozliwe zwraca pusta liste a jesli mozliwe to zwraca liste punktow
            if (movesList.isEmpty()) {
                movesList = shortestPathToHomeland(ch, getNewPosition(ch.getPoint(), localDir));//wznacza najkrotsza droge do domu z punktu
            }
            directionsList = getDirectionsFromPath(movesList);//zamienia liste punktow na liste kierunkow
            BotMoves.put(ch, directionsList);
            //losuj kierunek
            //zadbaj aby nie byl przeciwienstwem tego z ktorego przyszedles-OK
            //jesli przekroczy (shortest path w next)
            ch.getPlayer().isRoadSet(true);
            botMove(ch);
        }
        return rtr;
    }

    /***
     * Tworzenie listy ruchow dla Bota (taki plan na przyszlosc)
     * @param ch
     * @param localDir
     * @return
     */
    //sprawdz czy wykoanie ruchu (3x ten kier)&&(2x obrot 90o || 2x obrot 2 wersja 90o)&& find path to nasza baza nie przekroczy planszy
    List<Point> getMovesList(Checker ch, Direction localDir) {
        List<Point> rtr = new ArrayList<>();
        Point oldPoint = ch.getPoint();
        int x0 = (int) oldPoint.getX();
        int y0 = (int) oldPoint.getY();
        int x = 0;
        int y = 0;
        switch (ch.getDirection()) {
            case EAST -> x++;
            case WEST -> x--;
            case NORTH -> y--;
            case SOUTH -> y++;
        }
        rtr.add(new Point(x0 + x, y0 + y));
        rtr.add(new Point(x0 + 2 * x, y0 + 2 * y));
        rtr.add(new Point(x0 + 3 * x, y0 + 3 * y));
        rtr.add(new Point(x0 + 3 * x + y, y0 + 3 * y + x));
        rtr.add(new Point(x0 + 3 * x + 2 * y, y0 + 3 * y + 2 * x));
        rtr.add(new Point(x0 + 2 * x + 2 * y, y0 + 2 * y + 2 * x));
        rtr.add(new Point(x0 + x + 2 * y, y0 + y + 2 * x));
        rtr.addAll(shortestPathToHomeland(ch, new Point(x0 + x + 2 * y, y0 + y + 2 * x)));
        return rtr;
    }

    /***
     * Konwertuje liste-sciezke pol na liste kierunkow po kolei
     * @param movesList
     * @return
     */
    private List<Direction> getDirectionsFromPath(List<Point> movesList) {
        List<Direction> directionsList = new ArrayList<>();
        for (int i = 0; i < movesList.size() - 1; i++) {
            Point p1 = movesList.get(i);
            Point p2 = movesList.get(i + 1);
            if (p1.getX() == p2.getX()) {
                if (p1.getY() < p2.getY()) directionsList.add(Direction.SOUTH);
                else directionsList.add(Direction.NORTH);
            } else {
                if (p1.getX() < p2.getX()) directionsList.add(Direction.EAST);
                else directionsList.add(Direction.WEST);
            }
        }
        return directionsList;
    }

    /***
     * Zdobywa nowy punkt na podstawnie podanego kierunku
     * @param p
     * @param d
     * @return
     */
    private Point getNewPosition(Point p, Direction d) {
        int x = (int) p.getX();
        int y = (int) p.getY();
        switch (d) {
            case EAST -> x++;
            case WEST -> x--;
            case NORTH -> y--;
            case SOUTH -> y++;
        }
        return (new Point(x, y));
    }

    /***
     * Znajduje najkrotsza droge do domu(swoich pol)
     * @param checker
     * @param point
     * @return
     */
    private List<Point> shortestPathToHomeland(Checker checker, Point point) {
        List<Point> rtr = new ArrayList<>();
        //wyznaczam punkt najblizszy mi
        List<Point> potentialPointsList = fields
                .entrySet()
                .stream()
                .filter(e -> (checker.equals(e.getValue())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        double minPath = x_size + y_size;
        Point minPoint = new Point(-1, -1);
        for (Point p : potentialPointsList) {
            double tmp = Math.abs(p.getX() - point.getX()) + Math.abs(p.getY() - point.getY());
            if (tmp < minPath) {
                minPath = tmp;
                minPoint = p;
            }
        }
        if (minPoint.getX() < 0 || minPoint.getY() < 0) {
            return rtr;
        }
        while (minPoint.getX() < point.getX()) {
            point = new Point((int) point.getX() - 1, (int) point.getY());
            rtr.add(point);
        }
        while (minPoint.getX() > point.getX()) {
            point = new Point((int) point.getX() + 1, (int) point.getY());
            rtr.add(point);
        }
        while (minPoint.getY() > point.getY()) {
            point = new Point((int) point.getX(), (int) point.getY() + 1);
            rtr.add(point);
        }
        while (minPoint.getY() > point.getY()) {
            point = new Point((int) point.getX(), (int) point.getY() - 1);
            rtr.add(point);
        }

        return rtr;
    }
}