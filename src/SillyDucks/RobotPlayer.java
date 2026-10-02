package SillyDucks;

import battlecode.common.*;
import java.util.Random;

/**
 * The teentechiePlayer can do stuff, like spawning and wandering.
 * @author VolServe
 *
 */
public class RobotPlayer {
	// reserved indexes in shared array (64 slots)
	
	public static final int SAVINGS = 0; // indicates how many crumbs to save for digging/filling
	
	public static final int BACKUP1 = 1; // to call for backup
	public static final int BACKUP2 = 5;
	public static final int BACKUP3 = 9;
	
	public static final int NEXTTRAP = 13; // the next trap to place down defensively
	public static final int TRAPSWITCH = 14; // # of builds to wait until switching trap type 
	
	// trap values
	public static final int STUN = 0;
	public static final int BOOM = 1;
	
	// roles
	public static final int GATHERER = 0; // gathers crumbs during setup, acts as support afterwards
	public static final int TRAPPER = 1; // defense
	public static final int TRAPSUPPORT = 2;
	public static final int ATTACKER = 3; // main offense
	public static final int HEALER = 4; // main support
	
	// vars
	private static int myID;
	private static int role;
	private static MapLocation target;
	private static Random rng = new Random(6987598); 
	public static boolean hasFlag = false;
	public static boolean reachedBroadcast = false;
	public static boolean isPaving = false; // for TRAPSUPPORT
//	private static RobotInfo target = null; // for healers
//	public static int moveAttempts = 15;
	
	public static void run(RobotController rc) {
		// give each robot a unique permanent ID upon game start
		try {
			// must keep at least 200 crumbs in bank account
			rc.writeSharedArray(SAVINGS, 200);
			myID = rc.readSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1) + 1;
			rc.writeSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1, myID);
			if (myID > 0 && myID <= 25) {
				role = ATTACKER;
			}
			else if (myID > 25 && myID <= 40) {
				if (myID % 3 == 0) {
					role = rng.nextInt(1) == 0 ? TRAPPER : TRAPSUPPORT;
				}
				else {
					role = GATHERER;
				}
			}
//			else if (myID >= 35 && myID < 40) {
//				role = TRAPPER;
//			}
			else {
				role = HEALER;
			}
		}
		catch (GameActionException ex) {
			myID = 0;
//			System.out.println("hey");
			ex.printStackTrace();
		}
		
		// while loop runs **once per turn**
		// if bytecode limit exceeded, pause then resume next turn
		while (true) {
			try {
				if (rc.isSpawned()) {
					// enable status visibility of robot
//					rc.setIndicatorString(Movement.blockedAttempts + "; Destination: " + Movement.destination.x + ", " + Movement.destination.y);
//					rc.setIndicatorString((role == TRAPPER) + "");
					int roundNum = rc.getRoundNum();
					if (roundNum <= 200) {
						// builders prep phase
						if (role == GATHERER) {
							Movement.spreadSearchForCrumbs(rc);
						}
						else if (role == TRAPPER || role == TRAPSUPPORT) {
							Movement.circleSpawn(rc, 3);
							Direction[] allDirs = Direction.allDirections();
							tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
						}
						// if the robot somehow spawned and is not a builder
						else {
							Clock.yield();
						}
						
					}
					// action!
					else {
						// offensive action
						if (role == ATTACKER || role == HEALER) {
							if (hasFlag) {
								retrieveOpponentFlag(rc);
							}
							else {
								chaseOpponentFlag(rc);
								if (role == HEALER) {
									tryHeal(rc);
								}
								tryCombat(rc);
								
							}
						}
						else if (role == TRAPSUPPORT) {
							if (isPaving) {
								Movement.retreat(rc);
								MapLocation[] spawns = rc.getAllySpawnLocations();
								MapLocation curLoc = rc.getLocation();
								for (MapLocation spawn : spawns) {
									if (curLoc.equals(spawn)) {
										isPaving = false;
										break;
									}
								}
							}
							else {
								chaseOpponentFlag(rc);
							}
							Direction[] allDirs = Direction.allDirections();
							tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
						}
						else if (role == TRAPPER || role == GATHERER){
							Movement.circleSpawn(rc, 4);
							if (role == TRAPPER) {
								Direction[] allDirs = Direction.allDirections();
								tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
							}
							tryCombat(rc);
						}
					}
					
				}
				else {
					// attempt to spawn the robot
					tryToSpawn(rc);
					// set initial target
					if (rc.isSpawned()) {
						target = new MapLocation(rc.getMapWidth() - 1 - rc.getLocation().x, rc.getLocation().y);
					}
				}
				Clock.yield();
			} 
			catch (GameActionException e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	/**
	 * Tries to fight
	 * @param rc
	 * @throws GameActionException 
	 */
	private static void tryCombat(RobotController rc) throws GameActionException {
		if (rc.getHealth() > 300) {
			// attack
			RobotInfo[] nearbyOpps = rc.senseNearbyRobots(20, rc.getTeam().opponent());
			if (nearbyOpps.length == 0) {
				return;
			}
			else {
				RobotInfo weakest = null;
				int lowestHP = 99999;
				for (RobotInfo cand : nearbyOpps) {
					int candHP = cand.health;
					if (candHP < lowestHP) {
						lowestHP = candHP;
						weakest = cand;
					}
				}
				MapLocation weakestLoc = weakest.location;
				if (rc.canAttack(weakestLoc)) {
					rc.attack(weakestLoc);
				}
			}
		}
		else {
			Movement.retreat(rc);
		}
	}
	
	/**
	 * Attempts to spawn the robot within a spawn zone
	 * @param rc
	 */
	private static void tryToSpawn(RobotController rc) throws GameActionException{
		// checks if already spawned
		if (rc.isSpawned()) {
			return;
		}
		// attackers and healers don't spawn until the dam falls
		if ((role == ATTACKER || role == HEALER) && rc.getRoundNum() < 200) {
			return;
		}
		MapLocation[] spawnLocations = rc.getAllySpawnLocations();
//		if (myID != 1) {
//			return;
//		}
		for (MapLocation loc : spawnLocations) {
			if (rc.canSpawn(loc) ) {
				rc.spawn(loc);
				break;
			}
		}
	}
	
	/**
	 * Determine the closest flag broadcast location
	 * and move towards that area
	 * @param rc
	 */
	public static void chaseOpponentFlag(RobotController rc) throws GameActionException {
		FlagInfo[] nearbyFlags = rc.senseNearbyFlags(21, rc.getTeam().opponent());
		if (nearbyFlags.length != 0) {
			FlagInfo priority = null;
			MapLocation curLoc = rc.getLocation();
			int distSquaredToPriority = 9999;
			for (FlagInfo cand : nearbyFlags) {
				if (cand == null) {
					priority = cand;
				}
				if (cand.isPickedUp()) {
					priority = cand;
					if (role == TRAPSUPPORT) {
						isPaving = true;
					}
					break;
				}
				else if (! cand.isPickedUp()){
					int distSquaredToCand = curLoc.distanceSquaredTo(cand.getLocation());
					if (distSquaredToCand < distSquaredToPriority) {
						distSquaredToPriority = distSquaredToCand;
						priority = cand;
					}
				}
			}
			MapLocation priorityLoc = priority.getLocation();
			if (rc.canPickupFlag(priorityLoc)) {
				rc.pickupFlag(priorityLoc);
				retrieveOpponentFlag(rc);
				hasFlag = true;
				reachedBroadcast = false;
			}
			Movement.moveTowards(rc, priority.getLocation());
		}
		else {
			MapLocation[] approxFlagLocs = rc.senseBroadcastFlagLocations();
			MapLocation closestApprox = Movement.closestToMe(rc, approxFlagLocs);
			if (rc.getLocation().equals(closestApprox)) {
				reachedBroadcast = true;
			}
			if (reachedBroadcast) {
				Movement.wander(rc);
			}
			else {
				Movement.moveTowards(rc, closestApprox);
			}
			
		}
		
	}
	
	/**
	 * If holding the flag, ensures
	 * @param rc
	 * @throws GameActionException 
	 */
	public static void retrieveOpponentFlag(RobotController rc) throws GameActionException {
		MapLocation closestSpawn = Movement.closestToMe(rc, rc.getAllySpawnLocations());
		MapLocation curLoc = rc.getLocation();
		if (closestSpawn.equals(curLoc) && rc.canDropFlag(closestSpawn)) {
			rc.dropFlag(curLoc);
		}
		else {
			Movement.moveTowards(rc, closestSpawn);
		}
		
	}
	
	/**
	 * Attempts to place an explosive or stun trap
	 * @param rc, dir
	 */
	private static void tryToTrap(RobotController rc, Direction dir) throws GameActionException {
		int nextTrap = rc.readSharedArray(NEXTTRAP);
		boolean isBoom = nextTrap == BOOM;
		TrapType type = isBoom ? TrapType.EXPLOSIVE : TrapType.STUN; 
		int spendableCrumbs = rc.getCrumbs() - rc.readSharedArray(SAVINGS);
		// 15% chance to try to build in an available spot if can afford to build 
		if (rng.nextInt(100) < 15 && spendableCrumbs >= type.buildCost) {
//			Direction[] allDirs = Direction.allDirections();
//			Direction trapDir = null;
//			while (trapDir != null && trapDir != Direction.CENTER)) {
//				trapDir = allDirs[rng.nextInt(allDirs.length)];
//			}
			MapLocation trapLoc = rc.getLocation().add(dir);
			if (rc.canBuild(type, trapLoc)) {
				rc.build(type, trapLoc);
				int trapCooldown = rc.readSharedArray(TRAPSWITCH);
				if (trapCooldown == 0) {
					rc.writeSharedArray(NEXTTRAP, isBoom ? STUN : BOOM);
					rc.writeSharedArray(TRAPSWITCH, isBoom ? 4 : 1);
				}
				else {
					rc.writeSharedArray(TRAPSWITCH, --trapCooldown);
				}
			}
		}
		// do nothing otherwise
		else {
			return;
		}
	}
	
	/**
	 * For Healers: tries to heal the nearest wounded teammate. Will follow 
	 * the nearest wounded robot if it cannot heal anybody. 
	 * @throws GameActionException 
	 */
	private static void tryHeal(RobotController rc) throws GameActionException {
		MapLocation curLoc = rc.getLocation();
		Direction[] allDirs = Direction.allDirections(); 
		for (Direction cand : allDirs) {
			MapLocation candLoc = curLoc.add(cand); 
			if (rc.canHeal(candLoc)) {
				rc.heal(candLoc);
//				System.out.println("healed!");
			}
		}
		RobotInfo[] nearbyTeammates = rc.senseNearbyRobots(2, rc.getTeam());
	}
}
