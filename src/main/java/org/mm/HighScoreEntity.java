package org.mm;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class HighScoreEntity extends PanacheEntity {
    @Column(length = 30)
    public String name;

    @Column()
    public int score;
}
