import java.util.ArrayList;

public class MyBot {

    public static GameMap gameMap;
    public static int myID;

    public static void main(String[] args) throws java.io.IOException {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    if(site.owner == myID) {
                        moves.add(new Move(new Location(x, y), selectDirection(new Location(x,y),site)));
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
        boolean advAround = false;

        for(Direction d : Direction.CARDINALS){
            tempSite = gameMap.getSite(currentLocation,d);

            if(tempSite.owner!=myID){
                advAround = true;
                if(tempSite.strength<currentSite.strength ){//|| currentSite.strength>255-currentSite.production){
                    targetSite=tempSite;
                    targetDirection = d;
                    /*if(tempSite.production>targetSite.production){
                        targetSite=tempSite;
                        targetDirection=d;
                    }else if(targetSite.strength>=currentSite.strength){
                        targetSite=tempSite;
                        targetDirection=d;
                    }*/
                }
            }else{
                if(!advAround){
                    int tempStr = tempSite.strength+tempSite.production+currentSite.strength;
                    if(tempStr<nextStrength && tempStr<=255 || targetDirection==Direction.STILL) {
                        targetDirection = d;
                        nextStrength = tempStr;
                    }
                }
            }
        }
        if(currentSite.strength<currentSite.production*5){
            targetDirection = Direction.STILL;
        }

        return targetDirection;
    }


}
