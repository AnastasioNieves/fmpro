package com.tmpro.repository;

import com.tmpro.model.Match;
import com.tmpro.model.Player;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTeamIdIn(List<Long> teamIds, Sort sort);

    @Query("SELECT p FROM Match m JOIN m.squad p WHERE m.id = :matchId ORDER BY p.dorsal")
    List<Player> findSquadPlayersByMatchId(@Param("matchId") Long matchId);

    @Query("SELECT p FROM Match m JOIN m.lineup p WHERE m.id = :matchId ORDER BY p.dorsal")
    List<Player> findLineupPlayersByMatchId(@Param("matchId") Long matchId);

    @Query("SELECT COUNT(p) FROM Match m JOIN m.squad p WHERE m.id = :matchId")
    long countSquadByMatchId(@Param("matchId") Long matchId);

    @Query("SELECT COUNT(p) FROM Match m JOIN m.lineup p WHERE m.id = :matchId")
    long countLineupByMatchId(@Param("matchId") Long matchId);
}
