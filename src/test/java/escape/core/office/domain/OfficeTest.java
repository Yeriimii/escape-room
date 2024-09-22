package escape.core.office.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import escape.core.office.repository.OfficeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles(profiles = {"test", "log"})
class OfficeTest {

    @Autowired
    private OfficeRepository officeRepository;

    @PersistenceContext
    private EntityManager em;

    private Office unsavedOffice;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        unsavedOffice = Office.builder()
                .name("강남점")
                .welcomeMessage("강남점 방문을 환영합니다.")
                .build();

        account1 = Account.of("국민은행", "123-456-78910");
        account2 = Account.of("신한은행", "987-654-32100");
    }

    @Transactional
    @DisplayName("같은 이름으로 지점을 저장할 수 없다")
    @Test
    void cannot_create_office_with_duplicate_name() {
        // given
        Office savedOffice = officeRepository.save(Office.builder().name("duplicate name").build());

        // when & then
        assertThatThrownBy(() -> officeRepository.save(Office.builder().name(savedOffice.getName()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Transactional
    @DisplayName("은행 계좌 2개를 갖는 지점 생성에 성공한다")
    @Test
    void success_createOffice() {
        // given
        unsavedOffice.addAccount(account1);
        unsavedOffice.addAccount(account2);

        // when
        Office savedOffice = officeRepository.save(unsavedOffice);

        // then
        assertThat(savedOffice).satisfies(office -> {
            assertThat(office.getName()).isEqualTo("강남점");
            assertThat(office.getAccounts()).containsOnly(account1, account2);
            assertThat(office.getCreatedAt()).isBefore(LocalDateTime.now());
            assertThat(office.getUpdatedAt()).isBefore(LocalDateTime.now());
        });
    }

    @Transactional
    @DisplayName("2개의 계좌 중 1개를 삭제할 수 있다")
    @Test
    void removeAccount() {
        // given
        unsavedOffice.addAccount(account1);
        unsavedOffice.addAccount(account2);

        Office savedOffice = officeRepository.save(unsavedOffice);

        // when
        savedOffice.removeAccount(Account.of("국민은행", "123-456-78910"));

        // then
        assertThat(savedOffice.getAccounts()).hasSize(1);
        assertThat(savedOffice.getAccounts()).containsOnly(account2);
    }

    @Transactional
    @DisplayName("100자가 넘는 지점 이름으로 변경은 허용되지 않는다.")
    @Test
    void fail_changeName() {
        // given
        Office savedOffice = officeRepository.save(unsavedOffice);

        String changeName = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> savedOffice.changeName(changeName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("1글자 이상 100자 이하로 지점 이름을 변경할 수 있다.")
    @ParameterizedTest
    @ValueSource(strings = {"테", "테스트"})
    void success_changeName(String changeName) {
        // given
        Office savedOffice = officeRepository.save(unsavedOffice);

        // when
        savedOffice.changeName(changeName);
        em.flush();

        Office changedOffice = officeRepository.findById(savedOffice.getId()).orElseThrow();

        // then
        assertThat(changedOffice.getName()).isEqualTo(changeName);
    }

    @Transactional
    @DisplayName("지점 인삿말을 변경할 수 있다.")
    @Test
    void changeWelcomeMessage() {
        // given
        Office savedOffice = officeRepository.save(unsavedOffice);

        // when
        String changedMessage = "여러분을 환영합니다.";
        savedOffice.changeWelcomeMessage(changedMessage);
        em.flush();

        Office changedOffice = officeRepository.findById(savedOffice.getId()).orElseThrow();

        // then
        assertThat(changedOffice.getWelcomeMessage()).isEqualTo(changedMessage);
    }
}