/*
 * So.cpp
 *
 *  Created on: Jun 7, 2017
 *      Author: imp
 */

#include "BWAPI.h"
#include "BW/BWData.h"
#include "BWAPI/GameImpl.h"
#include "So.h"
#include "org_openbw_bwapi4j_BW.h"
#include "OpenBridgeModule.h"
#include <cstdio>
#include <chrono>
#include <thread>

#ifdef _WIN32
#include <Windows.h>
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT
#endif

using namespace BWAPI;

jint *intBuf;
const int bufferSize = 5000000;

bool finished = false;

// conversion ratios
double TO_DEGREES = 180.0 / M_PI;
double fixedScale = 100.0;

JNIEnv *globalEnv;
jobject globalBW;

jclass arrayListClass;
jmethodID arrayListAdd;

jclass integerClass;
jmethodID integerNew;

jclass tilePositionClass;
jmethodID tilePositionNew;

jclass weaponTypeClass;
jclass techTypeClass;
jclass unitTypeClass;
jclass upgradeTypeClass;
jclass damageTypeClass;
jclass explosionTypeClass;
jclass raceClass;
jclass unitSizeTypeClass;
jclass orderClass;

jclass pairClass;
jmethodID pairNew;

jclass bwMapClass;
jmethodID bwMapNew;

jmethodID addRequiredUnit;

extern "C" DLLEXPORT void gameInit(BWAPI::Game* game) {
	BWAPI::BroodwarPtr = game;
}
#ifdef _WIN32
BOOL APIENTRY DllMain( HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved ) {
	switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
		break;
		case DLL_PROCESS_DETACH:
		break;
	}
	return TRUE;
}
#endif

extern "C" DLLEXPORT BWAPI::AIModule* newAIModule() {

	return new OpenBridge::OpenBridgeModule();
}

/*
 * Finds and stores references to Java classes and methods globally.
 */
void initializeJavaReferences(JNIEnv *env, jobject caller) {

	std::cout << "initializing Java references..." << std::endl;
	arrayListClass = env->FindClass("java/util/ArrayList");
	arrayListAdd = env->GetMethodID(arrayListClass, "add",
			"(Ljava/lang/Object;)Z");

	integerClass = env->FindClass("java/lang/Integer");
	integerNew = env->GetMethodID(integerClass, "<init>", "(I)V");

	tilePositionClass = env->FindClass("org/openbw/bwapi4j/TilePosition");
	tilePositionNew = env->GetMethodID(tilePositionClass, "<init>", "(II)V");

	weaponTypeClass = env->FindClass("org/openbw/bwapi4j/type/WeaponType");
	techTypeClass = env->FindClass("org/openbw/bwapi4j/type/TechType");
	unitTypeClass = env->FindClass("org/openbw/bwapi4j/type/UnitType");
	upgradeTypeClass = env->FindClass("org/openbw/bwapi4j/type/UpgradeType");
	damageTypeClass = env->FindClass("org/openbw/bwapi4j/type/DamageType");
	explosionTypeClass = env->FindClass(
			"org/openbw/bwapi4j/type/ExplosionType");
	raceClass = env->FindClass("org/openbw/bwapi4j/type/Race");
	unitSizeTypeClass = env->FindClass("org/openbw/bwapi4j/type/UnitSizeType");
	orderClass = env->FindClass("org/openbw/bwapi4j/type/Order");
	pairClass = env->FindClass("org/openbw/bwapi4j/util/Pair");
	pairNew = env->GetMethodID(pairClass, "<init>",
			"(Ljava/lang/Object;Ljava/lang/Object;)V");

	bwMapClass = env->FindClass("org/openbw/bwapi4j/BWMapImpl");

	addRequiredUnit = env->GetMethodID(unitTypeClass, "addRequiredUnit",
			"(II)V");

	std::cout << "done." << std::endl;
}

JNIEXPORT void JNICALL Java_org_openbw_bwapi4j_BW_createUnit(JNIEnv *, jobject, jint playerID, jint unitType, jint posX, jint posY) {

	Broodwar->createUnit(Broodwar->getPlayer(playerID), (UnitType) unitType, Position(posX, posY));
}

JNIEXPORT void JNICALL Java_org_openbw_bwapi4j_BW_killUnit(JNIEnv *, jobject, jint unitID) {

	Broodwar->killUnit(Broodwar->getUnit(unitID));
}

JNIEXPORT void JNICALL Java_org_openbw_bwapi4j_BW_exit(JNIEnv *, jobject) {
//	do nothing
}

JNIEXPORT void JNICALL Java_org_openbw_bwapi4j_BW_mainThread(JNIEnv *, jobject) {

	BW::sacrificeThreadForUI([]{while (!finished) std::this_thread::sleep_for(std::chrono::seconds(5));});
//	std::cout << "thread done." << std::endl;
}

JNIEXPORT void JNICALL Java_org_openbw_bwapi4j_BW_startGame(JNIEnv *env, jobject caller, jobject bw) {

	globalEnv = env;
	globalBW = bw;

	/* allocate "shared memory" */
	intBuf = new jint[bufferSize];

	initializeJavaReferences(env, caller);

	try {

		BW::GameOwner gameOwner;

		gameOwner.setPrintTextCallback(
				[](const char* str) {
					std::string s;
					while (*str) {
						char c = *str++;
						if ((unsigned)c >= 0x20 || c == 9 || c == 10 || c == 13) s += c;
					}
					printf("%s\n", s.c_str());
				});

		BW::Game game = gameOwner.getGame();
		BroodwarImpl_handle handle(game);

		do {
			handle->autoMenuManager.startGame();

			while (!handle->bwgame.gameOver()) {

				handle->update();
				handle->bwgame.nextFrame();

				if (!handle->externalModuleConnected) {
					std::cout << "No module loaded, exiting" << std::endl;
					if (env->ExceptionOccurred()) {
						env->ExceptionDescribe();
					}
					return;
				}
			}
			handle->onGameEnd();
			handle->bwgame.leaveGame();

		} while (!handle->bwgame.gameClosed()
				&& handle->autoMenuManager.autoMenuRestartGame != ""
				&& handle->autoMenuManager.autoMenuRestartGame != "OFF");

	} catch (const std::exception& e) {

		printf("Error: %s\n", e.what());
	}
	finished = true;
}

int addBulletDataToBuffer(Bullet &b, int index) {

	intBuf[index++] = b->exists() ? 1 : 0;
	intBuf[index++] = static_cast<int>(TO_DEGREES * b->getAngle());
	intBuf[index++] = b->getID();
	intBuf[index++] = b->getPlayer() == NULL ? -1 : b->getPlayer()->getID();
	intBuf[index++] = b->getPosition().x;
	intBuf[index++] = b->getPosition().y;
	intBuf[index++] = b->getRemoveTimer();
	intBuf[index++] = b->getSource() == NULL ? -1 : b->getSource()->getID();
	intBuf[index++] = b->getTarget() == NULL ? -1 : b->getTarget()->getID();
	intBuf[index++] = b->getTargetPosition().x;
	intBuf[index++] = b->getTargetPosition().y;
	intBuf[index++] = b->getType();
	intBuf[index++] = static_cast<int>(fixedScale * b->getVelocityX());
	intBuf[index++] = static_cast<int>(fixedScale * b->getVelocityY());
	intBuf[index++] = b->isVisible() ? 1 : 0;

	return index;
}

/**
* Returns the list of active bullets in the game.
*
* Each bullet takes up a fixed number of integer values. Currently: 15.
*/
JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getAllBulletsData(JNIEnv * env, jobject jObject) {

	int index = 0;
	for (Bullet bullet : Broodwar->getBullets()) {

		index = addBulletDataToBuffer(bullet, index);
	}

	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);
	return result;
}

int addUnitDataToBuffer(Unit &u, int index) {

	intBuf[index++] = u->getID();
	intBuf[index++] = u->getReplayID();
	intBuf[index++] = u->getPlayer()->getID();
	intBuf[index++] = u->getType().getID();
	intBuf[index++] = u->getPosition().x;
	intBuf[index++] = u->getPosition().y;
	intBuf[index++] = u->getTilePosition().x;
	intBuf[index++] = u->getTilePosition().y;
	intBuf[index++] = static_cast<int>(TO_DEGREES * u->getAngle());
	intBuf[index++] = static_cast<int>(fixedScale * u->getVelocityX());
	intBuf[index++] = static_cast<int>(fixedScale * u->getVelocityY());
	intBuf[index++] = u->getHitPoints();
	intBuf[index++] = u->getShields();
	intBuf[index++] = u->getEnergy();
	intBuf[index++] = u->getResources();
	intBuf[index++] = u->getResourceGroup();
	intBuf[index++] = u->getLastCommandFrame();
	intBuf[index++] = u->getLastCommand().getType().getID();
	// getLastAttackingPlayer doesn't work as documented, have to check for "None" player
	intBuf[index++] = (u->getLastAttackingPlayer() != NULL
		&& u->getLastAttackingPlayer()->getType() != PlayerTypes::None)
		? u->getLastAttackingPlayer()->getID() : -1;
	intBuf[index++] = u->getInitialType().getID();
	intBuf[index++] = u->getInitialPosition().x;
	intBuf[index++] = u->getInitialPosition().y;
	intBuf[index++] = u->getInitialTilePosition().x;
	intBuf[index++] = u->getInitialTilePosition().y;
	intBuf[index++] = u->getInitialHitPoints();
	intBuf[index++] = u->getInitialResources();
	intBuf[index++] = u->getKillCount();
	intBuf[index++] = u->getAcidSporeCount();
	intBuf[index++] = u->getInterceptorCount();
	intBuf[index++] = u->getScarabCount();
	intBuf[index++] = u->getSpiderMineCount();
	intBuf[index++] = u->getGroundWeaponCooldown();
	intBuf[index++] = u->getAirWeaponCooldown();
	intBuf[index++] = u->getSpellCooldown();
	intBuf[index++] = u->getDefenseMatrixPoints();
	intBuf[index++] = u->getDefenseMatrixTimer();
	intBuf[index++] = u->getEnsnareTimer();
	intBuf[index++] = u->getIrradiateTimer();
	intBuf[index++] = u->getLockdownTimer();
	intBuf[index++] = u->getMaelstromTimer();
	intBuf[index++] = u->getOrderTimer();
	intBuf[index++] = u->getPlagueTimer();
	intBuf[index++] = u->getRemoveTimer();
	intBuf[index++] = u->getStasisTimer();
	intBuf[index++] = u->getStimTimer();
	intBuf[index++] = u->getBuildType().getID();
	intBuf[index++] = u->getTrainingQueue().size();
	intBuf[index++] = u->getTech().getID();
	intBuf[index++] = u->getUpgrade().getID();
	intBuf[index++] = u->getRemainingBuildTime();
	intBuf[index++] = u->getRemainingTrainTime();
	intBuf[index++] = u->getRemainingResearchTime();
	intBuf[index++] = u->getRemainingUpgradeTime();
	intBuf[index++] = (u->getBuildUnit() != NULL) ? u->getBuildUnit()->getID() : -1;
	intBuf[index++] = (u->getTarget() != NULL) ? u->getTarget()->getID() : -1;
	intBuf[index++] = u->getTargetPosition().x;
	intBuf[index++] = u->getTargetPosition().y;
	intBuf[index++] = u->getOrder().getID();
	intBuf[index++] = (u->getOrderTarget() != NULL) ? u->getOrderTarget()->getID() : -1;
	intBuf[index++] = u->getSecondaryOrder().getID();
	intBuf[index++] = u->getRallyPosition().x;
	intBuf[index++] = u->getRallyPosition().y;
	intBuf[index++] = (u->getRallyUnit() != NULL) ? u->getRallyUnit()->getID() : -1;
	intBuf[index++] = (u->getAddon() != NULL) ? u->getAddon()->getID() : -1;
	intBuf[index++] = (u->getNydusExit() != NULL) ? u->getNydusExit()->getID() : -1;
	intBuf[index++] = (u->getTransport() != NULL) ? u->getTransport()->getID() : -1;
	intBuf[index++] = u->getLoadedUnits().size(); // see separate getLoadedUnits method
	intBuf[index++] = (u->getCarrier() != NULL) ? u->getCarrier()->getID() : -1;
	// see getInterceptorCount and separate getInterceptors method
	intBuf[index++] = (u->getHatchery() != NULL) ? u->getHatchery()->getID() : -1;
	intBuf[index++] = u->getLarva().size(); // see separate getLarva method
	intBuf[index++] = (u->getPowerUp() != NULL) ? u->getPowerUp()->getID() : -1;
	intBuf[index++] = u->exists() ? 1 : 0;
	intBuf[index++] = u->hasNuke() ? 1 : 0;
	intBuf[index++] = u->isAccelerating() ? 1 : 0;
	intBuf[index++] = u->isAttacking() ? 1 : 0;
	intBuf[index++] = u->isAttackFrame() ? 1 : 0;
	intBuf[index++] = u->isBeingConstructed() ? 1 : 0;
	intBuf[index++] = u->isBeingGathered() ? 1 : 0;
	intBuf[index++] = u->isBeingHealed() ? 1 : 0;
	intBuf[index++] = u->isBlind() ? 1 : 0;
	intBuf[index++] = u->isBraking() ? 1 : 0;
	intBuf[index++] = u->isBurrowed() ? 1 : 0;
	intBuf[index++] = u->isCarryingGas() ? 1 : 0;
	intBuf[index++] = u->isCarryingMinerals() ? 1 : 0;
	intBuf[index++] = u->isCloaked() ? 1 : 0;
	intBuf[index++] = u->isCompleted() ? 1 : 0;
	intBuf[index++] = u->isConstructing() ? 1 : 0;
	intBuf[index++] = u->isDefenseMatrixed() ? 1 : 0;
	intBuf[index++] = u->isDetected() ? 1 : 0;
	intBuf[index++] = u->isEnsnared() ? 1 : 0;
	intBuf[index++] = u->isFollowing() ? 1 : 0;
	intBuf[index++] = u->isGatheringGas() ? 1 : 0;
	intBuf[index++] = u->isGatheringMinerals() ? 1 : 0;
	intBuf[index++] = u->isHallucination() ? 1 : 0;
	intBuf[index++] = u->isHoldingPosition() ? 1 : 0;
	intBuf[index++] = u->isIdle() ? 1 : 0;
	intBuf[index++] = u->isInterruptible() ? 1 : 0;
	intBuf[index++] = u->isInvincible() ? 1 : 0;
	intBuf[index++] = u->isIrradiated() ? 1 : 0;
	intBuf[index++] = u->isLifted() ? 1 : 0;
	intBuf[index++] = u->isLoaded() ? 1 : 0;
	intBuf[index++] = u->isLockedDown() ? 1 : 0;
	intBuf[index++] = u->isMaelstrommed() ? 1 : 0;
	intBuf[index++] = u->isMorphing() ? 1 : 0;
	intBuf[index++] = u->isMoving() ? 1 : 0;
	intBuf[index++] = u->isParasited() ? 1 : 0;
	intBuf[index++] = u->isPatrolling() ? 1 : 0;
	intBuf[index++] = u->isPlagued() ? 1 : 0;
	intBuf[index++] = u->isRepairing() ? 1 : 0;
	intBuf[index++] = u->isSelected() ? 1 : 0;
	intBuf[index++] = u->isSieged() ? 1 : 0;
	intBuf[index++] = u->isStartingAttack() ? 1 : 0;
	intBuf[index++] = u->isStasised() ? 1 : 0;
	intBuf[index++] = u->isStimmed() ? 1 : 0;
	intBuf[index++] = u->isStuck() ? 1 : 0;
	intBuf[index++] = u->isTraining() ? 1 : 0;
	intBuf[index++] = u->isUnderAttack() ? 1 : 0;
	intBuf[index++] = u->isUnderDarkSwarm() ? 1 : 0;
	intBuf[index++] = u->isUnderDisruptionWeb() ? 1 : 0;
	intBuf[index++] = u->isUnderStorm() ? 1 : 0;
	intBuf[index++] = u->isPowered() ? 1 : 0;
	intBuf[index++] = u->isUpgrading() ? 1 : 0;
	intBuf[index++] = u->isVisible() ? 1 : 0;
	intBuf[index++] = u->isResearching() ? 1 : 0;
	intBuf[index++] = u->isFlying() ? 1 : 0;

	return index;
}

/**
* Returns the list of active units in the game.
*
* Each unit takes up a fixed number of integer values. Currently: 125
*/
JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getAllUnitsData(JNIEnv * env, jobject jObject) {

	int index = 0;
	for (Unit unit : Broodwar->getAllUnits()) {

		index = addUnitDataToBuffer(unit, index);
	}

	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);
	return result;
}

// required for the OpenBW version since player->getColor() returns ordinal value instead of 256 color value.
int convertColor(int ordinal) {

	int color;
	switch(ordinal) {
	case 0:
		color = 111;
		break;
	case 1:
		color = 165;
		break;
	case 2:
		color = 159;
		break;
	case 3:
		color = 164;
		break;
	case 4:
		color = 179;
		break;
	case 5:
		color = 19;
		break;
	case 6:
		color = 255;
		break;
	case 7:
		color = 135;
		break;
	case 8:
		color = 117;
		break;
	case 9:
		color = 128;
		break;
	case 10:
		color = 0;
		break;
	case 11:
		color = 74;
		break;
	default:
		color = 0;
		std::cout << "warning: unrecognized color ordinal value." << std::endl;
		break;
	}
	return color;
}

int addPlayerDataToBuffer(Player &player, int index) {

	intBuf[index++] = player->getID();
	intBuf[index++] = player->getRace();
	intBuf[index++] = player->getStartLocation().x;
	intBuf[index++] = player->getStartLocation().y;
	intBuf[index++] = convertColor(player->getColor());
	intBuf[index++] = player->getTextColor();
	intBuf[index++] = player->getType();
	intBuf[index++] = player->getForce()->getID();
	intBuf[index++] = player->isNeutral() ? 1 : 0;
	intBuf[index++] = player->isVictorious() ? 1 : 0;
	intBuf[index++] = player->isDefeated() ? 1 : 0;
	intBuf[index++] = player->leftGame() ? 1 : 0;
	intBuf[index++] = player->minerals();
	intBuf[index++] = player->gas();
	intBuf[index++] = player->gatheredMinerals();
	intBuf[index++] = player->gatheredGas();
	intBuf[index++] = player->repairedMinerals();
	intBuf[index++] = player->repairedGas();
	intBuf[index++] = player->refundedMinerals();
	intBuf[index++] = player->refundedGas();
	intBuf[index++] = player->spentMinerals();
	intBuf[index++] = player->spentGas();
	intBuf[index++] = player->supplyTotal();
	intBuf[index++] = player->getUnitScore();
	intBuf[index++] = player->getKillScore();
	intBuf[index++] = player->getBuildingScore();
	intBuf[index++] = player->getRazingScore();
	intBuf[index++] = player->getCustomScore();
	intBuf[index++] = player->isObserver() ? 1 : 0;
	intBuf[index++] = player->supplyUsed();
	intBuf[index++] = player->supplyTotal(Races::Zerg);
	intBuf[index++] = player->supplyTotal(Races::Terran);
	intBuf[index++] = player->supplyTotal(Races::Protoss);
	intBuf[index++] = player->supplyUsed(Races::Zerg);
	intBuf[index++] = player->supplyUsed(Races::Terran);
	intBuf[index++] = player->supplyUsed(Races::Protoss);
	intBuf[index++] = player->allUnitCount();
	intBuf[index++] = player->visibleUnitCount();
	intBuf[index++] = player->completedUnitCount();
	intBuf[index++] = player->incompleteUnitCount();
	intBuf[index++] = player->deadUnitCount();
	intBuf[index++] = player->killedUnitCount();

	return index;
}

JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getAllPlayersData(JNIEnv *env, jobject jObject) {

	int index = 0;

	for (Player player : Broodwar->getPlayers()) {

		if (player->getID() != -1) {
			index = addPlayerDataToBuffer(player, index);
		}
	}

	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);

	return result;
}

JNIEXPORT jint JNICALL Java_org_openbw_bwapi4j_BW_getClientVersion(JNIEnv *env, jobject jObj) {

	return (jint)Broodwar->getClientVersion();
}

JNIEXPORT jstring JNICALL Java_org_openbw_bwapi4j_BW_getPlayerName(JNIEnv *env, jobject jObj, jint playerID) {

	// NewStringUTF can cause issues with unusual characters like Korean symbols
	return env->NewStringUTF(Broodwar->getPlayer(playerID)->getName().c_str());
	/* alternatively, use byte array:
	std::string str = Broodwar->getPlayer(playerID)->getName();
	jbyteArray jbArray = env->NewByteArray(str.length());
	env->SetByteArrayRegion(jbArray, 0, str.length(), (jbyte*)str.c_str());

	return jbArray;
	*/
}

JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getResearchStatus(JNIEnv *env, jobject jObj, jint playerID) {

	int index = 0;
	Player p = Broodwar->getPlayer(playerID);

	for (TechType techType : TechTypes::allTechTypes()) {
		intBuf[index++] = techType.getID();
		intBuf[index++] = p->hasResearched((techType)) ? 1 : 0;
		intBuf[index++] = p->isResearching((techType)) ? 1 : 0;
	}

	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);
	return result;
}

JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getUpgradeStatus(JNIEnv *env, jobject jObj, jint playerID) {

	int index = 0;
	Player p = Broodwar->getPlayer(playerID);

	for (UpgradeType upgradeType : UpgradeTypes::allUpgradeTypes()) {
		intBuf[index++] = upgradeType.getID();
		intBuf[index++] = p->getUpgradeLevel((upgradeType));
		intBuf[index++] = p->isUpgrading((upgradeType)) ? 1 : 0;
	}

	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);
	return result;
}

JNIEXPORT jintArray JNICALL Java_org_openbw_bwapi4j_BW_getGameData(JNIEnv *env, jobject jObj) {

	int index = 0;
	intBuf[index++] = Broodwar->getLastError();
	intBuf[index++] = Broodwar->getScreenPosition().x;
	intBuf[index++] = Broodwar->getScreenPosition().y;
	intBuf[index++] = Broodwar->getMousePosition().x;
	intBuf[index++] = Broodwar->getMousePosition().y;
	intBuf[index++] = Broodwar->getFrameCount();
	intBuf[index++] = Broodwar->getFPS();
	intBuf[index++] = Broodwar->isLatComEnabled() ? 1 : 0;
	intBuf[index++] = Broodwar->getRemainingLatencyFrames();
	intBuf[index++] = Broodwar->getLatencyFrames();
	intBuf[index++] = Broodwar->getLatency();

	if (Broodwar->isReplay()) {

		for (Player player : Broodwar->getPlayers()) {

			intBuf[index++] = player->getID();
		}
	} else {
		intBuf[index++] = Broodwar->self()->getID();
		intBuf[index++] = Broodwar->enemy()->getID();
	}
	jintArray result = env->NewIntArray(index);
	env->SetIntArrayRegion(result, 0, index, intBuf);

	return result;
}
