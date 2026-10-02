package ineedateamname;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Random;
public class Movement {
	private static Random rng = new Random(67895);
	private static Direction direction = randomDirection();
	private static MapLocation destination;
	private static boolean blocked = false;
	private static int distanceWhenBlocked = 999999;
	//private static ArrayList<MapLocation> Steps = new ArrayList<MapLocation>();
	private static Direction turnDirection;
	private static Direction oppositeTurnDirection;
	private static int roundCounter = 0;

	public static void moveTowards(RobotController rc, MapLocation location) throws GameActionException {
		if (!rc.isMovementReady() || location == null) {
			return;
		}
		
		if (!location.equals(destination)) {
			destination = location;
			blocked = false;
			distanceWhenBlocked = 999999;
		}	
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
		rc.setIndicatorString("unblocked");
		direction = rc.getLocation().directionTo(destination);
		if (rc.canMove(direction)) {
			rc.move(direction);

		}
		else {
			blocked = true;
			//Steps.clear();
			distanceWhenBlocked = rc.getLocation().distanceSquaredTo(destination);
			for (int i=0; i<8; i+=1) {
				direction = direction.rotateRight();
				if (rc.canMove(direction)) {
					break;
				}
			}
			if (rc.canMove(direction)) {
				//Steps.add(rc.getLocation());
				rc.move(direction);
				

			}
		}
	}
	
	private static void blockedMovement(RobotController rc) throws GameActionException {
		rc.setIndicatorString("blocked");
		if (rc.getLocation().distanceSquaredTo(destination) < distanceWhenBlocked) {
			blocked = false;
			unblockedMovement(rc);

			return;
		}
		
//		if (Steps.contains(rc.getLocation())){
//			blocked = false;
//			unblockedMovement(rc);
//			return;
//		}
		
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
		for (int i = 0; i < 8; i += 1) {
			if (rc.canMove(direction)) {
				//Steps.add(rc.getLocation());
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
	public static void wander(RobotController rc) throws GameActionException {
		if (!rc.isMovementReady()) {
			return;
		}
		if (rc.canMove(direction)) {
			rc.move(direction);
		}
		else {
			for (int i = 0; i<15; i+= 1) {
				if (!rc.canMove(direction)) {
					direction = randomDirection();
				}	
				else {
					break;
				}
			}
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
	private static Direction randomDirection() {
		Direction dir = Direction.CENTER;
		
		while (dir == Direction.CENTER) {
			int randomIdx = rng.nextInt(Direction.allDirections().length);		
			dir = Direction.allDirections()[randomIdx];
		}
		
		return dir;
	}
}
