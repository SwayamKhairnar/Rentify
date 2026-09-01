package com.rentify.user;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 500)
    private String avatar = "";

    @Column(nullable = false, length = 100)
    private String campus = "";

    @Column(nullable = false, length = 300)
    private String bio = "";

    @Column(nullable = false, length = 20)
    private String phone = "";

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews = 0;

    @Column(name = "lender_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal lenderRating = BigDecimal.ZERO;

    @Column(name = "total_lender_reviews", nullable = false)
    private Integer totalLenderReviews = 0;

    @Column(name = "renter_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal renterRating = BigDecimal.ZERO;

    @Column(name = "total_renter_reviews", nullable = false)
    private Integer totalRenterReviews = 0;

    @Column(name = "item_quality_average", nullable = false, precision = 3, scale = 1)
    private BigDecimal itemQualityAverage = BigDecimal.ZERO;

    @Column(name = "total_item_quality_reviews", nullable = false)
    private Integer totalItemQualityReviews = 0;

    @Column(name = "is_suspended", nullable = false)
    private boolean isSuspended = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User() {}

    public User(String name, String email, String password, String campus) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.campus = campus != null ? campus : "";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar != null ? avatar : ""; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus != null ? campus : ""; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio != null ? bio : ""; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone != null ? phone : ""; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating != null ? rating : BigDecimal.ZERO; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews != null ? totalReviews : 0; }

    public BigDecimal getLenderRating() { return lenderRating; }
    public void setLenderRating(BigDecimal lenderRating) { this.lenderRating = lenderRating != null ? lenderRating : BigDecimal.ZERO; }

    public Integer getTotalLenderReviews() { return totalLenderReviews; }
    public void setTotalLenderReviews(Integer totalLenderReviews) { this.totalLenderReviews = totalLenderReviews != null ? totalLenderReviews : 0; }

    public BigDecimal getRenterRating() { return renterRating; }
    public void setRenterRating(BigDecimal renterRating) { this.renterRating = renterRating != null ? renterRating : BigDecimal.ZERO; }

    public Integer getTotalRenterReviews() { return totalRenterReviews; }
    public void setTotalRenterReviews(Integer totalRenterReviews) { this.totalRenterReviews = totalRenterReviews != null ? totalRenterReviews : 0; }

    public BigDecimal getItemQualityAverage() { return itemQualityAverage; }
    public void setItemQualityAverage(BigDecimal itemQualityAverage) { this.itemQualityAverage = itemQualityAverage != null ? itemQualityAverage : BigDecimal.ZERO; }

    public Integer getTotalItemQualityReviews() { return totalItemQualityReviews; }
    public void setTotalItemQualityReviews(Integer totalItemQualityReviews) { this.totalItemQualityReviews = totalItemQualityReviews != null ? totalItemQualityReviews : 0; }

    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
