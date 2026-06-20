package com.tmpro.repository;

import com.tmpro.model.Statistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatisticRepository extends JpaRepository<Statistic, Long> {

    List<Statistic> findByPlayerId(Long playerId);

    @Query("SELECT s FROM Statistic s JOIN FETCH s.player ORDER BY s.id DESC")
    List<Statistic> findAllWithPlayer();

    @Query("SELECT s FROM Statistic s JOIN FETCH s.player WHERE s.id = :id")
    Optional<Statistic> findByIdWithPlayer(@Param("id") Long id);

    @Query("SELECT s FROM Statistic s JOIN FETCH s.player WHERE s.player.id = :playerId ORDER BY s.id DESC")
    List<Statistic> findByPlayerIdWithPlayer(@Param("playerId") Long playerId);

    @Query("SELECT s FROM Statistic s JOIN FETCH s.player p WHERE p.team.id = :teamId ORDER BY p.dorsal, s.id DESC")
    List<Statistic> findByTeamIdWithPlayer(@Param("teamId") Long teamId);

    @Query("SELECT s FROM Statistic s JOIN FETCH s.player WHERE s.matchEntity.id = :matchId")
    List<Statistic> findByMatchIdWithPlayer(@Param("matchId") Long matchId);

    Optional<Statistic> findByMatchEntityIdAndPlayerId(Long matchEntityId, Long playerId);

    @Query("SELECT COUNT(s), SUM(s.goals), SUM(s.assists) FROM Statistic s")
    List<Object[]> getAggregatedSummary();
}
