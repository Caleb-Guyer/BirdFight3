package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;

interface NetworkSessionClient {
    String getLastError();

    boolean connect();

    void disconnect();

    void sendSelect(BirdType type, boolean random, String skinKey);

    void sendMapVote(MapType map, boolean random);

    void sendReady(boolean ready);

    void sendInputMask(int mask);
}
