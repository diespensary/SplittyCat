package me.khromov.splittycat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private User linkedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    public static Participant ownerSlot(Event event, User ownerUser, String name, String normalizedName) {
        Participant participant = freeSlot(event, ownerUser, name, normalizedName);
        participant.linkTo(ownerUser);
        return participant;
    }

    public static Participant freeSlot(Event event, User createdByUser, String name, String normalizedName) {
        Participant participant = new Participant();
        participant.event = event;
        participant.name = name;
        participant.normalizedName = normalizedName;
        participant.createdByUser = createdByUser;
        return participant;
    }

    public void linkTo(User user) {
        linkedUser = user;
    }

    public boolean isLinked() {
        return linkedUser != null;
    }

    public boolean isLinkedTo(User user) {
        return linkedUser != null && user != null && linkedUser.hasId(user.getId());
    }

    public boolean isCreatedBy(User user) {
        return createdByUser != null && user != null && createdByUser.hasId(user.getId());
    }
}
