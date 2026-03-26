package com.example.birdgame3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class LanState {
    int matchTimer;
    boolean matchEnded;
    int activePlayers;
    double camX;
    double camY;
    double zoom;
    double shakeIntensity;
    int hitstopFrames;
    int[] scores = new int[4];
    List<String> killFeed = new ArrayList<>();
    LanBirdState[] birds = new LanBirdState[4];
    List<PowerUpState> powerUps = new ArrayList<>();
    List<NectarNodeState> nectarNodes = new ArrayList<>();
    List<SwingingVineState> swingingVines = new ArrayList<>();
    List<WindVentState> windVents = new ArrayList<>();
    List<CrowMinionState> crowMinions = new ArrayList<>();
    List<ChickMinionState> chickMinions = new ArrayList<>();

    void write(DataOutputStream out) throws IOException {
        out.writeInt(matchTimer);
        out.writeBoolean(matchEnded);
        out.writeInt(activePlayers);
        out.writeDouble(camX);
        out.writeDouble(camY);
        out.writeDouble(zoom);
        out.writeDouble(shakeIntensity);
        out.writeInt(hitstopFrames);
        out.writeInt(scores.length);
        for (int score : scores) {
            out.writeInt(score);
        }
        out.writeInt(killFeed.size());
        for (String msg : killFeed) {
            out.writeUTF(msg == null ? "" : msg);
        }
        out.writeInt(birds.length);
        for (LanBirdState bird : birds) {
            out.writeBoolean(bird != null);
            if (bird != null) {
                bird.write(out);
            }
        }
        out.writeInt(powerUps.size());
        for (PowerUpState p : powerUps) {
            out.writeDouble(p.x);
            out.writeDouble(p.y);
            out.writeInt(p.typeOrdinal);
        }
        out.writeInt(nectarNodes.size());
        for (NectarNodeState n : nectarNodes) {
            out.writeDouble(n.x);
            out.writeDouble(n.y);
            out.writeBoolean(n.isSpeed);
            out.writeBoolean(n.active);
        }
        out.writeInt(swingingVines.size());
        for (SwingingVineState v : swingingVines) {
            out.writeDouble(v.baseX);
            out.writeDouble(v.baseY);
            out.writeDouble(v.length);
            out.writeDouble(v.angle);
            out.writeDouble(v.angularVelocity);
        }
        out.writeInt(windVents.size());
        for (WindVentState v : windVents) {
            out.writeDouble(v.x);
            out.writeDouble(v.y);
            out.writeDouble(v.w);
            out.writeInt(v.cooldown);
        }
        out.writeInt(crowMinions.size());
        for (CrowMinionState c : crowMinions) {
            out.writeDouble(c.x);
            out.writeDouble(c.y);
            out.writeInt(c.age);
            out.writeInt(c.ownerIndex);
            out.writeBoolean(c.hasCrown);
            out.writeInt(c.variant);
        }
        out.writeInt(chickMinions.size());
        for (ChickMinionState c : chickMinions) {
            out.writeDouble(c.x);
            out.writeDouble(c.y);
            out.writeDouble(c.vx);
            out.writeInt(c.age);
            out.writeInt(c.ownerIndex);
            out.writeInt(c.variant);
            out.writeInt(c.life);
            out.writeBoolean(c.ultimate);
        }
    }

    static LanState read(DataInputStream in) throws IOException {
        LanState state = new LanState();
        state.matchTimer = in.readInt();
        state.matchEnded = in.readBoolean();
        state.activePlayers = in.readInt();
        state.camX = in.readDouble();
        state.camY = in.readDouble();
        state.zoom = in.readDouble();
        state.shakeIntensity = in.readDouble();
        state.hitstopFrames = in.readInt();
        int scoreCount = in.readInt();
        for (int i = 0; i < Math.min(scoreCount, state.scores.length); i++) {
            state.scores[i] = in.readInt();
        }
        for (int i = state.scores.length; i < scoreCount; i++) {
            in.readInt();
        }
        int feedCount = in.readInt();
        for (int i = 0; i < feedCount; i++) {
            state.killFeed.add(in.readUTF());
        }
        int birdCount = in.readInt();
        for (int i = 0; i < Math.min(birdCount, state.birds.length); i++) {
            boolean present = in.readBoolean();
            if (present) {
                state.birds[i] = LanBirdState.read(in);
            }
        }
        for (int i = state.birds.length; i < birdCount; i++) {
            boolean present = in.readBoolean();
            if (present) {
                LanBirdState.read(in);
            }
        }
        int powerCount = in.readInt();
        for (int i = 0; i < powerCount; i++) {
            PowerUpState p = new PowerUpState();
            p.x = in.readDouble();
            p.y = in.readDouble();
            p.typeOrdinal = in.readInt();
            state.powerUps.add(p);
        }
        int nectarCount = in.readInt();
        for (int i = 0; i < nectarCount; i++) {
            NectarNodeState n = new NectarNodeState();
            n.x = in.readDouble();
            n.y = in.readDouble();
            n.isSpeed = in.readBoolean();
            n.active = in.readBoolean();
            state.nectarNodes.add(n);
        }
        int vineCount = in.readInt();
        for (int i = 0; i < vineCount; i++) {
            SwingingVineState v = new SwingingVineState();
            v.baseX = in.readDouble();
            v.baseY = in.readDouble();
            v.length = in.readDouble();
            v.angle = in.readDouble();
            v.angularVelocity = in.readDouble();
            state.swingingVines.add(v);
        }
        int ventCount = in.readInt();
        for (int i = 0; i < ventCount; i++) {
            WindVentState v = new WindVentState();
            v.x = in.readDouble();
            v.y = in.readDouble();
            v.w = in.readDouble();
            v.cooldown = in.readInt();
            state.windVents.add(v);
        }
        int crowCount = in.readInt();
        for (int i = 0; i < crowCount; i++) {
            CrowMinionState c = new CrowMinionState();
            c.x = in.readDouble();
            c.y = in.readDouble();
            c.age = in.readInt();
            c.ownerIndex = in.readInt();
            c.hasCrown = in.readBoolean();
            c.variant = in.readInt();
            state.crowMinions.add(c);
        }
        int chickCount = in.readInt();
        for (int i = 0; i < chickCount; i++) {
            ChickMinionState c = new ChickMinionState();
            c.x = in.readDouble();
            c.y = in.readDouble();
            c.vx = in.readDouble();
            c.age = in.readInt();
            c.ownerIndex = in.readInt();
            c.variant = in.readInt();
            c.life = in.readInt();
            c.ultimate = in.readBoolean();
            state.chickMinions.add(c);
        }
        return state;
    }

    static final class PowerUpState {
        double x;
        double y;
        int typeOrdinal;
    }

    static final class NectarNodeState {
        double x;
        double y;
        boolean isSpeed;
        boolean active;
    }

    static final class SwingingVineState {
        double baseX;
        double baseY;
        double length;
        double angle;
        double angularVelocity;
    }

    static final class WindVentState {
        double x;
        double y;
        double w;
        int cooldown;
    }

    static final class CrowMinionState {
        double x;
        double y;
        int age;
        int ownerIndex;
        boolean hasCrown;
        int variant;
    }

    static final class ChickMinionState {
        double x;
        double y;
        double vx;
        int age;
        int ownerIndex;
        int variant;
        int life;
        boolean ultimate;
    }
}
