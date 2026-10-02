package SillyDucks;

import battlecode.common.*;
import java.util.Random;
//import java.util.Arrays;

/**
 * Movement will contain all methods associated with robot motion.
 * @author VolServe
 *
 */
public class Movement {
	private static Random rng = new Random(238467);
	
	private static Direction direction = randomDirection();
	public static MapLocation destination;
	private static boolean blocked = false;
	private static int distWhenBlocked = 999999;
	public static int blockedAttempts = 0;
	
	
	/**
	 * Move robot towards given location
	 * @return void
	 */
	public static void moveTowards(RobotController rc, MapLocation location) throws GameActionException{
		if (! rc.isMovementReady() || location == null) {
			return;
		}
		if (! location.equals(destination)) {
			destination = location;
			blocked = false;
			distWhenBlocked = 999999;
		}
		// check if already at given location
		if (rc.getLocation().equals(destination)) {
//			System.out.println("i'm here");
			return;
		}
		// otherwise, choose type of movement
		if (blocked) {
			
			blockedMovement(rc);
		}
		else {
			unblockedMovement(rc);
		}
		
	}
	
	private static void unblockedMovement(RobotController rc) throws GameActionException {
		direction = rc.getLocation().directionTo(destination);
		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		else {
			blocked = true;
			for (int attempt = 0; attempt < 8; attempt++) {
				direction = direction.rotateRight();
				if (rc.canMove(direction)) {
					break;
				}
			}
			
			if (rc.canMove(direction)) {
				rc.move(direction);
			}
		}
	}
	
	private static void blockedMovement(RobotController rc) throws GameActionException{
		MapLocation curLoc = rc.getLocation();
//		System.out.println("blocked: " + blockedAttempts);
		int spendableCrumbs = rc.getCrumbs() - rc.readSharedArray(RobotPlayer.SAVINGS);
		Direction towardsDest = curLoc.directionTo(destination);
		MapLocation fillCand = curLoc.add(towardsDest); 
		if (rc.senseMapInfo(fillCand).isWater() && 
			spendableCrumbs >= GameConstants.FILL_COST &&
			rc.canFill(fillCand)) {
			rc.fill(fillCand);
			direction = towardsDest;
		}
		if (curLoc.distanceSquaredTo(destination) < distWhenBlocked) {
			blocked = false;
			blockedAttempts = 0;
//			System.out.println("unblock");
			unblockedMovement(rc);
			return;
		}
//		if (blockedAttempts > 14) {
			
//		}
//		else {
//			blockedAttempts++;
//			System.out.println("halp");
//		}

		// check for walls on the left
		for (int attempt = 0; attempt < 8; attempt++) {
			if (! rc.canMove(direction.rotateLeft())) {
				break;
			}
			else {
				if (attempt == 7) {
					blocked = false;
					blockedAttempts = 0;
					unblockedMovement(rc);
					return;
				}
			}
		}
		
		// check for open path ahead or to the right
		for (int attempt = 0; attempt < 8; attempt++) {
			if (rc.canMove(direction)) {
				break;
			}
		}
		
		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		
		
	}
	
	/**
	 * Continuously moves robot in one direction. Upon 
	 * encountering an obstacle, will turn the robot in random direction.
	 * @throws GameActionException
	 */
	public static void wander(RobotController rc) throws GameActionException{
		if (! rc.isMovementReady()) {
			return;
		}
		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		else {
			// check at most 15 times for a valid random direction
			for (int attempt = 0; attempt < 15; attempt++) {
				if (! rc.canMove(direction)) {
					direction = randomDirection();
				}
				else {
					break;
				}
			}
			// if no valid direction has been found, systematically check all directions
			for (int index = 0; index < 8; index++) {
				if (!rc.canMove(direction)) {
					direction = direction.rotateLeft();
				}
				else {
					break;
				}
			}
		}
	}
	
	/**
	 * Searches for crumbs around the map,
	 * and avoids running into teammates.
	 * Planned to only occur during setup.
	 * @param rc
	 * @throws GameActionException
	 */
	
	public static void spreadSearchForCrumbs(RobotController rc) throws GameActionException{
		if (! rc.isMovementReady()) {
			return;
		}
		MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(10);
		MapLocation closestCrumb = closestToMe(rc, nearbyCrumbs);
//		MapLocation closestCrumb = null;
//		int distSquaredToClosest = 999;
//		MapLocation curLoc = rc.getLocation();
//		for (MapLocation cand : nearbyCrumbs) {
//			int distSquaredToCand = curLoc.distanceSquaredTo(cand);
//			if (closestCrumb == null || distSquaredToCand < distSquaredToClosest) {
//				closestCrumb = cand;
//				distSquaredToClosest = distSquaredToCand;
//			}
//		}
		if (closestCrumb != null) {
			moveTowards(rc, closestCrumb);
		}
		
//		RobotInfo[] closeTeammates = rc.senseNearbyRobots()
		else if ((rc.canMove(direction) && rng.nextInt(100) < 80)) {
			rc.move(direction);
		}
		else {
			// check at most 15 times for a valid random direction
			for (int attempt = 0; attempt < 15; attempt++) {
				if (! rc.canMove(direction)) {
					direction = randomDirection();
				}
				else {
					break;
				}
			}
			// if no valid direction has been found, systematically check all directions
			for (int index = 0; index < 8; index++) {
				if (!rc.canMove(direction)) {
					direction = direction.rotateLeft();
				}
				else {
					break;
				}
			}
		}
	}
	
	/**
	 * Circle the nearest spawnzone outside a certain radius
	 * @param r
	 * @return void
	 * @throws GameActionException 
	 */
	public static void circleSpawn(RobotController rc, int r) throws GameActionException {
		MapLocation[] spawns = rc.getAllySpawnLocations();
		MapLocation curLoc = rc.getLocation();
//		boolean isOnSpawn = false;
//		for (MapLocation spawn : spawns) {
//			if (spawn.equals(curLoc)) {
//				isOnSpawn = true;
//				break;
//			}
//		}
//		if (isOnSpawn) { 
//			
//		}
//		else {
//			
//		}
		// determine closest spawnpoint
		MapLocation closestSpawn = closestToMeExceptSelf(rc, spawns);
		int distSquaredToClosest = curLoc.distanceSquaredTo(closestSpawn);
//		MapLocation closestSpawn = null;
//		int distSquaredToClosest = 9999;
//		for (MapLocation cand : spawns) {
//			int distSquaredToCand = curLoc.distanceSquaredTo(cand);
//			if ((closestSpawn == null || distSquaredToCand < distSquaredToClosest) && distSquaredToCand != 0) {
//				closestSpawn = cand;
//				distSquaredToClosest = distSquaredToCand;
//			}
//		}
//		if (distSquaredToClosest == 999) {
//			System.out.println("this didn't work");
//			return;
//		}
		
		// if within r, move away from spawn; otherwise move tangent, counterclockwise to spawn
		Direction newDir = curLoc.directionTo(closestSpawn);
		if (distSquaredToClosest < r) {
			newDir = newDir.opposite();
		}
		else {
			newDir = newDir.rotateLeft();
		}
		moveTowards(rc, curLoc.add(newDir));
		
	}
	
	
	/**
	 * Retreats towards closest spawn
	 * @param rc
	 * @throws GameActionException
	 */
	public static void retreat(RobotController rc) throws GameActionException {
		MapLocation closestSpawn = closestToMe(rc, rc.getAllySpawnLocations());
		moveTowards(rc, closestSpawn);
	}
	
	
	
	
	/**
	 * Chooses any random direction except Direction.CENTER
	 * @return newDir
	 */
	private static Direction randomDirection() {
//		Direction[] allDirs = Direction.allDirections();
		Direction newDir = Direction.CENTER;
		while (newDir == Direction.CENTER) {
			int randomIdx = rng.nextInt(Direction.allDirections().length);
			newDir = Direction.allDirections()[randomIdx];
		}
		return newDir;
	}
	
	/**
	 * Returns the closest MapLocation within cands to the current location
	 * @param rc
	 * @param cands
	 * @return MapLocation
	 */
	
	public static MapLocation closestToMe(RobotController rc, MapLocation[] cands) {
		MapLocation curLoc = rc.getLocation();
		MapLocation closest = null;
		int distSquaredToClosest = 9999;
		for (MapLocation cand : cands) {
			int distSquaredToCand = curLoc.distanceSquaredTo(cand);
			if (closest == null || distSquaredToCand < distSquaredToClosest) {
				closest = cand;
				distSquaredToClosest = distSquaredToCand;
			}
		}
		return closest;
	}
	
	/**
	 * Returns the closest MapLocation within cands to the current location, barring any cands
	 * that are the same as the current location
	 * @param rc
	 * @param cands
	 * @return MapLocation
	 */
	
	public static MapLocation closestToMeExceptSelf(RobotController rc, MapLocation[] cands) {
		MapLocation curLoc = rc.getLocation();
		MapLocation closest = null;
		int distSquaredToClosest = 9999;
		for (MapLocation cand : cands) {
			int distSquaredToCand = curLoc.distanceSquaredTo(cand);
			if (! curLoc.equals(cand) && (closest == null || distSquaredToCand < distSquaredToClosest)) {
				closest = cand;
				distSquaredToClosest = distSquaredToCand;
			}
		}
		return closest;
	}
	
}
