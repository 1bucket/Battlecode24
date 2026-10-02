package pestSnack;

import battlecode.common.*;
import java.util.Random;

/**
 * 
 * Movement will handle all the move methods.
 *
 */

public strictfp class Movement {
	private static Random rng = new Random(4);

	public static Direction direction = randomDirection();
	private static MapLocation destination;
	public static boolean blocked = false;
	private static int distanceWhenBlocked = 999999;
	public static boolean isWandering = false;
	
	/**
	 * Robot moves to the given location.
	 * @throws GameActionException 
	 */

	//make variable for distance to nearest enemies using getLocation()
	//if distance to enemy < √4, canAttack = true;
	// else, canAttack = false;
	// if canAttack = true, then go to enemy and attack
	
	public static void moveTowards(RobotController rc, MapLocation location) throws GameActionException {
		if (!rc.isMovementReady() || location == null) {
			return;
		}

		if (!location.equals(destination)) {
			destination = location;
			blocked = false;
			distanceWhenBlocked = 999999;
		}

		// check if already at location or if location is valid 
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
			distanceWhenBlocked = rc.getLocation().distanceSquaredTo(destination);
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

	private static void blockedMovement (RobotController rc) throws GameActionException {
		if (rc.getLocation().distanceSquaredTo(destination) < distanceWhenBlocked) {
			blocked = false;
			unblockedMovement(rc);
			return;
		}

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
		
//		if (rc.canMove(direction)) {
//			rc.move(direction);
//		}

	}
	

	/**
	 * Moves in one direction until it encounters an obstacle, and then moves in another direction.
	 * @throws GameActionException 
	 */
	public static void wander(RobotController rc) throws GameActionException {
		if (!rc.isMovementReady()) {
			return;
		}

		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		
			// check at most 15 times for a valid, random directions,
			for (int i = 0; i < 15; i += 1) {
				if (!rc.canMove(direction)) {
					direction = randomDirection();
				}
				else {
					break;
				}
			}

			// if still no valid direction, systematically check all directions.

			for (int i = 0; i < 8; i+=1) {
				if (!rc.canMove(direction)) {
					direction = direction.rotateRight();
				}
				else {
					break;
				}
			}
		}
	
	
	/**
	 * 
	 * Randomly returns a direction for the robot to move in (a valid direction)- EXCEPT direction.center
	 * @return a direction
	 * 
	 */
	private static Direction randomDirection() {
		Direction dir = Direction.CENTER;

		while (dir == Direction.CENTER) {
			int randomIdx = rng.nextInt(Direction.allDirections().length);
			dir = Direction.allDirections() [randomIdx];
		}

		return dir;
	}
	
	public static Direction getDirection() {
		
		
		return direction;
	}
}
