// matrix_right — порт из cad3d с реальными параметрами
// Запуск: viewer → Script Editor → F5  ИЛИ  ./gradlew :scripting:run --args="examples"

val keyPlaceConfig = KeyPlaceConfig(
    plateZOffset = 8.0,
    rowCurvature = 20.1,
    tentingAngle = 8.0,
    columnCurvature = 12.1,
    keyswitchHeight = 18.0,
    keyswitchWidth = 18.0,
    extraWidth = 2.5,
    extraHeight = 1.0,
    keyPlaceHolderWidth = 15.7,
    keyPlaceHolderDepth = 15.7,
    keyPlaceHolderHeight = 4.0,
    horizontalExtraSpace = 1.0,
    verticalExtraSpace = 1.0,
    zAngleProvider = KeyZAngleProvider(),
    columnOffsetProvider = KeyOffsetProvider(),
    plateThickness = 2.0,
    saProfileKeyHeight = 4.5,
    columnsCount = 6,
    rowsCount = 3,
    centerCol = 2,
    centerRow = 1,
    isLowProfile = true,
    isHasHotswap = false,
    keyPlaceholderType = KeyPlaceholderType.None,
)

val thumbClusterSettings = ThumbClusterSettings(
    xOffset = 0.0,
    yOffset = -50.0,
    zOffset = 37.0,
    rotateY = -45.0,
    rotateZ = 18.0,
    arcRadiusZ = 0.0,
    arcRadiusY = -80.0,
    spaceBetweenKey = 6.5,
    type = ThumbClusterMode.SingleColumn3Buttons,
)

val cfg = KeyboardConfig(
    fn = 20,
    stlFn = 60,
    powerSwitcherType = PowerSwitcherType.None,
    isMagneticWristRestHolder = false,
    bordersOffset = 4.0,
    visibleKeyboardParts = setOf(KeyboardPart.KeyMatrix),
    modifiedKeyboardParts = emptySet(),
    thumbClusterSettings = thumbClusterSettings,
    screwNutHoleDiameter = 4.0,
    screwHolderWallhickness = 1.6,
    isSkeletonMode = false,
    trackball = TrackballConfig(
        mode = TrackballMode.None,
        ballDiameter = 39.0,
        bearingDiameter = 10.0,
        controllerScrewDiameter = 3.0,
    ),
    wallsSettings = WallsSettings(),
    controllerType = ControllerType.SuperMiniNRF52840,
    innerBatteryType = BatteryType.None,
    keyPlaceConfig = keyPlaceConfig,
)

// Step 1: Key placement
val keyPlace = KeyPlace(keyPlaceConfig)

// Step 2: Thumb key placement
val thumbKeyPlace = ThumbKeyPlace(cfg)

// Step 3: Thumb borders
val thumbBorders = SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)

// Step 4: Bottom edge patcher
val bottomEdgePatcher = DefaultBottomEdgePatcher(
    WallsSettings().borderThickness,
    WallsSettings().bottomBorderHeight,
)

// Step 5: Front-right wall builder (connects matrix to thumb cluster)
val topEdgeOffsetZ = -2.0
val frontRightBuilder = SingleRow3ButtonsFrontRightToMatrixWallBuilder(
    cfg, bottomEdgePatcher, topEdgeOffsetZ,
)

// Step 6: Thumb walls
val thumbWalls = SingleColumn3ButtonsThumbWalls(
    cfg, keyPlace, thumbKeyPlace, frontRightBuilder,
)

// Step 7: Key matrix (connections + borders + placeholders)
val keyMatrix = KeyMatrix(cfg, keyPlace, thumbKeyPlace)

val connections = keyMatrix.createConnectionsModel()
val borders = keyMatrix.createBordersModel(null, thumbBorders, thumbWalls)
val placeholders = keyMatrix.createPlaceholders()

// Step 8: Union everything
placeholders.model.addModel(connections.model.addModel(borders.model))
