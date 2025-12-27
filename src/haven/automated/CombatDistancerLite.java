package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;

import static haven.OCache.posres;
import static haven.automated.CombatDistanceTool.animalDistances;
import static haven.automated.CombatDistanceTool.vehicleDistance;

public class CombatDistancerLite implements Runnable {

    private final GameUI gui;

    public CombatDistancerLite(GameUI gui) {
        this.gui = gui;
    }


    @Override
    public void run() {
        tryToAutoDistance();
    }

    private void tryToAutoDistance() {
        if (gui != null && gui.map != null && gui.map.player() != null && gui.fv.current != null) {
            Double value = -1.0;

            double addedValue = 0.0;

            Gob player = gui.map.player();
            if (player.occupiedGobID != null) {
                Gob vehicle = gui.ui.sess.glob.oc.getgob(player.occupiedGobID);
                if (vehicle != null && vehicle.getres() != null) {
                    addedValue = vehicleDistance.getOrDefault(vehicle.getres().name, 0.0);
                }
            }
            Gob enemy = getEnemy();
            if(enemy != null && enemy.getres() != null){
                value = animalDistances.get(enemy.getres().name);
            }
            if(value != null && value > 0){
                moveToDistance(value+addedValue);
            }

        }
    }

    private Gob getEnemy() {
        if (gui.fv.current != null) {
            long id = gui.fv.current.gobid;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    if (gob.id == id) {
                        return gob;
                    }
                }
            }
        }
        return null;
    }

    private void moveToDistance(double distance) {
        try {
            Gob enemy = getEnemy();
            if (enemy != null && gui.map.player() != null) {
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.", Color.WHITE);
            }
        } catch (NumberFormatException e) {
            gui.error("Wrong distance format. Use ##.###");
        }
    }

    private Coord2d getNewCoord(Gob enemy, double distance, double angle) {
        return new Coord2d(enemy.rc.x + distance * Math.cos(angle), enemy.rc.y + distance * Math.sin(angle));
    }

}
