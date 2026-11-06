package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.repository.OULinesRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OULinesService {
    private final OULinesRepository lines;
    private final SnapshotRepository snapshots;

    public OULinesService(OULinesRepository lines, SnapshotRepository snapshots) {
        this.lines = lines;
        this.snapshots = snapshots;
    }

    public int importLines(OULinesImport request) {
        if (lines.count(request.season()) > 0) {
            throw new PickException("Lines for season " + request.season() + " are already locked and loaded");
        }

        Map<String, Double> incoming = request.lines() == null ? Map.of() : request.lines();
        Set<String> known = snapshots.teams(request.season()).stream()
                .map(TeamStanding::team)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> problems = new ArrayList<>();
        if (known.isEmpty()) {
            problems.add("No standings for season " + request.season() + " yet; run a sync first");
        }
        for (String team : new TreeSet<>(incoming.keySet())) {
            if (!known.contains(team)) {
                problems.add("Unknown team: " + team);
            }
            Double line = incoming.get(team);
            if (line == null || line <= 0) {
                problems.add(team + ": line must be a positive number");
            }
        }
        for (String team : known) {
            if (!incoming.containsKey(team)) {
                problems.add("Missing line for " + team);
            }
        }
        if (request.source() == null || request.source().isBlank()) {
            problems.add("source is required");
        }
        if (request.asOf() == null || request.asOf().isBlank()) {
            problems.add("asOf is required");
        }
        if (!problems.isEmpty()) {
            throw new InvalidRequestException(problems);
        }

        lines.saveAll(request.season(), incoming, request.source(), request.asOf());
        return incoming.size();
    }
}
