package DiamondDoves;

import battlecode.common.*;
import java.util.Random;

/**
 * All the methods associated with making the robot move
 */

public class Movement {
	private static Random rng = new Random(216);
	
	private static Direction direction = randomDirection();
	private static MapLocation destination;
	private static boolean blocked = false;
	private static int distanceWhenBlocked = 999999;
	
	/**
	 * Robot moves to the given location
	 * @throws GameActionException 
	 */
	public static void moveTowards(RobotController rc, MapLocation location) throws GameActionException {
		if (!rc.isMovementReady() || location == null) {
			return;
		}
		
		if (!location.equals(destination)) {
			destination = location;
			blocked = false;
			distanceWhenBlocked = 999999;
		}
		
		// check if already at location 
		if (rc.getLocation().equals(destination)) {
			return;
		}
		
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
			distanceWhenBlocked =  rc.getLocation().distanceSquaredTo(destination);
			
			for (int i = 0; i < 8; i += 1) {
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
	
	private static void blockedMovement(RobotController rc) throws GameActionException {
		if (rc.getLocation().distanceSquaredTo(destination) < distanceWhenBlocked) {
			blocked = false;
			unblockedMovement(rc);
			return;
		}
		// check for the wall on the left
		for (int i = 0; i < 8; i += 1) {
			if (!rc.canMove(direction.rotateLeft())) {
				break;
			}
			else {
				direction = direction.rotateLeft();
				if (i == 7) {
					blocked = false;
					unblockedMovement(rc);
					return;
				}
			}
		}
		// check for open pathway ahead or to the right
		for (int i = 0; i < 8; i += 1) {
			if (rc.canMove(direction)) {
				rc.move(direction);
				break;
			}
			else {
				direction = direction.rotateRight();
			}
		}
		
		//if (rc.canMove(direction)) {
			//rc.move(direction);
		//}
		
}
	
	
	/**
	 * Moves in one direction until it encounters and obstacle
	 * and then move in another random direction
	 * @throws GameActionException 
	 */
	public static void wander(RobotController rc) throws GameActionException {
		if (!rc.isMovementReady()) {
			return;
		}
		
		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		else {
			// check at most 15 times for a valid random direction
			for (int i = 0; i < 15; i += 1) {
				if (!rc.canMove(direction)) {
					direction = randomDirection();
				}
				else {
					break;
				}
			}
			
			// if still no valid direction, systematically check all directions
			for (int i = 0; i < 8; i+=1) {
				if (!rc.canMove(direction)) {
					direction = direction.rotateRight();
				}
				else {
					break;
				}
			}
		}
	}

	/**
	 * Returns one of the game's valid directions except Direction.CENTER
	 * @return a Direction
	 */
	private static Direction randomDirection() {
		Direction dir = Direction.CENTER;
		
		while (dir == Direction.CENTER) {
			int randomIdx = rng.nextInt(Direction.allDirections().length);
			dir = Direction.allDirections()[randomIdx];
		}
		
		return dir;
	}
}
