package io.github.liamtkehoe.nhl_pickem.web;

import static io.github.liamtkehoe.nhl_pickem.web.PicksController.USER_HEADER;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardFormulas;
import io.github.liamtkehoe.nhl_pickem.service.AdminService;
import io.github.liamtkehoe.nhl_pickem.service.AwardLeaderService;
import io.github.liamtkehoe.nhl_pickem.service.OULinesImport;
import io.github.liamtkehoe.nhl_pickem.service.OULinesService;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final OULinesService lines;
    private final AwardLeaderService awardLeaders;
    private final int season;

    public AdminController(AdminService admin, OULinesService lines, AwardLeaderService awardLeaders, @Value("${pickem.current-season}") int season) {
        this.admin = admin;
        this.lines = lines;
        this.awardLeaders = awardLeaders;
        this.season = season;
    }

    public record ImportResult(int lines) {}
    public record DeleteResult(String deleted) {}
    public record FinalizeResult(Instant finalizedAt) {}

    @GetMapping("/users")
    public List<AdminService.UserSummary> users() {
        return admin.users();
    }

    @DeleteMapping("/users/{username}")
    public DeleteResult deleteUser(@PathVariable String username, @RequestHeader(USER_HEADER) String requestedBy) {
        admin.deleteUser(username, requestedBy);

        return new DeleteResult(username);
    }

    @GetMapping("/users/{username}/picks")
    public PickSet picks(@PathVariable String username) {
        return admin.picksOf(username);
    }

    @PutMapping("/users/{username}/picks")
    public PickSet replacePicks(@PathVariable String username, @RequestBody SubmitPicksRequest request) {
        return admin.replacePicks(username, request);
    }

    @GetMapping("/awards")
    public List<AdminService.AwardStatus> awards() {
        return admin.awardStatus();
    }

    @PutMapping("/award-winners")
    public List<AdminService.AwardStatus> recordWinner(@RequestBody AwardLeaderService.WinnerRequest request) {
        return admin.recordWinner(request);
    }

    @PostMapping("/season/finalize")
    public FinalizeResult finalizeSeason(@RequestHeader(USER_HEADER) String requestedBy) {
        return new FinalizeResult(admin.finalizeSeason(requestedBy));
    }

    @PostMapping("/season/unfinalize")
    public FinalizeResult unfinalizeSeason() {
        admin.unfinalizeSeason();
        return new FinalizeResult(null);
    }

    @PutMapping("/lines")
    public ImportResult importLines(@RequestBody OULinesImport request) {
        return new ImportResult(lines.importLines(request));
    }

    //e.g. GET /api/admin/award-candidates/CALDER?limit=5
    @GetMapping("/award-candidates/{award}")
    public List<AwardFormulas.Candidate> candidates(@PathVariable Award award, @RequestParam(defaultValue = "10") int limit) {
        return awardLeaders.candidates(season, award, limit);
    }

    @DeleteMapping("/award-winners/{award}")
    public List<AdminService.AwardStatus> clearWinner(@PathVariable Award award) {
        return admin.clearWinner(award);
    }
}