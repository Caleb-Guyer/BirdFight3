package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

interface NetworkSessionHost {
    boolean start();

    void stop();

    boolean hasClients();

    void broadcastLobby(MapType map, boolean mapRandom, boolean[] connected, BirdType[] birds,
                        boolean[] randomBirds, String[] skinKeys, boolean[] ready);

    void broadcastStart(MapType map, long seed, boolean[] connected, BirdType[] birds, String[] skinKeys);

    void broadcastState(LanState state);

    void broadcastMatchEnd(int winnerIndex);

    void broadcastCountdown(int seconds);

    void broadcastResultsAction(int action, int delayMs);
}
