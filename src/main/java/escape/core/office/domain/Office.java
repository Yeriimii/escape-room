package escape.core.office.domain;

import escape.core.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
@Table(name = "office")
public class Office extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "office_id")
    private Integer id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Default
    @ElementCollection
    @CollectionTable(name = "account", joinColumns = {
            @JoinColumn(name = "office_id", foreignKey = @ForeignKey(name = "fk_account_office"))
    })
    private Set<Account> accounts = new HashSet<>();

    @Column(name = "welcome_message")
    private String welcomeMessage;

    public void changeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("이름은 null 이거나, 비어있거나, 공백만으로 채울 수 없습니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("변경할 이름은 100자가 넘을 수 없습니다.");
        }
        this.name = name;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
    }

    public void changeWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }
}
