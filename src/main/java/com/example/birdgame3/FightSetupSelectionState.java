package com.example.birdgame3;

final class FightSetupSelectionState {
    private final BirdGame3.BirdType[] selectedBirds;
    private final boolean[] randomSelected;
    private final String[] selectedSkinKeys;

    FightSetupSelectionState(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.selectedBirds = new BirdGame3.BirdType[slotCount];
        this.randomSelected = new boolean[slotCount];
        this.selectedSkinKeys = new String[slotCount];
    }

    BirdGame3.BirdType[] selectedBirds() {
        return selectedBirds;
    }

    boolean[] randomSelections() {
        return randomSelected;
    }

    String[] selectedSkinKeys() {
        return selectedSkinKeys;
    }

    boolean isValidSlot(int playerIdx) {
        return playerIdx >= 0 && playerIdx < selectedBirds.length;
    }

    boolean isRandomSelected(int playerIdx) {
        return isValidSlot(playerIdx) && randomSelected[playerIdx];
    }

    BirdGame3.BirdType selectedBird(int playerIdx) {
        return isValidSlot(playerIdx) ? selectedBirds[playerIdx] : null;
    }

    String selectedSkinKey(int playerIdx) {
        return isValidSlot(playerIdx) ? selectedSkinKeys[playerIdx] : null;
    }

    void setSelectedSkinKey(int playerIdx, String skinKey) {
        if (!isValidSlot(playerIdx)) {
            return;
        }
        selectedSkinKeys[playerIdx] = skinKey;
    }

    boolean isSelectedBird(int playerIdx) {
        return isValidSlot(playerIdx) && !randomSelected[playerIdx] && selectedBirds[playerIdx] == BirdGame3.BirdType.VULTURE;
    }

    boolean hasSelection(int playerIdx) {
        return isValidSlot(playerIdx) && (randomSelected[playerIdx] || selectedBirds[playerIdx] != null);
    }

    boolean allReady(int activePlayers) {
        int checkedPlayers = Math.clamp(activePlayers, 0, selectedBirds.length);
        for (int i = 0; i < checkedPlayers; i++) {
            if (!hasSelection(i)) {
                return false;
            }
        }
        return true;
    }

    void clearSelection(int playerIdx) {
        if (!isValidSlot(playerIdx)) {
            return;
        }
        randomSelected[playerIdx] = false;
        selectedBirds[playerIdx] = null;
        selectedSkinKeys[playerIdx] = null;
    }

    void selectRandom(int playerIdx) {
        if (!isValidSlot(playerIdx)) {
            return;
        }
        randomSelected[playerIdx] = true;
        selectedBirds[playerIdx] = null;
        selectedSkinKeys[playerIdx] = null;
    }

    void selectBird(int playerIdx, BirdGame3.BirdType birdType) {
        if (!isValidSlot(playerIdx)) {
            return;
        }
        if (birdType == null) {
            clearSelection(playerIdx);
            return;
        }
        randomSelected[playerIdx] = false;
        selectedBirds[playerIdx] = birdType;
    }

    void selectBirdWithSkin(int playerIdx, BirdGame3.BirdType birdType, String skinKey) {
        selectBird(playerIdx, birdType);
        if (isValidSlot(playerIdx) && birdType != null) {
            selectedSkinKeys[playerIdx] = skinKey;
        }
    }

}
