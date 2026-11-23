package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.Submission;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import io.github.liamtkehoe.nhl_pickem.repository.UserRepository;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardLeader;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final PickRepository picks;
    private final SeasonRepository seasons;
    private final SnapshotRepository snapshots;
    private final PickValidator validator;
    private final AwardLeaderService awardLeaders;
    private final Clock clock;
    private final int season;

    public AdminService(UserRepository users, PickRepository picks, SeasonRepository seasons, SnapshotRepository snapshots, PickValidator validator, AwardLeaderService awardLeaders, Clock clock, @Value("${pickem.current-season}") int season) {
        this.users = users;
        this.picks = picks;
        this.seasons = seasons;
        this.snapshots = snapshots;
        this.validator = validator;
        this.awardLeaders = awardLeaders;
        this.clock = clock;
        this.season = season;
    }

    public record UserSummary(long id, String username, Instant createdAt, Instant submittedAt) {}

    public record AwardStatus(Award award, String displayName, AwardLeader official, AwardLeader projected) {}

    public List<UserSummary> users() {
        Map<Long, Instant> submitted = picks.submissions(season).stream()
                .collect(Collectors.toMap(Submission::userId, Submission::submittedAt));

        return users.findAll().stream()
                .map(u -> new UserSummary(u.id(), u.username(), u.createdAt(), submitted.get(u.id())))
                .toList();
    }

    @Transactional
    public void deleteUser(String username, String requestedBy) {
        requireNotFinalized();
        User user = find(username);
        if (user.username().equalsIgnoreCase(requestedBy.trim())) {
            throw new InvalidRequestException(List.of("You can't delete the account you're logged in with"));
        }
        picks.deleteAllFor(user.id());
        users.delete(user.id());
    }

    public PickSet picksOf(String username) {
        User user = findSubmitted(username);
        return picks.find(user.id(), season);
    }

    @Transactional
    public PickSet replacePicks(String username, SubmitPicksRequest request) {
        requireNotFinalized();
        User user = findSubmitted(username);
        Map<Long, PlayerInfo> players = snapshots.players(season).stream()
                .collect(Collectors.toMap(PlayerInfo::playerId, Function.identity()));
        PickSet pickSet = validator.validate(request, snapshots.teams(season), players);
        picks.replacePicks(user.id(), season, pickSet);

        return picks.find(user.id(), season);
    }

    public List<AwardStatus> awardStatus() {
        Map<Award, AwardLeader> official = awardLeaders.officialWinners(season);
        Map<Award, AwardLeader> projected = awardLeaders.projectedLeaders(season);

        return Arrays.stream(Award.values())
                .map(a -> new AwardStatus(a, a.displayName(), official.get(a), projected.get(a)))
                .toList();
    }

    public List<AwardStatus> recordWinner(AwardLeaderService.WinnerRequest request) {
        requireNotFinalized();
        awardLeaders.recordWinner(season, request);

        return awardStatus();
    }

    public boolean isFinalized() {
        return seasons.finalizedAt(season).isPresent();
    }

    @Transactional
    public Instant finalizeSeason(String requestedBy) {
        if (isFinalized()) {
            throw new ConflictException("The season is already finalized");
        }
        Map<Award, AwardLeader> official = awardLeaders.officialWinners(season);
        List<String> missing = Arrays.stream(Award.values())
                .filter(a -> !official.containsKey(a))
                .map(a -> "Record a winner for the " + a.displayName())
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(missing);
        }
        Instant now = clock.instant();
        seasons.markFinalized(season, now, requestedBy.trim());

        return now;
    }

    public void unfinalizeSeason() {
        if (!isFinalized()) {
            throw new ConflictException("The season isn't finalized");
        }
        seasons.clearFinalized(season);
    }

    private void requireNotFinalized() {
        if (isFinalized()) {
            throw new ConflictException("The season is finalized. Un-finalize it first to make changes.");
        }
    }

    private User find(String username) {
        return users.findByUsername(username.trim())
                .orElseThrow(() -> new NotFoundException("No player named " + username));
    }

    private User findSubmitted(String username) {
        User user = find(username);
        if (picks.submittedAt(user.id(), season).isEmpty()) {
            throw new NotFoundException(user.username() + " hasn't submitted picks yet");
        }

        return user;
    }

    public List<AwardStatus> clearWinner(Award award) {
        requireNotFinalized();
        awardLeaders.clearWinner(season, award);

        return awardStatus();
    }
}
