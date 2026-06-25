package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.UserStatus;
import com.example.aitraining.domain.Models.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * <b>Repository Pattern</b> — all persistence operations for {@link User} documents in the
 * {@code users} collection.
 *
 * <p>There is no create/delete method here because user management is handled at the infrastructure
 * level (seeded by {@link com.example.aitraining.config.MongoSeedConfig}); the API only supports
 * reading and status updates.
 */
@Repository
public class UserRepository {
  private final MongoTemplate mongo;

  public UserRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  /**
   * Resolves the caller from an HTTP bearer token.
   *
   * <p>The token is matched case-insensitively against {@code email}, and also tried as a
   * UUID against {@code _id}.  Returns only {@code ACTIVE} accounts; a {@code DISABLED}
   * account returns {@link Optional#empty()} so the filter can reject it.
   *
   * @param token the raw value extracted from the {@code Authorization: Bearer} header
   */
  public Optional<User> findActiveByToken(String token) {
    List<Criteria> identityMatches = new ArrayList<>();
    identityMatches.add(Criteria.where("email").regex("^" + Pattern.quote(token) + "$", "i"));
    try {
      identityMatches.add(Criteria.where("_id").is(UUID.fromString(token)));
    } catch (IllegalArgumentException ignored) {
      // Token is not a UUID; the email match still applies.
    }
    Query query = Query.query(new Criteria().andOperator(
        Criteria.where("status").is(UserStatus.ACTIVE),
        new Criteria().orOperator(identityMatches.toArray(new Criteria[0]))));
    return Optional.ofNullable(mongo.findOne(query, User.class));
  }

  /**
   * Loads a user by ID.
   *
   * @throws org.springframework.dao.EmptyResultDataAccessException if not found
   */
  public User get(UUID userId) {
    return required(mongo.findById(userId, User.class));
  }

  public List<User> list(int limit) {
    return mongo.find(new Query().with(Sort.by(Sort.Direction.ASC, "createdAt")).limit(limit), User.class);
  }

  /**
   * Atomically sets the user's status and returns the updated document.
   * Used by admin endpoints to enable or disable accounts.
   *
   * @throws org.springframework.dao.EmptyResultDataAccessException if the user is not found
   */
  public User updateStatus(UUID userId, UserStatus status) {
    return required(mongo.findAndModify(
        Query.query(Criteria.where("_id").is(userId)),
        new Update().set("status", status),
        FindAndModifyOptions.options().returnNew(true),
        User.class));
  }

  private static User required(User user) {
    if (user == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return user;
  }
}
