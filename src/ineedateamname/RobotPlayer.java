package ineedateamname ;

import battlecode.common.*;
import battlecode.instrumenter.inject.System;
import java.util.Random;

import javax.swing.JList.DropLocation;

import TeentechPlayer.Movement;

public strictfp class RobotPlayer {
	private static int myID;
	private static MapLocation target=new MapLocation(0,0);
	private static Random rng = new Random(112);
	private static MapLocation nearbyFriendlyFlagLocation;
	private static int nearbyFriendlyFlagIndex;
	private static MapLocation FriendlyFlagLocation2;
	private static MapLocation FriendlyFlagLocation3;
	private static MapLocation targetFlag;
	private static int numberSpawning = 50;
	private static int roundWhenGotFlag = 9999;
	public static void run(RobotController rc) {

		//System.out.println("hi");

		try {
			myID = rc.readSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1) + 1;
			rc.writeSharedArray(GameConstants.SHARED_ARRAY_LENGTH - 1, myID);
		} catch (GameActionException e) {
			myID = 0;
		}
		while (true) {
			try {
				if (rc.isSpawned()) {
					if (rc.getRoundNum() == 2) {
						nearbyFriendlyFlagLocation = nearestFlag(rc);
						if (nearbyFriendlyFlagLocation != null && rc.getRoundNum() > 1) {
							uploadFLagLocation(rc, nearbyFriendlyFlagLocation);
							if (rc.getRoundNum() < 180 ){
								if (nearbyFriendlyFlagIndex == 0) {
									FriendlyFlagLocation2=new MapLocation(rc.readSharedArray(2),rc.readSharedArray(3));
									FriendlyFlagLocation3=new MapLocation(rc.readSharedArray(4),rc.readSharedArray(5));			
								}
								if (nearbyFriendlyFlagIndex == 2) {
									FriendlyFlagLocation2=new MapLocation(rc.readSharedArray(0),rc.readSharedArray(1));
									FriendlyFlagLocation3=new MapLocation(rc.readSharedArray(4),rc.readSharedArray(5));	
								}
								if (nearbyFriendlyFlagIndex == 4) {
									FriendlyFlagLocation2=new MapLocation(rc.readSharedArray(2),rc.readSharedArray(3));
									FriendlyFlagLocation3=new MapLocation(rc.readSharedArray(0),rc.readSharedArray(1));		
								}
							} 
						}
					}

					if (rc.getRoundNum() < 200) {
						for (int i=1; i<9; i+=1) {
							if (rc.canFill(oneBlockRadius(rc, rc.getLocation(), i))) {
								rc.fill(oneBlockRadius(rc, rc.getLocation(), i));
								break;
							}
						}
						if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 36|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 36|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 36){
							for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
								if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
									rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
									break;
								}
							}
						}
						Movement.wander(rc);
					}	
					else 
						//movement script for if no friendlies are holding flags
						if (rc.readSharedArray(10) == 0) {
							numberSpawning = 50;
							noFriendlyHasFlag(rc);
						}
					//movement script for if friendly is holding flag
						else {
							numberSpawning = 50;
							friendlyHasFlag(rc);
						}
				}
				else {
					tryToSpawn(rc);
				}
				Clock.yield();
			}
			catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void friendlyHasFlag(RobotController rc) throws GameActionException {
		rc.setIndicatorString(target.toString());
		//If holding flag, go home and update location to array
		if (rc.hasFlag()) {
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
			target = nearbyFriendlyFlagLocation;
			Movement.moveTowards(rc, target);
			rc.writeSharedArray(8, rc.getLocation().x);
			rc.writeSharedArray(9, rc.getLocation().y);
			rc.setIndicatorString(nearbyFriendlyFlagLocation.toString());
			rc.setIndicatorDot(nearbyFriendlyFlagLocation, 0, 0, 255);
		}
		else if (rc.hasFlag() == false && myID != rc.readSharedArray(10)) {
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			target = new MapLocation(rc.readSharedArray(8), rc.readSharedArray(9));
			Movement.moveTowards(rc, nearbyFriendlyFlagLocation);
			roundWhenGotFlag=rc.readSharedArray(11);
		}
		else if (rc.hasFlag() == false && myID == rc.readSharedArray(10) && rc.getHealth() > 0) {
			rc.writeSharedArray(10, 0);
			rc.writeSharedArray(8, 0);
			rc.writeSharedArray(9, 0);
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			target = rc.senseBroadcastFlagLocations()[(rc.getRoundNum()%(rc.senseBroadcastFlagLocations().length))];
			Movement.moveTowards(rc, target);
			rc.writeSharedArray(6, target.x);
			rc.writeSharedArray(7, target.y);
			rc.setIndicatorDot(nearbyFriendlyFlagLocation, 255, 0, 255);
		}
		else if (rc.hasFlag() == false && myID != rc.readSharedArray(10) && rc.readSharedArray(8) == 0 && rc.readSharedArray(9) == 0) {
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			target = new MapLocation(rc.readSharedArray(6),rc.readSharedArray(7));	
			Movement.moveTowards(rc, target);
		}
		if (rc.getRoundNum() > roundWhenGotFlag + 350);{
			rc.writeSharedArray(10, 0);
			rc.writeSharedArray(8, 0);
			rc.writeSharedArray(9, 0);
			if (rc.hasFlag()) {
				rc.dropFlag(rc.getLocation());
			}
		}
	}

	private static void noFriendlyHasFlag(RobotController rc) throws GameActionException {
		// Robot 1 tells other robots to target the flags after round number 201
		if (myID == 1) {
			leader(rc);
		} 
		normie(rc);
	}
	private static MapLocation oneBlockRadius(RobotController rc, MapLocation loc, int i) {
		MapLocation location = null;
		if (i==1) {
			location = new MapLocation (loc.x, loc.y+1);
		}
		else if (i==2) {
			location = new MapLocation (loc.x+1, loc.y+1);
		}
		else if (i==3) {
			location = new MapLocation (loc.x+1, loc.y);
		}
		else if (i==4) {
			location = new MapLocation (loc.x+1, loc.y-1);
		}
		else if (i==5) {
			location = new MapLocation (loc.x, loc.y-1);
		}
		else if (i==6) {
			location = new MapLocation (loc.x-1, loc.y-1);
		}
		else if (i==7) {
			location = new MapLocation (loc.x-1, loc.y);
		}
		else if (i==8) {
			location = new MapLocation (loc.x-1, loc.y+1);
		}
		return location;
	}
	private static void normie(RobotController rc) throws GameActionException {
		if (nearestEnemyFlag(rc) == null && !rc.hasFlag()) {
			target = new MapLocation(rc.readSharedArray(6),rc.readSharedArray(7));	
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			Movement.moveTowards(rc, target); 
			rc.setIndicatorString(target.toString());
		}
		// if a robot can pickup a flag, they pick it up and head home. turn ends
		else if (rc.canPickupFlag(nearestEnemyFlag(rc))) {
			rc.writeSharedArray(8, rc.getLocation().x);
			rc.writeSharedArray(9, rc.getLocation().y);
			rc.writeSharedArray(10, myID);
			roundWhenGotFlag = rc.getRoundNum();
			rc.writeSharedArray(11, roundWhenGotFlag);
			rc.pickupFlag(nearestFlag(rc));
			target = nearbyFriendlyFlagLocation;
			rc.setIndicatorString(target.toString());
			Movement.moveTowards(rc, target);
		}
		// if a robot can pickup a flag, they pick it up and head home. turn ends
		else if (rc.hasFlag()) {
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			target = nearbyFriendlyFlagLocation;
			Movement.moveTowards(rc, target);
			rc.setIndicatorDot(rc.getLocation(), 128, 255, 0);
			rc.writeSharedArray(8, rc.getLocation().x);
			rc.writeSharedArray(9, rc.getLocation().y);
			rc.writeSharedArray(10, myID);
			rc.setIndicatorString(target.toString());
		}
		// if a robot can see an enemy flag and are not holding one they go to it
		else if (nearestEnemyFlag(rc) != null && rc.hasFlag() == false) {
			target = nearestEnemyFlag(rc);
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			Movement.moveTowards(rc, target);
		}
		//if a robot is within sqrt(25) of the target location robot 1 said, they wander
		else if ((rc.getLocation().x-target.x)*(rc.getLocation().x-target.x)+(rc.getLocation().y-target.y)*(rc.getLocation().y-target.y) < 25 && nearestEnemyFlag(rc) == null && rc.hasFlag() == false) {
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
				if (rc.canFill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
					rc.fill(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
					break;
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canHeal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					if (rc.senseRobotAtLocation(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]).getHealth() < 211) {
						rc.heal(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
						break;
					}
				}
			}
			for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4).length; i+=1) {
				if (rc.canAttack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i])) {
					rc.attack(rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)[i]);
					break;
				}
			}
			if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 16|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 16){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.EXPLOSIVE, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			else if (rc.getLocation().distanceSquaredTo(nearbyFriendlyFlagLocation) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation2) < 25|| rc.getLocation().distanceSquaredTo(FriendlyFlagLocation3) < 25){
				for (int i=1; i<rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2).length; i+=1) {
					if (rc.canBuild(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i])) {
						rc.build(TrapType.STUN, rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2)[i]);
						break;
					}
				}
			}
			Movement.wander(rc);
		}
	}

	private static void leader(RobotController rc) throws GameActionException {
		if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
			rc.buyGlobal(GlobalUpgrade.ACTION);
		}
		else if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
			rc.buyGlobal(GlobalUpgrade.HEALING);
		} 
		else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
			rc.buyGlobal(GlobalUpgrade.CAPTURING);
		} 
		if (rc.getRoundNum()< 202) {
			target = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
			rc.writeSharedArray(6, target.x);
			rc.writeSharedArray(7, target.y);
			rc.setIndicatorString(target.toString());
		}
		else {
			if (rc.getRoundNum()%100==2) {
				target = nearestEnemyFlag(rc);
				if (target == null) {
					target = rc.senseBroadcastFlagLocations()[(rc.getRoundNum()%(rc.senseBroadcastFlagLocations().length))];	
					
				}
				rc.writeSharedArray(6, target.x);
				rc.writeSharedArray(7, target.y);
				rc.setIndicatorString(target.toString());
			}
		}
	}

	private static void tryToSpawn(RobotController rc) throws GameActionException {
		if (rc.isSpawned() || myID > numberSpawning) {
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
	private static MapLocation nearestFlag(RobotController rc) throws GameActionException {
		MapLocation loc=null;
		int nearestDistance = 999999;
		for (FlagInfo flag: rc.senseNearbyFlags(-1)) {
			if (rc.getLocation().distanceSquaredTo(flag.getLocation()) < nearestDistance){
				loc = flag.getLocation();
				nearestDistance=loc.distanceSquaredTo(rc.getLocation());
			}
		}

		//System.out.println("hi");

		return loc;
	}
	private static MapLocation nearestEnemyFlag(RobotController rc) throws GameActionException {
		MapLocation loc=null;
		int nearestDistance = 999999;
		for (FlagInfo flag: rc.senseNearbyFlags(-1, rc.getTeam().opponent())) {
			if (rc.getLocation().distanceSquaredTo(flag.getLocation()) < nearestDistance){
				loc = flag.getLocation();
				nearestDistance=loc.distanceSquaredTo(rc.getLocation());
			}
		}

		//System.out.println("hi");

		return loc;
	}
	private static void uploadFLagLocation(RobotController rc, MapLocation loc) throws GameActionException {
		for (int i = 0; i < 6; i += 2) {
			if (rc.readSharedArray(i) == 0 && rc.readSharedArray(i+1) == 0) {
				rc.writeSharedArray(i, loc.x);
				rc.writeSharedArray(i+1, loc.y);
				nearbyFriendlyFlagIndex = i;
			}
		}
	}
}
