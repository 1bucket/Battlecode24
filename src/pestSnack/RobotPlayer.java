package pestSnack;

import battlecode.common.*;

public strictfp class RobotPlayer {
	public static int myID;

	private static MapLocation target;
	public boolean canAttack = false;

	public static final int SETUP_ROUNDS = 0;
	public static void run(RobotController rc) {
		try {
			myID = rc.readSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1) + 1;
			rc.writeSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1, myID);

		} catch (GameActionException e) {
			myID = 0;
		}
		while (true) {
			try {
				
				if (rc.isSpawned()) {
					
					if (rc.getRoundNum() <= GameConstants.SETUP_ROUNDS) {
						Setup.run(rc);
						
						
					
					}
					else {
						target = nearestFlagLocation (rc.senseNearbyFlags(-1, rc.getTeam().opponent()), rc.getLocation());
						if (target == null) {
						target = nearestLocation(rc.senseBroadcastFlagLocations(), rc.getLocation());
						}
						tryToPickUpFlag(rc);

						if (rc.hasFlag()) {
							target = nearestLocation(rc.getAllySpawnLocations(), rc.getLocation());
						}
						else {
							MapLocation nearestEnemy = nearestRobotLocation(rc.senseNearbyRobots(-1, rc.getTeam().opponent()), rc.getLocation());
							if (nearestEnemy != null) {
								if (rc.canAttack(nearestEnemy)) {
									rc.attack(nearestEnemy);
								}
								target = nearestEnemy;
							}





						}
						//Movement.wander(rc);
						Movement.moveTowards(rc, target);
						rc.setIndicatorString(target.toString());

					}
				}
				else {
					tryToSpawn(rc);

				}
				Clock.yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}












	private static void tryToPickUpFlag(RobotController rc) throws GameActionException {
		//find flag within pickup distance
		FlagInfo[] flags = rc.senseNearbyFlags(2, rc.getTeam().opponent());
		for (FlagInfo flag : flags) {
			if(rc.canPickupFlag(flag.getLocation())) {
				rc.pickupFlag(flag.getLocation());
				break;
			}
		}
	}


	public static void tryToSpawn(RobotController rc) throws GameActionException {
		if(rc.isSpawned() || myID > 50) {
			return;
		}

		MapLocation[] spawnLocations = rc.getAllySpawnLocations();

		for (MapLocation loc: spawnLocations) {
			if (rc.canSpawn(loc)) {
				rc.spawn(loc);
				break;
			}
		}
	}
	//sense broadcast location, go there, look around there; if you can pick up flag, pick up flag. 

		
	
	
	
	public static MapLocation nearestRobotLocation (RobotInfo[] robots , MapLocation loc) {
		MapLocation nearest = null;
		int nearestDistance = 999999;

		for (RobotInfo robot : robots) {
			if (robot.getLocation().distanceSquaredTo(loc) < nearestDistance) {
				nearest = robot.getLocation();
				nearestDistance = nearest.distanceSquaredTo(loc);
			}

		}


		return nearest;
	}

	public static MapLocation nearestFlagLocation (FlagInfo[] flags, MapLocation loc) {
		MapLocation nearest = null;
		int nearestDistance = 999999;

		for (FlagInfo flag : flags) {
			if (flag.getLocation().distanceSquaredTo(loc) < nearestDistance) {
				nearest = flag.getLocation();
				nearestDistance = nearest.distanceSquaredTo(loc);
			}

		}
		
		return nearest;
	}

	public static MapLocation nearestLocation (MapLocation[] locations , MapLocation focalPoint) {
		MapLocation nearest = null;
		int nearestDistance = 999999;

		for (MapLocation loc : locations) {
			if (loc.distanceSquaredTo(focalPoint) < nearestDistance) {
				nearest = loc;
				nearestDistance = nearest.distanceSquaredTo(focalPoint);
			}

		}

		return nearest;
	}

}

