package escape.core.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import escape.core.admin.domain.Admin.AdminBuilder;
import escape.core.admin.repository.AdminRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
class AdminTest {

    private static final String TEST_LOGIN_ID = "test_admin";
    private static final String PASSWORD = "password123";
    private static final String TEST_NAME = "Test Admin";
    private static final String PHONE_NUMBER = "010-1234-5678";

    @Autowired
    private AdminRepository adminRepository;

    @PersistenceContext
    private EntityManager em;

    private Admin unsavedAdmin;
    private Admin.AdminBuilder adminBuilder;

    @BeforeEach
    void setUp() {
        unsavedAdmin = Admin.builder()
                .loginId(TEST_LOGIN_ID)
                .password(PASSWORD)
                .name(TEST_NAME)
                .phoneNumber(PHONE_NUMBER)
                .role(Role.OFFICE_MANAGER)
                .build();

        adminBuilder = Admin.builder()
                .loginId(TEST_LOGIN_ID)
                .password(PASSWORD)
                .name(TEST_NAME)
                .phoneNumber(PHONE_NUMBER)
                .role(Role.OFFICE_MANAGER);
    }

    @Transactional
    @DisplayName("같은 로그인 ID로 관리자를 저장할 수 없다")
    @Test
    void cannot_create_admin_with_duplicate_login_id() {
        // given
        Admin savedAdmin = adminRepository.save(adminBuilder.loginId("duplicate").build());

        // when & then
        assertThatThrownBy(() -> {
            Admin adminWithDuplicateLoginId = adminBuilder.loginId(savedAdmin.getLoginId()).build();
            adminRepository.save(adminWithDuplicateLoginId);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Transactional
    @DisplayName("아이디, 패스워드, 이름, 역할 중 하나라도 없는 관리자는 생성에 실패한다")
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideInvalidAdmin")
    void fail_createAdmin(String caseName, Admin invalidAdmin) {
        // when & then
        assertThatThrownBy(() -> adminRepository.save(invalidAdmin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<Arguments> provideInvalidAdmin() {
        AdminBuilder adminBuilder = Admin.builder();

        return Stream.of(
                Arguments.of("아이디가 null", adminBuilder.loginId(null).build()),
                Arguments.of("아이디가 20자 초과", adminBuilder.loginId("a".repeat(21)).build()),
                Arguments.of("패스워드가 null", adminBuilder.loginId(TEST_LOGIN_ID).password(null).build()),
                Arguments.of("이름이 null", adminBuilder.loginId(TEST_LOGIN_ID).password(PASSWORD).name(null).build()),
                Arguments.of("이름이 100자 초과",
                        adminBuilder.loginId(TEST_LOGIN_ID).password(PASSWORD).name(TEST_NAME.repeat(100)).build()),
                Arguments.of("역할이 null",
                        adminBuilder.loginId(TEST_LOGIN_ID).password(PASSWORD).name(TEST_NAME).role(null).build())
        );
    }

    @Transactional
    @DisplayName("관리자 생성에 성공한다")
    @Test
    void success_createAdmin() {
        // when
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // then
        assertThat(savedAdmin).satisfies(admin -> {
            assertThat(admin.getLoginId()).isEqualTo(TEST_LOGIN_ID);
            assertThat(admin.getName()).isEqualTo(TEST_NAME);
            assertThat(admin.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
            assertThat(admin.getRole()).isEqualTo(Role.OFFICE_MANAGER);
            assertThat(admin.getCreatedAt()).isBefore(LocalDateTime.now());
            assertThat(admin.getUpdatedAt()).isBefore(LocalDateTime.now());
        });
    }

    @Transactional
    @DisplayName("100자가 넘는 이름으로 변경은 허용되지 않는다")
    @Test
    void fail_changeName() {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        String changeName = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> savedAdmin.changeName(changeName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100자가 넘을 수 없습니다");
    }

    @Transactional
    @DisplayName("1글자 이상 100자 이하로 이름을 변경할 수 있다")
    @ParameterizedTest
    @ValueSource(strings = {"관", "관리자"})
    void success_changeName(String changeName) {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when
        savedAdmin.changeName(changeName);
        em.flush();

        Admin changedAdmin = adminRepository.findById(savedAdmin.getId()).orElseThrow();

        // then
        assertThat(changedAdmin.getName()).isEqualTo(changeName);
    }

    @Transactional
    @DisplayName("8자 미만인 비밀번호로 변경할 수 없다")
    @ParameterizedTest
    @ValueSource(strings = {"", "1", "1234567"})
    void fail_changePassword(String newPassword) {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when & then
        assertThatThrownBy(() -> savedAdmin.changePassword(newPassword))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("변경할 비밀번호가 8자 이상이면 비밀번호를 변경할 수 있다")
    @Test
    void success_changePassword() {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when
        String changedPassword = "newPassword123";
        savedAdmin.changePassword(changedPassword);
        em.flush();

        Admin changedAdmin = adminRepository.findById(savedAdmin.getId()).orElseThrow();

        // then
        assertThat(changedAdmin.getPassword()).isEqualTo(changedPassword);
    }

    @Transactional
    @DisplayName("올바른 형식의 전화번호로 변경할 수 있다")
    @Test
    void success_changePhoneNumber() {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when
        String changedPhoneNumber = "010-9876-5432";
        savedAdmin.changePhoneNumber(changedPhoneNumber);
        em.flush();

        Admin changedAdmin = adminRepository.findById(savedAdmin.getId()).orElseThrow();

        // then
        assertThat(changedAdmin.getPhoneNumber()).isEqualTo(changedPhoneNumber);
    }

    @Transactional
    @DisplayName("잘못된 형식의 전화번호로 변경할 수 없다")
    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "010-123-4567", "011-1234-5678"})
    void fail_changePhoneNumber(String invalidPhoneNumber) {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when & then
        assertThatThrownBy(() -> savedAdmin.changePhoneNumber(invalidPhoneNumber))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전화번호 패턴에 맞지 않는 값입니다");
    }

    @Transactional
    @DisplayName("관리자 역할을 변경할 수 있다")
    @ParameterizedTest
    @EnumSource
    void changeRole(Role newRole) {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when
        savedAdmin.changeRole(newRole);
        em.flush();

        Admin changedAdmin = adminRepository.findById(savedAdmin.getId()).orElseThrow();

        // then
        assertThat(changedAdmin.getRole()).isEqualTo(newRole);
    }

    @Transactional
    @DisplayName("관리자 역할을 null로 변경할 수 없다")
    @Test
    void fail_changeRole() {
        // given
        Admin savedAdmin = adminRepository.save(unsavedAdmin);

        // when & then
        assertThatThrownBy(() -> savedAdmin.changeRole(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역할 값은 null 일 수 없습니다");
    }
}