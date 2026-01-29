package me.khromov.splittycat.repository;

import me.khromov.splittycat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTgId(long tgId);
}
