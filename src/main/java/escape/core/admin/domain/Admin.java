package escape.core.admin.domain;

import escape.core.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 관리자 엔터티를 생성할 때는 반드시 {@link Role}을 지정해야 합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin")
public class Admin extends BaseEntity {

    private static final int MAX_LENGTH_PHONE_NUMBER = 13;
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^(010)-(\\d{4})-(\\d{4})$");
    private static final int MIN_LENGTH_PASSWORD = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Integer id;

    @Column(name = "login_id", unique = true, nullable = false, length = 20)
    private String loginId;

    @Column(name = "password")
    private String password;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = MAX_LENGTH_PHONE_NUMBER)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    public void changeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("이름은 null 이거나, 비어있거나, 공백만으로 채울 수 없습니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("변경할 이름은 100자가 넘을 수 없습니다.");
        }
        this.name = name;
    }

    public void changePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("패스워드는 null 이거나, 비어있거나, 공백만으로 채울 수 없습니다.");
        }
        if (password.length() < MIN_LENGTH_PASSWORD) {
            throw new IllegalArgumentException("패스워드는 8자 이상이어야 합니다.");
        }
        this.password = password;
    }

    public void changePhoneNumber(String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        this.phoneNumber = phoneNumber;
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("전화번호는 null 이거나, 비어있거나, 공백만으로 채울 수 없습니다.");
        }
        if (phoneNumber.length() > MAX_LENGTH_PHONE_NUMBER) {
            throw new IllegalArgumentException("전화번호 길이는 13자리를 넘을 수 없습니다.");
        }
        if (!PHONE_NUMBER_PATTERN.matcher(phoneNumber).find()) {
            throw new IllegalArgumentException("전화번호 패턴에 맞지 않는 값입니다.");
        }
    }

    public void changeRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("역할 값은 null 일 수 없습니다.");
        }
        this.role = role;
    }
}
