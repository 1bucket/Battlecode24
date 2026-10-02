package DiamondDoves;

import battlecode.common.*;

public strictfp class RobotPlayer {
	private static int myID;

	private static MapLocation target;


	public static void run(RobotController rc) {

		try {
			myID = rc.readSharedArray(GameConstants.SHARED_ARRAY_LENGTH) + 1;
			rc.writeSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1, myID);
		} catch (GameActionException e) {
			myID = 0;
		}


		while (true) {
			try {
				if (rc.isSpawned()) {
					assignTarget(rc);
					//					Movement.wander(rc);
					tryToPickup(rc);
					tryToAttack(rc);
					tryToHeal(rc);
					tryToFill(rc);
					tryToBuild(rc);
					tryToDig(rc);
					tryToGlobal(rc);
					if (rc.hasFlag()) {
						target = nearestSpawn(rc.getAllySpawnLocations(), rc.getLocation());
					}
					rc.setIndicatorString(target.toString());
					Movement.moveTowards(rc,target);
				}
				else {
					tryToSpawn(rc);
					if (rc.isSpawned()) {
						target = new MapLocation(rc.getLocation().x,
								rc.getMapHeight() - rc.getLocation().y);
						if (rc.getLocation().equals(target)) {
							target = new MapLocation(rc.getLocation().y,
									rc.getMapWidth() - rc.getLocation().x);
						}
					}
				}

				Clock.yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}

	private static void assignTarget(RobotController rc) throws GameActionException {
		FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());

		MapLocation myLocation = rc.getLocation();

		MapLocation nearestFlagLocation = null;

		if (enemyFlags.length > 0) {
			int minDistanceSquared = Integer.MAX_VALUE;

			for (FlagInfo flag : enemyFlags) {
				int distSquared = flag.getLocation().distanceSquaredTo(myLocation);
				if (distSquared < minDistanceSquared) {
					minDistanceSquared = distSquared;
					nearestFlagLocation = flag.getLocation();

				}
			}
		}
		if (nearestFlagLocation != null) {
			target = nearestFlagLocation;
		}	else {
			MapLocation[] broadcastedFlags = rc.senseBroadcastFlagLocations();

			if (broadcastedFlags.length > 0) {
				target = broadcastedFlags[0];
				for (MapLocation location : broadcastedFlags) {
					if (rc.getLocation().distanceSquaredTo(location) < rc.getLocation().distanceSquaredTo(target)) {
						target = location;
					}
				}
			} else {
				RobotInfo [] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

				if (enemies.length > 0) {
					target = enemies[0].getLocation();
				}
				else {
					target = new
							MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
				}
			}


		}
	}

	private static void tryToSpawn(RobotController rc) throws GameActionException {
		MapLocation[] spawnLocations = rc.getAllySpawnLocations();

		for (MapLocation loc: spawnLocations)	{
			if (rc.canSpawn(loc)){
				rc.spawn(loc);
				break;
			}
		}
	}

	private static void tryToPickup(RobotController rc) throws GameActionException {
		FlagInfo[] flags = rc.senseNearbyFlags(2,rc.getTeam().opponent());
		if (flags.length > 0) {
			MapLocation nearestFlagLocation = nearestFlag (flags, rc.getLocation());

			if (nearestFlagLocation != null) {
				if (rc.canPickupFlag(nearestFlagLocation)) {
					rc.pickupFlag(nearestFlagLocation);
				}
			}	else {
				tryToAttack(rc);
			}
		}


	}
	private static MapLocation nearestFlag (FlagInfo[] flags, MapLocation loc) {
		MapLocation nearest = null;
		int nearestDist = 99999;

		for (FlagInfo flag: flags) {
			if (flag.getLocation().distanceSquaredTo(loc) < nearestDist) {
				nearest = flag.getLocation();
				nearestDist = nearest.distanceSquaredTo(loc);
			}
		}


		return nearest;
	}

	private static MapLocation nearestSpawn (MapLocation[] locs, MapLocation focus) {
		MapLocation nearest = null;
		int nearestDist = 99999;

		for (MapLocation loc: locs) {
			if (loc.distanceSquaredTo(focus) < nearestDist) {
				nearest = loc;
				nearestDist=nearest.distanceSquaredTo(focus);
			}
		}
		return nearest;
	}

	private static void tryToAttack(RobotController rc) throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(4,rc.getTeam().opponent());

		for (RobotInfo enemy: enemies) {
			if (rc.canAttack(enemy.getLocation())) {
				rc.attack(enemy.getLocation());
				break;
			}
		}
	}



	private static void tryToHeal(RobotController rc) throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(4,rc.getTeam());

		for (RobotInfo ally: allies) {
			if (rc.canHeal(ally.getLocation())) {
				rc.heal(ally.getLocation());

			}
		}

	}
	private static void tryToFill(RobotController rc) throws GameActionException {
		MapLocation[] nearbyTiles = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);

		for (MapLocation loc : nearbyTiles) {
			if (rc.canFill(loc)) {
				rc.fill(loc);
				Clock.yield();
			}
		}
	}
	private static void tryToDig(RobotController rc) throws GameActionException {
		if (Math.random() * 100 < 3) {
			MapLocation mySpawnLocation = rc.getLocation();
			MapLocation[] nearbyTiles = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);

			for (MapLocation loc : nearbyTiles) {
				if (rc.canDig(loc) && !loc.equals(mySpawnLocation)) {
					rc.dig(loc);
					break;
				}
			}
		}
	}
	private static void tryToBuild(RobotController rc) throws GameActionException {
		MapLocation[] spawnLocations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);

		for (MapLocation spawnLoc : spawnLocations) {
			if (rc.canBuild(TrapType.EXPLOSIVE, spawnLoc))
				rc.build(TrapType.EXPLOSIVE, spawnLoc);


		}
	}
	private static void tryToGlobal(RobotController rc) throws GameActionException {
		if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
			rc.buyGlobal(GlobalUpgrade.HEALING);
		if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
			rc.buyGlobal(GlobalUpgrade.CAPTURING);
		if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
			rc.buyGlobal(GlobalUpgrade.ACTION);
	}
} 
