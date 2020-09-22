package com.vinson.addev.serialport;

import com.socks.library.KLog;
import com.vinson.addev.model.annotation.MovementState;
import com.vinson.addev.model.request.SensorData;

import java.util.LinkedList;

public class SensorEngine {

    private static SensorEngine INSTANCE;
    LinkedList<SensorData> scope = new LinkedList<>();
    IMoveStateChange mStateChange;
    @MovementState
    private int curMoveState = MovementState.MOVEMENT_NONE;
    private float speedThreshold = 0.1f;
    private float accThreshold = 0.1f;
    private float degThreshold = 0.1f;

    public static SensorEngine getInstance() {
        if (INSTANCE == null)
            return new SensorEngine();
        else return INSTANCE;
    }

    public void setStateChange(IMoveStateChange stateChange) {
        mStateChange = stateChange;
    }

    public void newData(SensorData data) {
        if (scope.size() >= 10) {
            scope.removeLast();
        }
        scope.addFirst(data);
        if (scope.size() >= 10) {
            // apply algorithm
            calMoveState();
        } else KLog.d("sensor data not enough!");
    }

    public void calMoveState() {
        LinkedList<Float> speeds = new LinkedList<>();
        for (SensorData data : scope) {
            speeds.addLast(data.speedz);
        }
        float speedSum = 0;
        for (Float speed : speeds) speedSum += speed;

        // static state
        if (speedSum / speeds.size() == 0) {
            if (mStateChange != null && curMoveState != MovementState.MOVEMENT_STATIC)
                mStateChange.onMoveStateChange(curMoveState, MovementState.MOVEMENT_STATIC);
            curMoveState = MovementState.MOVEMENT_STATIC;
            return;
        }

        boolean accelerate = true;
        for (int i = 9; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (speeds.get(j) < speeds.get(i)) {
                    accelerate = false;
                    break;
                }
            }
            if (!accelerate) break;
        }

        // accelerate state
        if (accelerate) {
            if (mStateChange != null && curMoveState != MovementState.MOVEMENT_ACCELERATE)
                mStateChange.onMoveStateChange(curMoveState, MovementState.MOVEMENT_ACCELERATE);
            curMoveState = MovementState.MOVEMENT_ACCELERATE;
            return;
        } else {
            boolean decelerate = true;
            for (int m = 9; m >= 0; m--) {
                for (int n = m - 1; n >= 0; n--) {
                    if (speeds.get(n) > speeds.get(m)) {
                        decelerate = false;
                        break;
                    }
                }
                if (!decelerate) break;
            }
            // decelerate state
            if (decelerate) {
                if (mStateChange != null && curMoveState != MovementState.MOVEMENT_DECELERATE)
                    mStateChange.onMoveStateChange(curMoveState, MovementState.MOVEMENT_DECELERATE);
                curMoveState = MovementState.MOVEMENT_DECELERATE;
                return;
            }
        }

        boolean stable = true;
        for (int i = 9; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (speeds.get(j) != speeds.get(i)) {
                    stable = false;
                    break;
                }
            }
            if (!stable) break;
        }
        // stable state
        if (stable) {
            if (mStateChange != null && curMoveState != MovementState.MOVEMENT_STABLE)
                mStateChange.onMoveStateChange(curMoveState, MovementState.MOVEMENT_STABLE);
            curMoveState = MovementState.MOVEMENT_STABLE;
        }
    }

    public interface IMoveStateChange {
        void onMoveStateChange(@MovementState int oldState, @MovementState int newState);
    }


}
