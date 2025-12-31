/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities.serialization;

import com.google.gson.annotations.SerializedName;

public class TypeData {
    public enum Action {
        @SerializedName("i")
        INVALID,

        @SerializedName("p")
        PRESS,

        @SerializedName("r")
        RELEASE,

        @SerializedName("t")
        TYPE
    }

    private final Action a;
    private final int k;
    private final int m;
    private final int s;

    public TypeData() {
        a = Action.INVALID;
        k = 0;
        m = 0;
        s = 0;
    }

    public TypeData(Action action, int code, int modifier, int scan) {
        a = action;
        k = code;
        m = modifier;
        s = scan;
    }

    public Action getAction() {
        return a;
    }

    public int getKeyCode() {
        return k;
    }

    public int getModifier() {
        return m;
    }

    public int getScanCode() {
        return s;
    }
}
