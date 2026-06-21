package com.tmpro.config;

import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.model.Match;
import com.tmpro.model.MatchStatus;
import com.tmpro.model.Player;
import com.tmpro.model.Statistic;
import com.tmpro.model.Team;
import com.tmpro.repository.MatchRepository;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.repository.RoleRepository;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.repository.TeamRepository;
import com.tmpro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    @Bean
    @Profile("local")
    CommandLineRunner seedLocalData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            MatchRepository matchRepository,
            StatisticRepository statisticRepository,
            DemoDataSeeder demoDataSeeder
    ) {
        return args -> {
            Role adminRole = ensureRole(roleRepository, "ADMIN");
            Role userRole = ensureRole(roleRepository, "USER");
            Role trainerRole = ensureRole(roleRepository, "TRAINER");

            User admin = userRepository.findById("yknU8JhLqMRCOATldGyJTAlpTkF3")
                    .orElseGet(() -> {
                        User created = new User();
                        created.setId("yknU8JhLqMRCOATldGyJTAlpTkF3");
                        created.setUsername(ADMIN_USERNAME);
                        return created;
                    });

            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(""); // Passwords gestionadas por Firebase
            admin.setRoleId(adminRole.getId());
            userRepository.save(admin);

            log.info("Usuario administrador listo: {} / {}", ADMIN_USERNAME, ADMIN_PASSWORD);


            User trainer = userRepository.findById("trainer-demo-uid")
                    .orElseGet(() -> {
                        User created = new User();
                        created.setId("trainer-demo-uid");
                        created.setUsername("entrenador");
                        return created;
                    });
            trainer.setUsername("entrenador");
            trainer.setPassword("");
            trainer.setRoleId(trainerRole.getId());
            userRepository.save(trainer);
            log.info("Usuario entrenador demo: no puede loguearse sin Firebase (ID simulado)");

            demoDataSeeder.seed(teamRepository, playerRepository, admin.getId());

            teamRepository.findAll().forEach(team -> {
                if (team.getOwnerUserId() == null) {
                    team.setOwnerUserId(admin.getId());
                    teamRepository.save(team);
                }
            });

            Team demoTeam = teamRepository.findByName("FC Prueba");


            User fan = userRepository.findById("fan-demo-uid")
                    .orElseGet(() -> {
                        User created = new User();
                        created.setId("fan-demo-uid");
                        created.setUsername("usuario");
                        return created;
                    });
            fan.setUsername("usuario");
            fan.setPassword("");
            fan.setRoleId(userRole.getId());
            if (demoTeam != null) {
                fan.setTeamId(demoTeam.getId());
            }
            userRepository.save(fan);
            log.info("Usuario base demo: no puede loguearse sin Firebase (ID simulado)");
            if (demoTeam != null) {
                for (Match match : matchRepository.findAll()) {
                    boolean changed = false;
                    if (match.getTeamId() == null) {
                        match.setTeamId(demoTeam.getId());
                        changed = true;
                    }
                    if (match.getOpponentName() == null || match.getOpponentName().isBlank()) {
                        match.setOpponentName("Rival");
                        changed = true;
                    }
                    if (changed) {
                        matchRepository.save(match);
                    }
                }

                log.info("Borrando estadísticas previas para reiniciar las 38 jornadas...");
                for (Statistic s : statisticRepository.findAll()) {
                    statisticRepository.deleteById(s.getId());
                }
                log.info("Borrando partidos previos...");
                for (Match m : matchRepository.findAll()) {
                    matchRepository.deleteById(m.getId());
                }
                
                log.info("Generando las 38 jornadas...");
                for (int i = 1; i <= 38; i++) {
                    seedClosedMatch(demoTeam, playerRepository, matchRepository, statisticRepository, i);
                }
                log.info("Generación de jornadas finalizada.");
            }
        };
    }

    private void seedClosedMatch(
            Team team,
            PlayerRepository playerRepository,
            MatchRepository matchRepository,
            StatisticRepository statisticRepository,
            int matchNumber
    ) {
        String location = "Jornada " + matchNumber;

        List<Player> roster = new ArrayList<>(playerRepository.findByTeamId(team.getId()));
        if (roster.isEmpty()) {
            return;
        }

        Collections.shuffle(roster, new Random(42));
        List<Player> squad = roster.subList(0, Math.min(16, roster.size()));
        List<Player> lineup = squad.subList(0, Math.min(11, squad.size()));

        Match match = new Match();
        match.setTeamId(team.getId());
        match.setLocation(location);
        match.setOpponentName("FC Rival " + matchNumber);
        match.setDate(new Date(System.currentTimeMillis() - (86400000L * (40 - matchNumber)))); // Fechas escalonadas
        match.setHome(true);
        match.setStatus(MatchStatus.FINISHED);
        matchRepository.save(match);

        match.setSquadPlayerIds(squad.stream().map(Player::getId).collect(Collectors.toList()));
        match.setLineupPlayerIds(lineup.stream().map(Player::getId).collect(Collectors.toList()));
        matchRepository.save(match);

        Random random = new Random(20260517L + matchNumber);
        int opponentScore = random.nextInt(4);
        int teamScore = random.nextInt(5);
        match.setTeamScore(teamScore);
        match.setOpponentScore(opponentScore);
        matchRepository.save(match);

        String label = match.getLocation() + " · " + match.getDate();

        for (Player p : squad) {
            Statistic stat = new Statistic();
            stat.setPlayerId(p.getId());
            stat.setMatchId(match.getId());
            stat.setMatch(label);
            boolean starter = lineup.stream().anyMatch(lp -> lp.getId().equals(p.getId()));
            int minutes = starter ? 60 + random.nextInt(36) : random.nextInt(46);
            stat.setMinutesPlayed(minutes);

            boolean isPortero = "Portero".equalsIgnoreCase(p.getPosition());
            int passesTotal = 10 + random.nextInt(40);
            stat.setPassesTotal(passesTotal);
            stat.setPassesCompleted((int) (passesTotal * (0.6 + (random.nextDouble() * 0.3))));
            
            int duelsTotal = random.nextInt(15);
            stat.setDuelsTotal(duelsTotal);
            stat.setDuelsWon((int) (duelsTotal * (0.3 + (random.nextDouble() * 0.5))));
            
            stat.setInterceptions(random.nextInt(6));

            if (isPortero) {
                stat.setSaves(random.nextInt(6));
                stat.setGoalsConceded(opponentScore);
            } else {
                int shots = random.nextInt(5);
                stat.setShotsTotal(shots);
                stat.setShotsOnTarget((int) (shots * (0.2 + (random.nextDouble() * 0.8))));
            }

            statisticRepository.save(stat);
        }

        List<Statistic> stats = statisticRepository.findByMatchIdWithPlayer(match.getId());
        for (int i = 0; i < teamScore; i++) {
            Statistic scorer = stats.get(random.nextInt(Math.max(1, Math.min(stats.size(), lineup.size()))));
            scorer.setGoals(scorer.getGoals() + 1);
            statisticRepository.save(scorer);

            if (stats.size() > 1) {
                Statistic assistant = stats.get(random.nextInt(Math.max(1, Math.min(stats.size(), lineup.size()))));
                if (!assistant.getId().equals(scorer.getId())) {
                    assistant.setAssists(assistant.getAssists() + 1);
                    statisticRepository.save(assistant);
                }
            }
        }
    }

    private Role ensureRole(RoleRepository repository, String name) {
        return repository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return repository.save(role);
        });
    }
}
