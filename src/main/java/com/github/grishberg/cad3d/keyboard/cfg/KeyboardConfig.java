package com.github.grishberg.cad3d.keyboard.cfg;


public class KeyboardConfig {

    private final int fn;
    private final double plateZOffset;
    private final double rowCurvature;
    private final double tentingAngle;
    private final double columnCurvature;
    private final double keyswitchHeight;
    private final double keyswitchWidth;
    private final double extraWidth;
    private final double extraHeight;
    private final double plateThickness;
    private final double saProfileKeyHeight;
    private final int centerCol;
    private final int rowsCount;
    private final int columnsCount;
    private final int centerRow;
    private final double keyPlaceHolderWidth;
    private final double keyPlaceHolderDepth;
    private final double keyPlaceHolderHeight;
    private final boolean isLowProfile;
    private final KeyZAngleProvider zAngleProvider;
    private final KeyOffsetProvider columnOffsetProvider;
    private final PowerSwitcherType powerSwitcherType;
    private final boolean hasHotswap;
    private final boolean magneticWristRestHolder;

    private final double bordersOffset;

    public KeyboardConfig(
        int fn,
        double plateZOffset,
        double columnCurvature,
        double tentingAngle,
        double rowCurvature,
        double keyswitchHeight,
        double keyswitchWidth,
        double extraWidth,
        double extraHeight,
        double plateThickness,
        double saProfileKeyHeight,
        int centerCol,
        int rowsCount,
        int columnsCount,
        int centerRow,
        double keyPlaceHolderWidth,
        double keyPlaceHolderDepth,
        double keyPlaceHolderHeight,
        boolean isLowProfile,
        KeyZAngleProvider zAngleProvider,
        KeyOffsetProvider columnOffsetProvider,
        PowerSwitcherType powerSwitcherType,
        boolean hasHotswap,
        boolean magneticWristRestHolder,
        double bordersOffset
    ) {
        this.fn = fn;
        this.plateZOffset = plateZOffset;
        this.rowCurvature = columnCurvature;
        this.tentingAngle = tentingAngle;
        this.columnCurvature = rowCurvature;
        this.keyswitchHeight = keyswitchHeight;
        this.keyswitchWidth = keyswitchWidth;
        this.extraWidth = extraWidth;
        this.extraHeight = extraHeight;
        this.plateThickness = plateThickness;
        this.saProfileKeyHeight = saProfileKeyHeight;
        this.centerCol = centerCol;
        this.rowsCount = rowsCount;
        this.columnsCount = columnsCount;
        this.centerRow = centerRow;
        this.keyPlaceHolderWidth = keyPlaceHolderWidth;
        this.keyPlaceHolderDepth = keyPlaceHolderDepth;
        this.keyPlaceHolderHeight = keyPlaceHolderHeight;
        this.isLowProfile = isLowProfile;
        this.zAngleProvider = zAngleProvider;
        this.columnOffsetProvider = columnOffsetProvider;
        this.powerSwitcherType = powerSwitcherType;
        this.hasHotswap = hasHotswap;
        this.magneticWristRestHolder = magneticWristRestHolder;
        this.bordersOffset = bordersOffset;
    }

    public int getFn() {
        return fn;
    }

    public double getPlateZOffset() {
        return plateZOffset;
    }

    public double getRowCurvature() {
        return rowCurvature;
    }

    public double getTentingAngle() {
        return tentingAngle;
    }

    public double getColumnCurvature() {
        return columnCurvature;
    }

    public double getKeyswitchHeight() {
        return keyswitchHeight;
    }

    public double getKeyswitchWidth() {
        return keyswitchWidth;
    }

    public double getExtraWidth() {
        return extraWidth;
    }

    public double getExtraHeight() {
        return extraHeight;
    }

    public double getPlateThickness() {
        return plateThickness;
    }

    public double getSaProfileKeyHeight() {
        return saProfileKeyHeight;
    }

    public int getCenterCol() {
        return centerCol;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public int getColumnsCount() {
        return columnsCount;
    }

    public int getCenterRow() {
        return centerRow;
    }

    public double getKeyPlaceHolderWidth() {
        return keyPlaceHolderWidth;
    }

    public double getKeyPlaceHolderDepth() {
        return keyPlaceHolderDepth;
    }

    public double getKeyPlaceHolderHeight() {
        return keyPlaceHolderHeight;
    }

    public boolean isLowProfile() {
        return isLowProfile;
    }

    public KeyZAngleProvider getZAngleProvider() {
        return zAngleProvider;
    }

    public KeyOffsetProvider getColumnOffsetProvider() {
        return columnOffsetProvider;
    }

    public PowerSwitcherType getPowerSwitcherType() {
        return powerSwitcherType;
    }

    public boolean isHasHotswap() {
        return hasHotswap;
    }

    public boolean isMagneticWristRestHolder() {
        return magneticWristRestHolder;
    }
	
	public int getLastCol(){
		return columnsCount - 1;
	}
	
	public int getLastRow(){
		return rowsCount - 1;
	}

    public double getBordersOffset() {
        return bordersOffset;
    }
}
