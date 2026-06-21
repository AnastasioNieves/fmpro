package com.tmpro.repository;

import com.tmpro.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {
    
    private final Map<String, User> data = new ConcurrentHashMap<>();
    
    public User save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }
        data.put(user.getId(), user);
        return user;
    }
    
    public Optional<User> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<User> findAll() {
        return new ArrayList<>(data.values());
    }

    public void deleteById(String id) {
        data.remove(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return data.values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equals(username))
                .findFirst();
    }

    public Optional<User> findByUsernameIgnoreCase(String username) {
        return data.values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return findByUsernameIgnoreCase(username).isPresent();
    }
}
