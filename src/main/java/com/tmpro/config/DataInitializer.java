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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
            ensureRole(roleRepository, "USER");
            ensureRole(roleRepository, "TRAINER");

            User admin = userRepository.findByUsernameIgnoreCase(ADMIN_USERNAME)
                    .orElseGet(() -> {
                        User created = new User();
                        created.setUsername(ADMIN_USERNAME);
                        return created;
                    });

            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRoleId(adminRole.getId());
            userRepository.save(admin);

            log.info("Usuario administrador listo: {} / {}", ADMIN_USERNAME, ADMIN_PASSWORD);

            Role trainerRole = roleRepository.findByNameIgnoreCase("TRAINER").orElseThrow();
            User trainer = userRepository.findByUsernameIgnoreCase("entrenador")
                    .orElseGet(() -> {
                        User created = new User();
                        created.setUsername("entrenador");
                        return created;
                    });
            trainer.setUsername("entrenador");
            trainer.setPassword(passwordEncoder.encode("entrenador"));
            trainer.setRoleId(trainerRole.getId());
            userRepository.save(trainer);
            log.info("Usuario entrenador demo: entrenador / entrenador");

            demoDataSeeder.seed(teamRepository, playerRepository, admin.getId());

            teamRepository.findAll().forEach(team -> {
                if (team.getOwnerUserId() == null) {
                    team.setOwnerUserId(admin.getId());
                    teamRepository.save(team);
                }
            });

            Team demoTeam = teamRepository.findByName("FC Prueba");

            Role userRole = roleRepository.findByNameIgnoreCase("USER").orElseThrow();
            User fan = userRepository.findByUsernameIgnoreCase("usuario")
                    .orElseGet(() -> {
                        User created = new User();
                        created.setUsername("usuario");
                        return created;
                    });
            fan.setUsername("usuario");
            fan.setPassword(passwordEncoder.encode("usuario"));
            fan.setRoleId(userRole.getId());
            if (demoTeam != null) {
                fan.setTeamId(demoTeam.getId());
            }
            userRepository.save(fan);
            log.info("Usuario base demo: usuario / usuario (sigue a FC Prueba)");
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

                seedClosedMatch(demoTeam, playerRepository, matchRepository, statisticRepository);
            }
        };
    }

    private void seedClosedMatch(
            Team team,
            PlayerRepository playerRepository,
            MatchRepository matchRepository,
            StatisticRepository statisticRepository
    ) {
        String location = "Partido demo cerrado";
        boolean exists = matchRepository.findAll().stream()
                .anyMatch(m -> location.equalsIgnoreCase(m.getLocation()));
        if (exists) {
            return;
        }

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
        match.setOpponentName("FC Rival Demo");
        match.setDate(LocalDateTime.now().minusDays(1).withSecond(0).withNano(0));
        match.setHome(true);
        match.setStatus(MatchStatus.FINISHED);
        matchRepository.save(match);

        match.setSquad(new ArrayList<>(squad));
        match.setLineup(new ArrayList<>(lineup));
        matchRepository.save(match);

        Random random = new Random(20260517L);
        int opponentScore = random.nextInt(5);
        int teamScore = random.nextInt(5);
        match.setTeamScore(teamScore);
        match.setOpponentScore(opponentScore);
        matchRepository.save(match);

        String label = match.getLocation() + " · " + match.getDate();

        for (Player p : squad) {
            Statistic stat = new Statistic();
            stat.setPlayer(p);
            stat.setMatchEntity(match);
            stat.setMatch(label);
            boolean starter = lineup.stream().anyMatch(lp -> lp.getId().equals(p.getId()));
            int minutes = starter ? 60 + random.nextInt(36) : random.nextInt(46);
            stat.setMinutesPlayed(minutes);
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
        return repository.findByNameIgnoreCase(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return repository.save(role);
        });
    }
}
