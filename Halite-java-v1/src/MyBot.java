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

    public static void main(String[] args) throws java.io.IOException {
        playersID = new LinkedList<>();
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
                    if(site.owner == myID) {
                        moves.add(new Move(location, selectDirection(location,site)));
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    private static Direction selectDirection(Location currentLocation,Site currentSite){
        Site tempSite;
        Direction targetDirection = Direction.STILL;
        int maxProd = -1;
        int minStrength = 256;
        double currentDistance = 255;
        boolean advAround = false;

        for(Direction d : Direction.CARDINALS){
            tempSite = gameMap.getSite(currentLocation,d);

            if(tempSite.owner!=myID){
                advAround = true;
                if(tempSite.strength<minStrength) {
                    minStrength = tempSite.strength;
                    if (tempSite.strength < currentSite.strength || currentSite.strength>255-currentSite.production){
                        targetDirection = d;

                    } else {
                        targetDirection = Direction.STILL;
                    }
                }
            }else{
                if(!advAround){

                        double tempDistance = getHeuristicDistance(currentLocation, d);
                        if (tempDistance < currentDistance) {
                            targetDirection = d;
                            currentDistance = tempDistance;
                        }
                }
            }
        }
        if(currentSite.strength<currentSite.production*5){
            targetDirection = Direction.STILL;
        }

        return targetDirection;
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
        return i;
    }


}
