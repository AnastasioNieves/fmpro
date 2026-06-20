package com.tmpro.repository;

import com.tmpro.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByName(String name);

    Optional<Long> findIdByName(String name);

    @Query("SELECT p FROM Player p JOIN FETCH p.team")
    List<Player> findAllWithTeam();

    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.team WHERE p.id = :id")
    Optional<Player> findByIdWithTeam(@Param("id") Long id);

    long countByTeamId(Long teamId);

    List<Player> findByTeamId(Long teamId);
}
