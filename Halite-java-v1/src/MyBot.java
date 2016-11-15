import java.util.ArrayList;
import java.util.LinkedList;

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
                        AggressionSite as = setupAggression(location,site);
                        if(as.aggro!=Direction.STILL) {
                            moves.add(new Move(location, selectAggroDirection(location, as)));
                        }else{
                            moves.add(new Move(location,selectDirection(location,site)));
                        }
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


    private static Direction selectAggroDirection(Location currentLocation, AggressionSite currentSite){
        Site targetSite = gameMap.getSite(currentLocation,currentSite.aggro);
        if(targetSite.owner==myID){
            if(currentSite.strength>currentSite.production*5){
                return currentSite.aggro;
            }else{
                return Direction.STILL;
            }
        }else if(targetSite.owner==barbID){
            if (targetSite.strength < currentSite.strength || currentSite.strength>255-currentSite.production){
                return currentSite.aggro;
            }else{
                return Direction.STILL;
            }
        }else{
           return currentSite.aggro;
        }
    }

    private static AggressionSite setupAggression(Location loc, Site site){
        int distance = 256;
        int tempDistance;
        Direction aggressionD = Direction.STILL;
        for(Direction d : Direction.CARDINALS){
            tempDistance = getAggressionDistance(loc,d);
            if(distance>tempDistance){
                distance = tempDistance;
                aggressionD = d;
            }
        }
        if(distance==256){
            aggressionD = Direction.STILL;
        }
        AggressionSite as = new AggressionSite(site);
        as.aggro = aggressionD;
        return as;
    }
    private static int getAggressionDistance(Location l, Direction d){
        int i = 0;
        Site tempSite = gameMap.getSite(l,d);
        Location tempLoc = gameMap.getLocation(l,d);
        int maxDist = (d==Direction.NORTH || d==Direction.SOUTH) ? gameMap.height : gameMap.width;
        while(!playersID.contains(tempSite.owner)&& i<maxDist){
            i++;
            tempSite = gameMap.getSite(tempLoc,d);
            tempLoc = gameMap.getLocation(tempLoc,d);
        }
        if(i==maxDist){
            return 256;
        }
        return i;
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

    private static class AggressionSite extends Site{
        public Direction aggro;
        public int targetID;
        public AggressionSite(Site site){
            this.production=site.production;
            this.owner = site.owner;
            this.strength = site.strength;
        }
    }

}
