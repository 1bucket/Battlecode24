package pestSnack;

import battlecode.common.*;


public class Setup{
	private static boolean hasAllFlagLocations = false;

	public static MapLocation target;
	
	public static void run(RobotController rc) throws GameActionException {
		storeFlagLocation(rc);
		
		if (rc.getRoundNum() <= 100) {
			//go to nearest ally flag and place traps close
			tryToPutDownTraps(rc);
		}
		else {
			tryToGetCrumbs(rc);
		}
		
		
		
//		
//		switch (RobotPlayer.myID/8) {
//		case 0:
////flag 1
//			break;
//		case 1:
//			break;
//		case 2:
//			break;
//		default:
//			tryToGetCrumbs(rc);
//		}
		
		assignTarget(rc);
		
		if (target !=null) {
			Movement.moveTowards(rc, target);
		}
		else {
			Movement.wander(rc);
		}
		Clock.yield();
		
//		if (RobotPlayer.myID > 0 && RobotPlayer.myID < 9) {
//			//1-8 (flag #1)
//		}
//		else {
//			if (RobotPlayer.myID > 8 && RobotPlayer.myID <17) {
//				//9-16 (flag #2)
//
//			}
//			else {
//				if (RobotPlayer.myID > 16 && RobotPlayer.myID <26) {
//					//17-25 (flag #3)
//				}
//				else {
//					if (RobotPlayer.myID > 25 && RobotPlayer.myID < 51) {
//						//26-50 (crumbs)
//						Movement.wander(rc);
////					}
//			}
//
//			}
//		}
	}
	
	
	private static void tryToPutDownTraps(RobotController rc) throws GameActionException {
		MapLocation[] trapLocations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);

		for (MapLocation loc: trapLocations) {
			if (rc.canBuild(TrapType.EXPLOSIVE, loc)) {
				rc.build(TrapType.EXPLOSIVE, loc);
				break;
			}
		}
		
	}


	private static void assignTarget(RobotController rc) throws GameActionException {
		int locationIndex = RobotPlayer.myID/8;
		int targetInt = rc.readSharedArray(locationIndex);
		if (targetInt > 0) {
			target = convertIntegerToMapLocation(targetInt);
		}
		else {
			target = null;
		}
	}


	// MapLocation of ID 306 = 1, loc of ID 405 = 2
	
private static int convertMapLocationToInteger (MapLocation loc) {	
	return 100*loc.x + loc.y;
	
}

private static MapLocation convertIntegerToMapLocation (int mapInteger) {
	int x = mapInteger/100;
	int y = mapInteger - x*100;
	
	return new MapLocation(x, y);
}

	private static void storeFlagLocation(RobotController rc) throws GameActionException {
		if (hasAllFlagLocations) {
			return;
		}
		//sense any nearby flags
		FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
		
		for (FlagInfo flag: flags) {
			for (int i = 0; i < 3; i += 1) {
				int locInt = convertMapLocationToInteger(flag.getLocation());
				int storedValue = rc.readSharedArray(i);
				if (storedValue == 0) {
					if (rc.canWriteSharedArray(i, locInt)) {
						rc.writeSharedArray(i, locInt);
					}
				}
				else {
					if (storedValue == locInt) {
						break;
					}
				}
			}
		}
	}
// convert location to coordinates, convert coordinates to string, convert string to int


	public static MapLocation nearestCrumb(MapLocation[] locations, MapLocation focal) {
		
		MapLocation nearest = null;
		int nearestDistance = 999999;

		for (MapLocation loc : locations) {
			if (loc.distanceSquaredTo(focal) < nearestDistance) {
				nearest = loc;
				nearestDistance = nearest.distanceSquaredTo(focal);
			}
		}
			return nearest;
	}
	

	public static void tryToGetCrumbs(RobotController rc) throws GameActionException {
		target = nearestCrumb(rc.senseNearbyCrumbs(-1), rc.getLocation());
	}


	

}
