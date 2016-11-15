import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;

public class MyBot {

    public static GameMap gameMap;
    public static int myID;
    public static int frameCounter = 0;
    public static int totalTerritory = 0;
    public static int totalProduction = 0;
    public static int barbID;
    public static LinkedList<Integer> playersID;
    public static HashMap<Location,Site> pointsOfConflict;
    public static mode currentMode;
    public static enum mode{
        EXPLORATION, SMALLFIGHT, BIGFIGHT;

        public static mode getMode(){
            if(pointsOfConflict.isEmpty()){
                return EXPLORATION;
            }else if(pointsOfConflict.size()<20){
                return SMALLFIGHT;
            }else{
                return BIGFIGHT;
            }
        }
    }

    public static void main(String[] args) throws java.io.IOException {
        playersID = new LinkedList<>();
        pointsOfConflict = new HashMap<>();
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");

        for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                if(site.owner == myID) {
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
                }
            }
        }

        while(true) {
            currentMode = mode.getMode();
            totalProduction = 0;
            totalTerritory = 0;
            frameCounter++;
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    Location location = new Location(x,y);
                    totalProduction+=site.production;
                    totalTerritory++;
                    if(pointsOfConflict.containsKey(location) && !playersID.contains(site.owner)){
                        pointsOfConflict.remove(location);
                    }
                    if(site.owner == myID) {
                        for(Direction d :Direction.CARDINALS){
                            Site tempSite = gameMap.getSite(location,d);
                            if(playersID.contains(tempSite.owner)){
                                pointsOfConflict.put(location,tempSite);
                            }
                        }
                        moves.add(new Move(location, selectDirection(location,site)));
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    private static Direction selectDirection(Location currentLocation,Site currentSite){
        Site tempSite;
        Site targetSite = gameMap.getSite(currentLocation,Direction.EAST);
        Direction targetDirection = Direction.STILL;
        int nextStrength = 255;
        int maxProd = -1;
        double currentDistance = 255;
        boolean advAround = false;

        for(Direction d : Direction.CARDINALS){
            tempSite = gameMap.getSite(currentLocation,d);

            if(tempSite.owner!=myID){
                advAround = true;
                if(tempSite.production>maxProd) {
                    maxProd = tempSite.production;
                    if (tempSite.strength < currentSite.strength || currentSite.strength>255-currentSite.production){
                        targetSite = tempSite;
                        targetDirection = d;

                    } else {
                        targetDirection = Direction.STILL;
                    }
                }
            }else{
                if(!advAround){
                    if(currentMode == mode.EXPLORATION) { //exploration mode

                        double tempDistance = getHeuristicDistance(currentLocation, d);
                        if (tempDistance < currentDistance) {
                            targetDirection = d;
                            currentDistance = tempDistance;
                        }
                    }else if(currentMode == mode.SMALLFIGHT){ //conquest mode
                        double bestDistance = 255;
                        double tempDistance;
                        Location bestLocation = currentLocation;
                        for(Location conflict : pointsOfConflict.keySet()){
                            tempDistance = getDistance(currentLocation,conflict);
                            if(tempDistance<bestDistance){
                                bestLocation = conflict;
                                bestDistance = tempDistance;
                            }
                        }
                        targetDirection =  getDirectionFromLocation(currentLocation,bestLocation);

                    }else{
                        if(pointsOfConflict.size()>20) {
                            double tempDistance = getHeuristicDistance(currentLocation, d);
                            if (tempDistance < currentDistance) {
                                targetDirection = d;
                                currentDistance = tempDistance;
                            }
                        }
                    }

                    /*if(totalTerritory<50){
                        int tempStr = tempSite.strength+tempSite.production+currentSite.strength;
                        if(tempStr<nextStrength && tempStr<=255 || targetDirection==Direction.STILL) {
                            targetDirection = d;
                            nextStrength = tempStr;
                        }
                    }else{
                        targetDirection = Direction.EAST;
                    }*/

                }
            }
        }
        if(currentSite.strength<currentSite.production*5){
            targetDirection = Direction.STILL;
        }

        return targetDirection;
    }

    private static double getDistance(Location l1, Location l2){
        return Math.sqrt(Math.pow((l1.x - l2.x),2) + Math.pow((l1.y-l2.y),2));
    }
    private static Direction getDirectionFromLocation(Location l1, Location l2){
        if(l1.equals(l2)){
            return Direction.NORTH;
        }
        int dist1 = Math.abs(l1.x - l2.x);
        int dist2 = Math.abs(l1.y - l2.y);
        if(dist1>dist2) {
            if (l1.x < l2.x && dist1 <= gameMap.width) {
                return Direction.EAST;
            } else if (l1.x < l2.x && dist1 > gameMap.width) {
                return Direction.WEST;
            } else if (l1.x > l2.x && dist1 <= gameMap.width) {
                return Direction.WEST;
            } else if (l1.x > l2.x && dist1 > gameMap.width) {
                return Direction.EAST;
            }
        }else {
            if (l1.y < l2.y && dist2 <= gameMap.height) {
                return Direction.SOUTH;
            } else if (l1.y < l2.y && dist2 > gameMap.height) {
                return Direction.NORTH;
            } else if (l2.y < l1.y && dist2 <= gameMap.height) {
                return Direction.NORTH;
            } else if (l2.y < l1.y && dist2 > gameMap.height) {
                return Direction.SOUTH;
            }
        }
        return Direction.NORTH;
    }

    private static int getHeuristicDistance(Location l, Direction d){
        int i = 0;
        Site tempSite = gameMap.getSite(l,d);
        Location tempLoc = gameMap.getLocation(l,d);
        while(tempSite.owner==myID && i<9){
            i++;
            tempSite = gameMap.getSite(tempLoc,d);
            tempLoc = gameMap.getLocation(tempLoc,d);
        }
        return i;
    }


}
