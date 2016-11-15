/**
 * Created by andre on 15/11/2016.
 */
public class PlayerState {
    private final int ID;
    private int territory;
    private int production;
    private GameMap gameMap;

    public void setTerritory(int territory) {
        this.territory = territory;
    }

    public void setProduction(int production) {
        this.production = production;
    }

    public void addTerritory(int territory) {
        this.territory += territory;
    }

    public void addProduction(int production) {
        this.production += production;
    }

    public int getID() {
        return ID;
    }

    public int getTerritory() {
        return territory;
    }

    public int getProduction() {
        return production;
    }

    public void updateGameMap(GameMap gameMap) {
        this.gameMap=gameMap;
    }


    public PlayerState(int ID, Location startingLocation, GameMap gameMap){
        this.ID=ID;
        territory=1;
        production = gameMap.getSite(startingLocation).production;
    }



}
