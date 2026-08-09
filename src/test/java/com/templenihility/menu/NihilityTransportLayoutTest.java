package com.templenihility.menu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NihilityTransportLayoutTest {
    @Test
    void sideButtonsDoNotCoverTheFilterGrid() {
        int filterLeft = NihilityTransportLayout.FILTER_X - 1;
        int filterRight = NihilityTransportLayout.FILTER_X
            + NihilityTransportLayout.FILTER_COLUMNS * NihilityTransportLayout.SLOT_STEP - 1;
        int leftButtonRight = NihilityTransportLayout.LEFT_BUTTON_X + NihilityTransportLayout.BUTTON_WIDTH;
        int rightButtonLeft = NihilityTransportLayout.RIGHT_BUTTON_X;

        assertTrue(leftButtonRight <= filterLeft);
        assertTrue(rightButtonLeft >= filterRight);
    }

    @Test
    void clearButtonEndsBeforeThePlayerInventory() {
        int clearBottom = NihilityTransportLayout.CLEAR_BUTTON_Y
            + NihilityTransportLayout.CLEAR_BUTTON_HEIGHT;
        assertTrue(clearBottom <= NihilityTransportLayout.PLAYER_INV_Y);
    }

    @Test
    void allControlsRemainInsideTheVanillaPanel() {
        int rightEdge = NihilityTransportLayout.RIGHT_BUTTON_X + NihilityTransportLayout.BUTTON_WIDTH;
        int bottomEdge = NihilityTransportLayout.PLAYER_INV_Y + 76;

        assertTrue(rightEdge <= NihilityTransportLayout.IMAGE_WIDTH);
        assertTrue(bottomEdge <= NihilityTransportLayout.IMAGE_HEIGHT);
    }
}
