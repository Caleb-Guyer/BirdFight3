package com.example.birdgame3;

record OnlineRoomInfo(String code, String roomName, String hostName, int playerCount, int maxPlayers) {
    OnlineRoomInfo {
        code = code == null ? "" : code;
        roomName = roomName == null ? "" : roomName;
        hostName = hostName == null ? "" : hostName;
        playerCount = Math.max(0, playerCount);
        maxPlayers = Math.max(1, maxPlayers);
    }
}
