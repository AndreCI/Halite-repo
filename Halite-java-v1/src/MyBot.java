import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

public class MyBot {

    public static GameMap gameMap;
    public static int myID;
    public static int frameCounter = 0;
    public static int totalTerritoryOnMap=0;
    public static int totalProductionOnMap=0;
    public static int barbID;
    public static PlayerState myPlayerState;
    public static LinkedList<Integer> playersID;
    public static LinkedList<PlayerState> players;
    public static Mode mode = Mode.FIGHT;

    public enum Mode{
        EXPLORATION, FIGHT;
    }

    public static void main(String[] args) throws java.io.IOException {
        playersID = new LinkedList<>();
        players = new LinkedList<>();
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                totalProductionOnMap+=site.production;
                totalTerritoryOnMap++;
                if(site.owner == myID) {
                    myPlayerState = new PlayerState(myID,new Location(x,y),gameMap);
                    Site barbSite = gameMap.getSite(new Location(x,y),Direction.NORTH);
                    barbID = barbSite.owner;
                }
            }
        }
        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                if(site.owner != myID && site.owner!=barbID && !playersID.contains(site.owner)) {
                    playersID.add(site.owner);
                    players.add(new PlayerState(site.owner,new Location(x,y),gameMap));
                }
            }
        }

        Networking.sendInit("MyJavaBot");


        while(true) {
            frameCounter++;
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x,y);
                    Site site = gameMap.getSite(location);
                    if(site.owner == myID) {

                        if(mode!=Mode.FIGHT) {
                            for (Direction d : Direction.CARDINALS) {
                                Site tempSite = gameMap.getSite(location, d);
                                if (playersID.contains(tempSite.owner)) {
                                    mode = Mode.FIGHT;
                                }
                            }
                        }

                        if(mode == Mode.FIGHT) {
                            moves.add(new Move(location, selectAggroDirection(location, site)));
                        }else{
                            moves.add(new Move(location,selectDirection(location,site)));
                        }
                    }
                }
            }
            Networking.sendFrame(moves);

            for(PlayerState ps : players){
                ps.setProduction(0);
                ps.setTerritory(0);
                ps.updateGameMap(gameMap);
            }
            myPlayerState.setProduction(0);
            myPlayerState.setTerritory(0);
            for(int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x,y);
                    Site site = gameMap.getSite(location);
                    int id = site.owner;
                    if(id!=barbID && id!=myID){
                        PlayerState ps = getPlayerFromID(id);
                        ps.addProduction(site.production);
                        ps.addTerritory(1);
                    }else if(id == myID){
                        myPlayerState.addTerritory(1);
                        myPlayerState.addProduction(site.production);
                    }
                }
            }

        }
    }

    public static PlayerState getPlayerFromID(int ID){
        return players.get(playersID.indexOf(ID));
    }
    private static Direction selectAggroDirection(Location currentLocation, Site currentSite){
        LinkedList<Direction> enemies = spotEnemies(currentLocation);

        if(enemies.isEmpty()){
            return selectDirection(currentLocation,currentSite);
        }else {
            int minStrength = 256;
            Direction targetDirection = checkAround(currentLocation,enemies);
            /*for (Direction d : enemies) {
                Site tempSite = gameMap.getSite(currentLocation, d);
                if (minStrength>tempSite.strength){
                    minStrength=tempSite.strength;
                    targetDirection = d;
                }
            }*/
            Site tempSite = gameMap.getSite(currentLocation,targetDirection);
            if(!playersID.contains(tempSite.owner)){
                if( tempSite.strength >= currentSite.strength && currentSite.strength<=255-currentSite.production) {
                    targetDirection = Direction.STILL;
                }
            }
            return targetDirection;
        }
    }
    public static LinkedList<Direction> spotEnemies(Location loc){
        LinkedList<Direction> enemies = new LinkedList<>();
        for(Direction d1 : Direction.CARDINALS){
            for(Direction d2 : Direction.CARDINALS){
                //if(!((d1==Direction.SOUTH && d2 == Direction.NORTH) || (d1==Direction.NORTH && d2==Direction.SOUTH) || (d1==Direction.EAST && d2==Direction.WEST) || (d1==Direction.WEST && d2==Direction.EAST))){
                    Site tempSite = gameMap.getSite(gameMap.getLocation(loc,d1),d2);
                    if(playersID.contains(tempSite.owner) && !enemies.contains(d1)){
                        enemies.add(d1);
                    }
                //}
            }
        }
        return enemies;
    }

    public static Direction checkAround(Location loc, LinkedList<Direction> enemies){
        HashMap<Direction,Integer> bestMove = new HashMap<>();
        for(Direction d1 : enemies){
            bestMove.put(d1,0);
            for(Direction d2 : Direction.CARDINALS){
                if(!((d1==Direction.SOUTH && d2 == Direction.NORTH) || (d1==Direction.NORTH && d2==Direction.SOUTH) || (d1==Direction.EAST && d2==Direction.WEST) || (d1==Direction.WEST && d2==Direction.EAST))){
                    Site tempSite = gameMap.getSite(gameMap.getLocation(loc,d1),d2);
                    if(tempSite.owner==myID){
                        bestMove.put(d1,bestMove.get(d1)+tempSite.strength);
                    }
                }
            }
        }
        int i = 500;
        Direction finalDir = Direction.STILL;
        for(Direction d : bestMove.keySet()){
            if(bestMove.get(d) < i){
                i = bestMove.get(d);
                finalDir = d;
            }
        }
        return finalDir;
    }
//TODO : check if 255 str friends are near, if yes, dodge them #SelfDodgeBot
    private static Direction selectDirection(Location currentLocation,Site currentSite){
        Site tempSite;
        Direction targetDirection = Direction.STILL;
        int minStrength = 256;
        double currentDistance = 255;
        boolean barbAround = false;

        for(Direction d : Direction.CARDINALS){
            tempSite = gameMap.getSite(currentLocation,d);

            if(tempSite.owner!=myID){
                barbAround = true;
                if(tempSite.strength<minStrength) {
                    minStrength = tempSite.strength;
                    if (tempSite.strength < currentSite.strength || currentSite.strength>255-currentSite.production){
                        targetDirection = d;
                    } else {
                        targetDirection = Direction.STILL;
                    }
                }
            }else{
                if(!barbAround){
                    double tempDistance = getHeuristicDistance(currentLocation, d);
                    if (tempDistance < currentDistance) {
                        targetDirection = d;
                        currentDistance = tempDistance;
                    }

                }
            }
        }
        if(!barbAround && reinforce(currentSite,currentDistance)) {
                targetDirection = Direction.STILL;
        }

        return targetDirection;
    }

    public static boolean reinforce(Site currentSite, double distanceToBorder){
        if(currentSite.strength==0){
            return true;
        }else if(distanceToBorder>20){
            return false;
        }else{
            return currentSite.strength < currentSite.production*(20 - distanceToBorder);
        }
    }




    private static int getHeuristicDistance(Location l, Direction d){
        int i = 0;
        Site tempSite = gameMap.getSite(l,d);
        Location tempLoc = gameMap.getLocation(l,d);
        int maxDist = (d==Direction.NORTH || d==Direction.SOUTH) ? gameMap.height : gameMap.width;
        while(tempSite.owner==myID && i<maxDist){
            i++;
            tempSite = gameMap.getSite(tempLoc,d);
            tempLoc = gameMap.getLocation(tempLoc,d);
        }
        return i+1;
    }

}
