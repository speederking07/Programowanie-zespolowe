package pl.zespolowe.splix.domain.game;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class CircuitTest {
    public static void main(String[] args) {
        Set<Point> fields = new HashSet<>();
        Point p=new Point();
        //a
        p.x=0;
        p.y=0;
        fields.add(p);
        p=new Point();
        p.x=1;
        p.y=0;
        fields.add(p);
        p=new Point();
        p.x=2;
        p.y=0;
        fields.add(p);
        p=new Point();
//aa
        p.x=0;
        p.y=1;
        fields.add(p);
        p=new Point();
        p.x=1;
        p.y=1;
        fields.add(p);
        p=new Point();
        p.x=2;
        p.y=1;
        fields.add(p);
        //aa
        p.x=0;
        p.y=2;
        fields.add(p);
        p=new Point();
        p.x=1;
        p.y=2;
        fields.add(p);
        p=new Point();
        p.x=2;
        p.y=2;
        fields.add(p);

        System.out.println(isBorderPoint(p,fields));
        Set<Point> result =getCircuit(fields);




    }

    private static Set<Point> getCircuit(Set<Point> points){
        Set<Point> rtr = new HashSet<>();
        for(Point point: points){
            if(isBorderPoint(point, points)){ rtr.add(point);
            System.out.println(point);}
        }
        return rtr;
    }
    static boolean isBorderPoint(Point p, Set<Point> points){
        Set<Point> potentialPoints = new HashSet<>();
        int x=p.x;
        int y=p.y;

        //pointy potencjalne
        Point pot= new Point();
        pot.x=x+1;
        pot.y=y;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x-1;
        pot.y=y;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x;
        pot.y=y+1;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x;
        pot.y=y-1;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x+1;
        pot.y=y+1;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x+1;
        pot.y=y-1;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x-1;
        pot.y=y+1;
        potentialPoints.add(pot);
        pot= new Point();

        pot.x=x-1;
        pot.y=y-1;
        potentialPoints.add(pot);

        //koniec


        boolean rtr=true;
        for(Point potPo: potentialPoints){
            rtr=true;
            for(Point po: points) {
                if (potPo.equals(po)) {
                    rtr=false;
                }
            }
        }
        return rtr;
    }
}