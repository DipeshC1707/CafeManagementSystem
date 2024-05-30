package com.inn.cafe.dao;

import com.inn.cafe.POJO.Token;
import com.inn.cafe.POJO.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface TokenDao extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByUserAndExpirationDateAfter(User user, Instant now);
}