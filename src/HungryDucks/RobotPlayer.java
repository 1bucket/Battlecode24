package HungryDucks;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * The teentechiePlayer can do stuff, like spawning and wandering.
 * @author VolServe
 *
 */
public class RobotPlayer {
	

	public enum Role {
		GATHERER, // gathers crumbs
		TRAPPER, // defender, places traps
		TRAPSUPPORT, // defender during setup, support afterwards
		ATTACKER, // main offensive
		HEALER // offensive support
	}

	// reserved indexes in shared array (64 spots)
	
	public static final int SAVINGS = 0; // indicates how many crumbs to save for digging/filling
	public static final int NEXTTRAP = 1; // the next trap to place down defensivelY
	public static final int TRAPSWITCH = 2; // # of builds to wait until switching trap type
	
	/**
	 * Indexes reserved for broadcasting location of enemy flags
	 * OPPFLAG# is reserved for the myID of the robot carrying enemy flag
	 * OPPFLAG# + 1 and + 2 hold the x and y coords (respectively) of that robot's location
	 * OPPFLAG# + 3 holds the number of friendly robots within the robot's squad
	 * 	A friendly robot is considered to be a member of the squad if it can be
	 * 	seen by the robot carrying the enemy flag
	 */
	public static final int OPPFLAG0 = 3; // to broadcast location of enemy flag while carrying it
	public static final int OPPFLAG1 = 7;
	public static final int OPPFLAG2 = 11;
	
	/**
	 * Indexes reserved for broadcasting location of team flags that aren't located
	 * at their default locations
	 * MYFLAG#STATUS holds the safety status of the flag, indicates whether safe or stolen
	 * MYFLAG# holds ID of robot broadcasting information
	 * MYFLAG# + 1 and + 2 report the x and y coords (respectively) of their location
	 */
	public static final int MYFLAG0STATUS = 16; // to broadcast location of stolen team's flag
	public static final int MYFLAG0 = 17;
	public static final int MYFLAG1STATUS = 20;
	public static final int MYFLAG1 = 21;
	public static final int MYFLAG2STATUS = 24;
	public static final int MYFLAG2 = 25;
	// stolen flag status
	public static final int SAFE = 0;
	public static final int STOLEN = 1;
	// trap values
	public static final int STUN = 0;
	public static final int BOOM = 1;
	public static final int WATER = 2;
	
	// comically large number
	public static final int BIGNUM = 9999;
	// # of attempts to see crumbs before becoming offensive
	public static final int CRUMBSEARCHLIMIT = 30;
	// max # of offensive players allowed to surround enemy flag carrier
	public static final int SQUADLIMIT = 15;
	
	// vars
	private static int myID;
	private static Role role;
	
	public static MapLocation target;
	private static Random rng = new Random(6987598); 
	private static boolean hasFlag = false;
	private static boolean reachedBroadcast = false;
	public static boolean isPaving = false; // for TRAPSUPPORT
	private static RobotInfo[] nearbyOpps;
	private static RobotInfo[] nearbyFriends;
	private static int flagBroadcastSlot = -1;
	private static Team myTeam;
	private static boolean recovering = false; // true if chasing after stolen team flag, false otherwise
	private static int theftDetectionCooldown = 3;
	public static int failedCrumbSearch = 0;
	public static boolean cantGather = false; // only necessary for gatherers so far
//	private static RobotInfo target = null; // for healers
//	public static int moveAttempts = 15;
	
	public static void run(RobotController rc) {
		// give each robot a unique permanent ID upon game start
		try {
			// must keep at least 200 crumbs in bank account
			rc.writeSharedArray(SAVINGS, 200);
			// empty reservation for broadcasting flag locations
			rc.writeSharedArray(OPPFLAG0, BIGNUM);
			rc.writeSharedArray(OPPFLAG1, BIGNUM);
			rc.writeSharedArray(OPPFLAG2, BIGNUM);
			rc.writeSharedArray(MYFLAG0, BIGNUM);
			rc.writeSharedArray(MYFLAG1, BIGNUM);
			rc.writeSharedArray(MYFLAG2, BIGNUM);
			
			myTeam = rc.getTeam();
			myID = rc.readSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1) + 1;
			rc.writeSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1, myID);
			if (myID > 0 && myID <= 25) {
				role = Role.ATTACKER;
			}
			else if (myID > 25 && myID <= 40) {
				if (myID % 3 == 0) {
					role = rng.nextInt(1) == 0 ? Role.TRAPPER : Role.TRAPSUPPORT;
				}
				else {
					role = Role.GATHERER;
				}
			}
//			else if (myID >= 35 && myID < 40) {
//				role = TRAPPER;
//			}
			else {
				role = Role.HEALER;
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
//					rc.setIndicatorString(Movement.destination != null ? Movement.destination.x + ", " + Movement.destination.y : "nowhere");
//					rc.setIndicatorString(role + "");
					if (Movement.destination != null) {
						rc.setIndicatorString(Movement.destination.x + ", " + Movement.destination.y + "; " + role);
					}
					else {
						rc.setIndicatorString("n");
					}
//					if (role == Role.GATHERER) {
//						rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
//					}
					tryDetectTheft(rc);
					int roundNum = rc.getRoundNum();
					switch (role) {
						case ATTACKER: {
							if (roundNum < 200) {
								Movement.wander(rc);
							}
							else {
								updateNearbyRobots(rc);
								if (hasFlag) {
									retrieveOpponentFlag(rc);
								}
								else {
									if (recovering) {
										chaseTeamFlag(rc);
									}
									else {
										chaseOpponentFlag(rc);
									}
									
									if (rc.isActionReady()) {
										if (nearbyOpps.length > 0) {
											tryCombat(rc);
//											System.out.println("s");
										}
										else if (rng.nextInt(77) < 15 && nearbyFriends.length > 0) {
											tryHeal(rc);
										}
											
									}
//									else {
//										evade(rc);
//									}
									
								}
							}
							break;
						}
						case HEALER: {
							if (roundNum < 200) {
								Movement.wander(rc);
							}
							else {
								healSupport(rc);
							}
							break;
						}
						case TRAPSUPPORT: {
							if (roundNum <= 190) {
								Movement.circleSpawn(rc, (roundNum % 10) + 5);
							}
							else {
								
								if (theftDetectionCooldown <= 0) {
									theftDetectionCooldown = 3;
									detectStolenTeamFlag(rc);
								}
								else {
									theftDetectionCooldown -= 1;
								}
								if (hasFlag) {
									retrieveOpponentFlag(rc);
								}
								else if (recovering) {
									chaseTeamFlag(rc);
								}
								else {
									updateNearbyRobots(rc);
									chaseOpponentFlag(rc);
								}
								
							}
							if (rc.isActionReady()) {
								Direction[] allDirs = Direction.allDirections();
								tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
								if (rc.isActionReady()) {
									updateNearbyRobots(rc);
									tryHeal(rc);
								}
							}
							break;
						}
						case TRAPPER: {
							if (recovering) {
								chaseTeamFlag(rc);
							}
							else {
//								Movement.circleSpawn(rc, 10);
								patrol(rc);
							}
							if (rc.isActionReady()) {
								Direction[] allDirs = Direction.allDirections();
								tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
								if (rc.isActionReady()) {
									updateNearbyRobots(rc);
									tryCombat(rc);
								}
							}
							break;
						}
						case GATHERER: {
							if (hasFlag) {
								retrieveOpponentFlag(rc);
							}
							if (recovering) {
								chaseTeamFlag(rc);
							}
							else {
								MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
								if (nearbyCrumbs.length > 0) {
									cantGather = false;
									failedCrumbSearch = 0;
								}
								if (cantGather) {
//									patrol(rc);
									if (roundNum < 200) {
										patrol(rc);
										Direction[] allDirs = Direction.allDirections();
										tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
									}
									else {
										chaseOpponentFlag(rc);
										if (rc.isActionReady()) {
											if (nearbyOpps.length > 0) {
												tryCombat(rc);
											}
											else if (rng.nextInt(77) < 15 && nearbyFriends.length > 0) {
												tryHeal(rc);
											}
	
										}
									}
								}
								else {
									Movement.spreadSearchForCrumbs(rc, nearbyCrumbs);
								}
							}
							if (rc.isActionReady()) {
								updateNearbyRobots(rc);
								tryCombat(rc);
							}
							break;
						}
					}
					
					
//					if (roundNum <= 200) {
//						// builders prep phase
//						if (role == Role.GATHERER) {
//							Movement.spreadSearchForCrumbs(rc);
//						}
//						else if (role == Role.TRAPPER || role == Role.TRAPSUPPORT) {
//							Movement.circleSpawn(rc, 3);
//							Direction[] allDirs = Direction.allDirections();
//							tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
//						}
//						// if the robot somehow spawned and is not a builder
//						else {
//							Clock.yield();
//						}
//						
//					}
////					 action!
//					else {
//						// offensive action
//						if (role == Role.ATTACKER || role == Role.HEALER) {
//							if (hasFlag) {
//								retrieveOpponentFlag(rc);
//							}
//							else {
//								chaseOpponentFlag(rc);
//								if (role == Role.HEALER) {
//									tryHeal(rc);
//								}
//								tryCombat(rc);
//								
//							}
//						}
//						else if (role == Role.TRAPSUPPORT) {
//							if (isPaving) {
//								Movement.retreat(rc);
//								MapLocation[] spawns = rc.getAllySpawnLocations();
//								MapLocation curLoc = rc.getLocation();
//								for (MapLocation spawn : spawns) {
//									if (curLoc.equals(spawn)) {
//										isPaving = false;
//										break;
//									}
//								}
//							}
//							else {
//								chaseOpponentFlag(rc);
//							}
//							Direction[] allDirs = Direction.allDirections();
//							tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
//						}
//						else if (role == Role.TRAPPER || role == Role.GATHERER){
//							Movement.circleSpawn(rc, 4);
//							if (role == Role.TRAPPER) {
//								Direction[] allDirs = Direction.allDirections();
//								tryToTrap(rc, allDirs[rng.nextInt(allDirs.length)]);
//							}
//							tryCombat(rc);
//						}
//					}
					
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
	 * Wrapper for flag theft detection
	 * @throws GameActionException 
	 */
	public static void tryDetectTheft(RobotController rc) throws GameActionException {
		if (theftDetectionCooldown <= 0) {
			theftDetectionCooldown = 3;
			detectStolenTeamFlag(rc);
		}
		else {
			theftDetectionCooldown -= 1;
		}
	}
	
	/**
	 * Heal support decision algorithm
	 * @param rc
	 */
	private static void healSupport(RobotController rc) throws GameActionException {
		updateNearbyRobots(rc);
		if (hasFlag) {
			retrieveOpponentFlag(rc);
		}
		else {
			if (recovering) {
				chaseTeamFlag(rc);
			}
			else {
				chaseOpponentFlag(rc);
			}
			if (rc.isActionReady()) {
				if (nearbyFriends.length > 0) {
					tryHeal(rc);
				}
				else if (nearbyOpps.length > 0) {
					tryCombat(rc);
				}
			}
		}
	}
	
	/**
	 * Tries to fight
	 * @param rc
	 * @throws GameActionException 
	 */
	private static void tryCombat(RobotController rc) throws GameActionException {
		// attack
//		RobotInfo[] nearbyOpps = rc.senseNearbyRobots(20, rc.getTeam().opponent());
		if (nearbyOpps.length == 0) {
			return;
		}
		else {
			RobotInfo priority = null;
			int lowestHP = 99999;
			for (RobotInfo cand : nearbyOpps) {
				if (cand.hasFlag()) {
					priority = cand;
					break;
				}
				int candHP = cand.health;
				if (candHP < lowestHP) {
					lowestHP = candHP;
					priority = cand;
				}
			}
			MapLocation priorityLoc = priority.location;
			if (rc.canAttack(priorityLoc)) {
				rc.attack(priorityLoc);
			}
		}
		if (rc.getHealth() < 300) {
			evade(rc);
		}
	}
	
	/**
	 * Moves robot away from any opponent robots. If surrounded by enemy players,
	 * retreat towards home.
	 */
	private static void evade(RobotController rc) throws GameActionException {
		ArrayList<Direction> availableDirs = new ArrayList<Direction>(); 
		for (Direction dir : Direction.allDirections()) {
			availableDirs.add(dir);
		}
		MapLocation curLoc = rc.getLocation();
		for (RobotInfo oppInfo : nearbyOpps) {
			if (oppInfo.getLocation().distanceSquaredTo(curLoc) <= 2) {
				Direction oppDir = curLoc.directionTo(oppInfo.getLocation());
				availableDirs.remove(oppDir);
				availableDirs.remove(oppDir.rotateLeft());
				availableDirs.remove(oppDir.rotateRight());
			}
		}
		if (availableDirs.size() == 0) {
			return;
		}
		else {
			for (Direction evadeDir : availableDirs) {
				if (rc.canMove(evadeDir)) {
					Movement.moveTowards(rc, curLoc.add(evadeDir));
					return;
				}
			}
			
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
		if ((role == Role.ATTACKER || role == Role.HEALER) && rc.getRoundNum() < 180) {
			return;
		}
		MapLocation[] spawnLocations = rc.getAllySpawnLocations();
//		if (myID != 1) {
//			return;
//		}
		ArrayList<MapLocation> availableSpawns = new ArrayList<MapLocation>();
		for (MapLocation loc : spawnLocations) {
			availableSpawns.add(loc);
		}
		while (availableSpawns.size() > 0) {
			MapLocation loc = availableSpawns.get(rng.nextInt(availableSpawns.size()));
			if (rc.canSpawn(loc) ) {
				rc.spawn(loc);
				break;
			}
			else {
				availableSpawns.remove(loc);
			}
		}
	}
	
	/**
	 * Provides two lists of robots in the surrounding area:
	 * enemies and teammates
	 * @param rc
	 */
	private static void updateNearbyRobots(RobotController rc) {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		ArrayList<RobotInfo> nearOpps = new ArrayList<RobotInfo>();
		ArrayList<RobotInfo> nearFriends = new ArrayList<RobotInfo>();
		for (RobotInfo nearbyRob : nearbyRobots) {
			if (nearbyRob.getTeam() == myTeam) {
				nearFriends.add(nearbyRob);
			}
			else {
				nearOpps.add(nearbyRob);
			}
		}
		nearbyOpps = toArray(nearOpps);
		nearbyFriends = toArray(nearFriends);
	}
	
	/**
	 * Determine the closest flag broadcast location
	 * and move towards that area
	 * @param rc
	 * @throws GameActionException 
	 */
	public static void chaseOpponentFlag(RobotController rc) throws GameActionException  {
//		try {
			
		FlagInfo[] nearbyFlags = rc.senseNearbyFlags(21, myTeam.opponent());
		if (nearbyFlags.length != 0) {
			FlagInfo priority = null;
//			boolean priorityPickedUp = false;
			MapLocation curLoc = rc.getLocation();
			int distSquaredToPriority = 9999;
			for (FlagInfo cand : nearbyFlags) {
				if (cand == null) {
					priority = cand;
				}
				if (cand.isPickedUp()) {
					priority = cand;
//					priorityPickedUp = true;
					distSquaredToPriority = priority.getLocation().distanceSquaredTo(curLoc);
					if (role == Role.TRAPSUPPORT) {
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
			else {
				Direction dir = curLoc.directionTo(priorityLoc);
				Direction opposite = dir.opposite();
				Direction tangent = dir.rotateLeft();
				if (rc.canMove(opposite) && distSquaredToPriority < 9) {
					Movement.moveTowards(rc,  curLoc.add(opposite));
				}
				else if (rc.canMove(tangent) && distSquaredToPriority < 20) {
					Movement.moveTowards(rc,  curLoc.add(tangent));
				}
				else {
					Movement.moveTowards(rc, priorityLoc);
				}
			}
		}
		else {
			MapLocation priority = null;
			
			MapLocation closestPickedUpOppFlagLoc = null;
			MapLocation[] pickedUpOppFlags = new MapLocation[3];
//			System.out.println(rc.readSharedArray(OPPFLAG0) == BIGNUM ? rc.getID() : "");
//			System.out.println(rc.readSharedArray(OPPFLAG1) == BIGNUM ? rc.getID() : "");
//			System.out.println(rc.readSharedArray(OPPFLAG2) == BIGNUM ? rc.getID() : "");
			pickedUpOppFlags[0] = rc.readSharedArray(OPPFLAG0) != BIGNUM ? 
								  new MapLocation(rc.readSharedArray(OPPFLAG0 + 1), rc.readSharedArray(OPPFLAG0 + 2)) : 
									null;
			pickedUpOppFlags[1] = rc.readSharedArray(OPPFLAG1) != BIGNUM ? 
									new MapLocation(rc.readSharedArray(OPPFLAG1 + 1), rc.readSharedArray(OPPFLAG1 + 2)) : 
									null;
			pickedUpOppFlags[2] = rc.readSharedArray(OPPFLAG2) != BIGNUM ? 
									new MapLocation(rc.readSharedArray(OPPFLAG2 + 1), rc.readSharedArray(OPPFLAG2 + 2)) : 
										null;
			MapLocation[] cands = new MapLocation[3];
			cands[0] = rc.readSharedArray(OPPFLAG0 + 3) < SQUADLIMIT ? pickedUpOppFlags[0] : null;
			cands[1] = rc.readSharedArray(OPPFLAG1 + 3) < SQUADLIMIT ? pickedUpOppFlags[1] : null;
			cands[2] = rc.readSharedArray(OPPFLAG2 + 3) < SQUADLIMIT ? pickedUpOppFlags[2] : null;
			closestPickedUpOppFlagLoc = Movement.closestToMe(rc,  cands);
			
//			MapLocation cand = Movement.closestToMe(rc,  pickedUpOppFlags);
//			if (cand != null && rc.getLocation().distanceSquaredTo(cand) < 225) {
//				priority = cand;
//			}
			
//			if (priority == null) {
				MapLocation[] approxFlagLocs = rc.senseBroadcastFlagLocations();
				MapLocation closestApproxFlagLoc = Movement.closestToMe(rc, approxFlagLocs);
//			}
			if (closestPickedUpOppFlagLoc == null) {
				priority = closestApproxFlagLoc;
			}
			if (closestApproxFlagLoc == null) {
				priority = closestPickedUpOppFlagLoc;
			}
			if (closestPickedUpOppFlagLoc != null && closestApproxFlagLoc != null && priority == null) {
				MapLocation curLoc = rc.getLocation();
				int distSquaredToPicked = curLoc.distanceSquaredTo(closestPickedUpOppFlagLoc);
				int distSquaredToApprox = curLoc.distanceSquaredTo(closestApproxFlagLoc);
				priority = distSquaredToPicked < distSquaredToApprox ? closestPickedUpOppFlagLoc : closestApproxFlagLoc;
			}
			 
				
			if (priority == null) {
				patrol(rc);
			}
			if (priority == closestApproxFlagLoc) {
				if (rc.getLocation().equals(priority)) {
					reachedBroadcast = true;
				}
				if (reachedBroadcast) {
					Movement.wander(rc);
				}
				else {
					Movement.moveTowards(rc, priority);
				}
			}
			else if (priority == closestPickedUpOppFlagLoc) {
				Movement.moveTowards(rc, priority);
			}
		}
//		}
//		
//		catch (Exception ex) {
//			ex.printStackTrace();
//		}
		
	}
	
	/**
	 * If holding the flag, robot goes home
	 * @param rc
	 * @throws GameActionException 
	 */
	public static void retrieveOpponentFlag(RobotController rc) throws GameActionException {
		if (! hasFlag) {
			return;
		}
		MapLocation closestSpawn = Movement.closestToMe(rc, rc.getAllySpawnLocations());
		MapLocation curLoc = rc.getLocation();
//		System.out.println("what");
		if (! rc.hasFlag()) {
//			System.out.println("hello");
//			rc.dropFlag(curLoc);
			hasFlag = false;
//			System.out.println("dropped");
			updateFlagLoc(rc, myTeam.opponent(), null, null);
		}
		else {
			
			if (nearbyOpps.length > 0) {
//				evade(rc);
			}
			if (rc.isMovementReady()) {
				Movement.moveTowards(rc, closestSpawn);
			}

			updateFlagLoc(rc, myTeam.opponent(), curLoc, null);
		}
		
	}
	
	/**
	 * Attempts to place an explosive or stun trap
	 * @param rc, dir
	 */
	private static void tryToTrap(RobotController rc, Direction dir) throws GameActionException {
		if (! rc.isActionReady()) {
			return;
		}
		int nextTrap = rc.readSharedArray(NEXTTRAP);
		boolean isBoom = nextTrap == BOOM;
		TrapType type = isBoom ? TrapType.EXPLOSIVE : TrapType.STUN; 
		int spendableCrumbs = rc.getCrumbs() - rc.readSharedArray(SAVINGS);
		// 15% chance to try to build in an available spot if can afford to build 
		if (rng.nextInt(100) < 15 && spendableCrumbs >= type.buildCost) {
			MapLocation trapLoc = rc.getLocation().add(dir);
			if (rc.canBuild(type, trapLoc)) {
				rc.build(type, trapLoc);
				int trapCooldown = rc.readSharedArray(TRAPSWITCH);
				if (trapCooldown == 0) {
					rc.writeSharedArray(NEXTTRAP, isBoom ? STUN : BOOM);
					rc.writeSharedArray(TRAPSWITCH, isBoom ? 2 : 1);
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
		for (RobotInfo friend : nearbyFriends) {
			MapLocation target = friend.getLocation(); 
			if (rc.canHeal(target)) {
				rc.heal(target);
				break;
			}
		}
	}
	
	/**
	 * Broadcast a flag's location. Unclaims reserved spot if jailed or has dropped flag
	 * @param robots
	 * @return void
	 * @throws GameActionException 
	 */
	private static void updateFlagLoc(RobotController rc, Team flagOwner, MapLocation flagLoc, FlagInfo flag) throws GameActionException {

//		int BIGNUM = GameConstants.MAX_SHARED_ARRAY_VALUE;
		if (myTeam.opponent() == flagOwner && (rc.getHealth() <= 0 || !hasFlag)) {
			if (rc.canWriteSharedArray(flagBroadcastSlot, BIGNUM)) {
				rc.writeSharedArray(flagBroadcastSlot, BIGNUM);
			}
			flagBroadcastSlot = -1;
			hasFlag = false;
//			System.out.println("reset");
			return;
		}
		if (myTeam == flagOwner && (rc.getHealth() <= 0 || ! !Movement.isLocSpawn(rc, flagLoc))) {
			if (rc.canWriteSharedArray(flagBroadcastSlot, BIGNUM)) {
				rc.writeSharedArray(flagBroadcastSlot, BIGNUM);
			}
			if (rc.canWriteSharedArray(flagBroadcastSlot - 1, SAFE)) {
				rc.writeSharedArray(flagBroadcastSlot - 1, SAFE);
			}
			recovering = false;
			return;
		}
//		Team myTeam = rc.getTeam();
//		int flagBroadcastSlot = -1;
		if (flagBroadcastSlot == -1) {
			if (flagOwner == myTeam) {
				if (rc.readSharedArray(MYFLAG0) == BIGNUM || rc.readSharedArray(MYFLAG0) == myID) {
					flagBroadcastSlot = MYFLAG0;
				}
				else if (rc.readSharedArray(MYFLAG1) == myID || rc.readSharedArray(MYFLAG1) == BIGNUM) {
					flagBroadcastSlot = MYFLAG1;
				}
				else if (rc.readSharedArray(MYFLAG2) == myID || rc.readSharedArray(MYFLAG2) == BIGNUM) {
					flagBroadcastSlot = MYFLAG2;
				}
				
			}
			else {
				if (rc.readSharedArray(OPPFLAG0) == BIGNUM || rc.readSharedArray(OPPFLAG0) == myID) {
					flagBroadcastSlot = OPPFLAG0;
				}
				else if (rc.readSharedArray(OPPFLAG1) == myID || rc.readSharedArray(OPPFLAG1) == BIGNUM) {
					flagBroadcastSlot = OPPFLAG1;
				}
				else if (rc.readSharedArray(OPPFLAG2) == myID || rc.readSharedArray(OPPFLAG2) == BIGNUM) {
					flagBroadcastSlot = OPPFLAG2;
				}
			}
		}
		if (flagBroadcastSlot > -1) {
//			System.out.println(myTeam == flagOwner ? rc.canWriteSharedArray(flagBroadcastSlot, 1) : "");
			rc.writeSharedArray(flagBroadcastSlot, myID);
			rc.writeSharedArray(flagBroadcastSlot + 1, flagLoc.x);
			rc.writeSharedArray(flagBroadcastSlot + 2, flagLoc.y);
			if (flagOwner == myTeam.opponent()) {
				if (nearbyFriends != null && rc.canWriteSharedArray(flagBroadcastSlot + 3, nearbyFriends.length)) {
//					System.out.println(nearbyFriends.length);
					rc.writeSharedArray(flagBroadcastSlot + 3, nearbyFriends.length);
				}
			}
			if (flagOwner == myTeam) {
				rc.writeSharedArray(flagBroadcastSlot - 1, STOLEN);
				recovering = true;
			}
		}
	}
	
	private static RobotInfo[] toArray(ArrayList<RobotInfo> robots) {
		RobotInfo[] robs = new RobotInfo[robots.size()];
		for (int index = 0; index < robs.length; index += 1) {
			robs[index] = robots.get(index);
		}
		return robs;
	}
	
	/**
	 * Wanders around spawn zone
	 * @throws GameActionException
	 * @return void 
	 */
	
	private static void patrol(RobotController rc) throws GameActionException {
		MapLocation nearestSpawn = Movement.closestToMe(rc,  rc.getAllySpawnLocations());
		MapLocation curLoc = rc.getLocation();
		if (curLoc.distanceSquaredTo(nearestSpawn) <= 20) {
			Movement.wander(rc);
		}
		else {
			Movement.moveTowards(rc, curLoc.add(curLoc.directionTo(nearestSpawn)));
		}
	}
	
	/**
	 * Detects if a team flag has been stolen
	 * @throws GameActionException 
	 */
	private static void detectStolenTeamFlag(RobotController rc) throws GameActionException {
		FlagInfo[] nearbyTeamFlags = rc.senseNearbyFlags(-1, myTeam);
		for (FlagInfo cand : nearbyTeamFlags) {
			MapLocation candLoc = cand.getLocation();
			if (! Movement.isLocSpawn(rc, candLoc)) {
//				recovering = true;
//				System.out.println(rc.canWriteSharedArray(MYFLAGSTATUS,  12));
//				rc.writeSharedArray(MYFLAGSTATUS, STOLEN);
				updateFlagLoc(rc, myTeam, candLoc, cand);
			}
		}
	}
	/**
	 * Chase stolen team flags
	 * @throws GameActionException 
	 */
	public static void chaseTeamFlag(RobotController rc) throws GameActionException {
//		if (rc.readSharedArray(MYFLAG0STATUS) == SAFE) System.out.println("w");
//		if (rc.readSharedArray(MYFLAG1STATUS) == SAFE) System.out.println("w");
//		if (rc.readSharedArray(MYFLAG2STATUS) == SAFE) System.out.println("w");
		if (rc.readSharedArray(MYFLAG0STATUS) == SAFE ||
			rc.readSharedArray(MYFLAG1STATUS) == SAFE ||
			rc.readSharedArray(MYFLAG2STATUS) == SAFE) {
			recovering = false;
			return;
		}
		else {
			MapLocation priority = null;
			FlagInfo[] nearbyTeamFlags = rc.senseNearbyFlags(-1, myTeam);
			for (FlagInfo cand : nearbyTeamFlags) {
				if (cand.isPickedUp()) {
					priority = cand.getLocation();
					break;
				}
			}
			if (priority == null) {
				MapLocation[] stolenFlags = new MapLocation[3];
				stolenFlags[0] = rc.readSharedArray(MYFLAG0) != BIGNUM ? 
									  new MapLocation(rc.readSharedArray(MYFLAG0 + 1), rc.readSharedArray(MYFLAG0 + 2)) : 
										null;
				stolenFlags[1] = rc.readSharedArray(MYFLAG1) != BIGNUM ? 
										new MapLocation(rc.readSharedArray(MYFLAG1 + 1), rc.readSharedArray(MYFLAG1 + 2)) : 
										null;
				stolenFlags[2] = rc.readSharedArray(MYFLAG2) != BIGNUM ? 
										new MapLocation(rc.readSharedArray(MYFLAG2 + 1), rc.readSharedArray(MYFLAG2 + 2)) : 
											null;
				MapLocation cand = Movement.closestToMe(rc,  stolenFlags);
				if (cand != null && rc.getLocation().distanceSquaredTo(cand) < 225) {
					priority = cand;
				} // if the closest stolen team flag is too far, it's not worth it to chase it probably
			}
			if (priority == null) {
//				recovering = false;
				return;
			}
			else {
				recovering = true;
				Movement.moveTowards(rc,  priority);
			}
			
		}
	}
}
