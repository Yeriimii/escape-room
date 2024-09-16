package escape.core.office.domain;

import static org.assertj.core.api.Assertions.assertThat;

import escape.core.office.repository.OfficeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficeTest {

    @Autowired
    private OfficeRepository officeRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @DisplayName("은행 계좌 2개를 갖는 지점 생성에 성공한다")
    @Test
    void createOffice() {
        // given
        Office office = Office.builder()
                .name("강남점")
                .welcomeMessage("강남점 방문을 환영합니다.")
                .build();

        Account account1 = Account.of("국민은행", "123-456-78910");
        Account account2 = Account.of("신한은행", "987-654-32100");

        office.addAccount(account1);
        office.addAccount(account2);

        // when
        Office savedOffice = officeRepository.save(office);

        // then
        assertThat(savedOffice).isEqualTo(office);
        assertThat(savedOffice.getName()).isEqualTo("강남점");
        assertThat(savedOffice.getAccounts()).containsOnly(account1, account2);
        assertThat(savedOffice.getCreatedAt()).isBefore(LocalDateTime.now());
        assertThat(savedOffice.getUpdatedAt()).isBefore(LocalDateTime.now());
    }

    @Transactional
    @DisplayName("2개의 계좌 중 1개를 삭제할 수 있다")
    @Test
    void removeAccount() {
        // given
        Office office = Office.builder()
                .name("강남점")
                .welcomeMessage("강남점 방문을 환영합니다.")
                .build();

        Account account1 = Account.of("국민은행", "123-456-78910");
        Account account2 = Account.of("신한은행", "987-654-32100");

        office.addAccount(account1);
        office.addAccount(account2);

        Office savedOffice = officeRepository.save(office);

        // when
        savedOffice.removeAccount(Account.of("국민은행", "123-456-78910"));

        // then
        assertThat(savedOffice.getAccounts()).hasSize(1);
        assertThat(savedOffice.getAccounts()).containsOnly(account2);
    }

    @Transactional
    @DisplayName("지점 이름을 변경할 수 있다.")
    @Test
    void changeName() {
        // given
        Office office = Office.builder()
                .name("강남점")
                .build();

        Office savedOffice = officeRepository.save(office);

        // when
        String changedName = "을지로점";
        savedOffice.changeName(changedName);
        em.flush();

        Office changedOffice = officeRepository.findById(savedOffice.getId()).orElseThrow();

        // then
        assertThat(changedOffice.getName()).isEqualTo(changedName);
    }

    @Transactional
    @DisplayName("지점 인삿말을 변경할 수 있다.")
    @Test
    void changeWelcomeMessage() {
        // given
        Office office = Office.builder()
                .name("강남점")
                .welcomeMessage("강남점 방문을 환영합니다.")
                .build();

        Office savedOffice = officeRepository.save(office);

        // when
        String changedMessage = "여러분을 환영합니다.";
        savedOffice.changeWelcomeMessage(changedMessage);
        em.flush();

        Office changedOffice = officeRepository.findById(savedOffice.getId()).orElseThrow();

        // then
        assertThat(changedOffice.getWelcomeMessage()).isEqualTo(changedMessage);
    }
}