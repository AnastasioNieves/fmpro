package com.tmpro.controller;

import com.tmpro.model.Match;
import com.tmpro.model.dto.LiveStatDTO;
import com.tmpro.model.dto.LiveStatsUpdateRequest;
import com.tmpro.model.dto.MatchDetailDTO;
import com.tmpro.model.dto.MatchScoreUpdateRequest;
import com.tmpro.model.dto.MatchSummaryDTO;
import com.tmpro.model.dto.PlayerIdsRequest;
import com.tmpro.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchSummaryDTO>> getAllMatches() {
        return ResponseEntity.ok(matchService.findAllMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDetailDTO> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchDetail(id));
    }

    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Match match) {
        return ResponseEntity.ok(matchService.saveMatch(match));
    }

    @PutMapping("/{id}/squad")
    public ResponseEntity<MatchDetailDTO> updateSquad(
            @PathVariable Long id,
            @RequestBody PlayerIdsRequest request
    ) {
        return ResponseEntity.ok(matchService.updateSquad(id, request.getPlayerIds()));
    }

    @PutMapping("/{id}/lineup")
    public ResponseEntity<MatchDetailDTO> updateLineup(
            @PathVariable Long id,
            @RequestBody PlayerIdsRequest request
    ) {
        return ResponseEntity.ok(matchService.updateLineup(id, request.getPlayerIds()));
    }

    @PutMapping("/{id}/live-stats")
    public ResponseEntity<List<LiveStatDTO>> updateLiveStats(
            @PathVariable Long id,
            @RequestBody LiveStatsUpdateRequest request
    ) {
        return ResponseEntity.ok(matchService.updateLiveStats(id, request.getStats()));
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<MatchDetailDTO> updateScore(
            @PathVariable Long id,
            @RequestBody MatchScoreUpdateRequest request
    ) {
        return ResponseEntity.ok(matchService.updateScore(id, request));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<MatchDetailDTO> closeMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.closeMatch(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}
