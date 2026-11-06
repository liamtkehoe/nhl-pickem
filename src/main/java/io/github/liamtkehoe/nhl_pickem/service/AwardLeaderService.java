package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardLeader;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AwardLeaderService {
    public Map<Award, AwardLeader> currentLeaders(int seasonId) {
        return Map.of();
    }
}
