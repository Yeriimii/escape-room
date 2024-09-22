package escape.core.theme.domain;

import escape.core.office.domain.Office;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * <p> 테마 정보를 나타내는 엔티티 클래스입니다. </p>
 * 테마를 생성할 때 반드시 {@link Office} 객체를 지정해야 합니다.
 *
 * @see Office
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "theme")
public class Theme {

    public static final int MAX_CAPACITY = 6;
    public static final int MIN_CAPACITY = 1;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Integer id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Default
    @Column(name = "discount")
    private Integer discountAmount = 0;

    @Default
    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = false;

    @Default
    @Column(name = "capacity")
    private Integer capacity = 2;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false, foreignKey = @ForeignKey(name = "fk_theme_office"))
    private Office office;

    public Integer getFinalEntranceFee() {
        return price - discountAmount;
    }

    public void changeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("이름은 null 이거나, 비어있거나, 공백만으로 채울 수 없습니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("변경할 이름은 100자가 넘을 수 없습니다.");
        }
        this.name = name;
    }

    public void changePrice(Integer price) {
        if (price < 0) {
            throw new IllegalArgumentException("입장료 가격은 음수일 수 없습니다.");
        }
        this.price = price;
    }

    public void changeOpenTime(LocalTime openTime) {
        if (openTime == null) {
            throw new IllegalArgumentException("예약 오픈 시간은 null 일 수 없습니다.");
        }
        this.openTime = openTime;
    }

    public void changeDiscountAmount(Integer discountAmount) {
        if (discountAmount < 0) {
            throw new IllegalArgumentException("할인 금액은 음수일 수 없습니다.");
        }
        this.discountAmount = discountAmount;
    }

    public void changeIsAvailable(Boolean isAvailable) {
        if (isAvailable == null) {
            throw new IllegalArgumentException("이용 가능 상태 값은 null 일 수 없습니다.");
        }
        this.isAvailable = isAvailable;
    }

    public void changeCapacity(Integer capacity) {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    String.format("수용 인원은 %d명 ~ %d명 사이의 값으로만 변경할 수 있습니다.", MIN_CAPACITY, MAX_CAPACITY)
            );
        }
        this.capacity = capacity;
    }
}