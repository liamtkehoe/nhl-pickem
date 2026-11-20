package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardLeader;
import io.github.liamtkehoe.nhl_pickem.service.AwardLeaderService;
import io.github.liamtkehoe.nhl_pickem.service.OULinesImport;
import io.github.liamtkehoe.nhl_pickem.service.OULinesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final OULinesService lines;
    private final AwardLeaderService awardLeaders;
    private final int season;

    public AdminController(OULinesService lines, AwardLeaderService awardLeaders, @Value("${pickem.current-season}") int season) {
        this.lines = lines;
        this.awardLeaders = awardLeaders;
        this.season = season;
    }

    public record ImportResult(int lines) {}

    @PutMapping("/lines")
    public ImportResult importLines(@RequestBody OULinesImport request) {
        return new ImportResult(lines.importLines(request));
    }

    @PutMapping("/award-winners")
    public Map<Award, AwardLeader> recordWinner(@RequestBody AwardLeaderService.WinnerRequest request) {
        awardLeaders.recordWinner(season, request);
        return awardLeaders.currentLeaders(season);
    }
}
