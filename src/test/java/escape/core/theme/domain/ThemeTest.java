package escape.core.theme.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import escape.core.office.domain.Office;
import escape.core.office.repository.OfficeRepository;
import escape.core.theme.repository.ThemeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles(profiles = {"test", "log"})
class ThemeTest {

    public static final int PRICE = 10_000;
    public static final int DISCOUNT_AMOUNT = 500;
    public static final int CAPACITY = 2;
    public static final String THEME_NAME = "히어로 컴퍼니";
    public static final LocalTime NOW = LocalTime.now();

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @PersistenceContext
    private EntityManager em;

    private Office savedOffice;
    private Theme.ThemeBuilder themeBuilder;

    @BeforeEach
    void setOffice() {
        Office unsavedOffice = Office.builder()
                .name("더미 지점")
                .build();

        savedOffice = officeRepository.save(unsavedOffice);

        themeBuilder = Theme.builder()
                .name(THEME_NAME)
                .price(PRICE)
                .discountAmount(DISCOUNT_AMOUNT)
                .openTime(NOW)
                .isAvailable(true)
                .capacity(CAPACITY);
    }


    @Transactional
    @DisplayName("Office 객체 없이 Theme 객체를 생성할 수 없다")
    @Test
    void cannot_create_theme_without_office() {
        // given
        Theme themeWithoutOffice = themeBuilder.build();

        // when & then
        assertThatThrownBy(() -> themeRepository.save(themeWithoutOffice))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Transactional
    @DisplayName("영속화되지 않은 Office 객체로 Theme 를 저장하려 하면 InvalidDataAccessApiUsageException 예외가 발생한다")
    @Test
    void cannot_create_theme_with_unsaved_office() {
        // given
        Office unsavedOffice = Office.builder().name("unsaved office").build();
        Theme themeWithUnsavedOffice = themeBuilder.office(unsavedOffice).build();

        // when & then
        assertThatThrownBy(() -> themeRepository.save(themeWithUnsavedOffice))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidThemeData")
    @DisplayName("테마 이름이나 입장 가격이 없는 테마를 저장할 수 없다")
    void cannot_create_theme_without_required_fields(String name, Integer price) {
        // given
        Theme invalidTheme = themeBuilder
                .name(name)
                .price(price)
                .office(savedOffice)
                .build();

        // when & then
        assertThatThrownBy(() -> themeRepository.save(invalidTheme))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<Arguments> provideInvalidThemeData() {
        return Stream.of(
                Arguments.of(null, PRICE),
                Arguments.of(THEME_NAME, null)
        );
    }

    @Transactional
    @DisplayName("영속화된 Office 객체가 있으면 테마를 저장할 수 있다")
    @Test
    void can_create_theme_with_saved_office() {
        // given
        Theme themeWithSavedOffice = themeBuilder.office(savedOffice).build();

        // when
        Theme savedTheme = themeRepository.save(themeWithSavedOffice);

        // then
        assertThat(savedTheme).satisfies(theme -> {
            assertThat(theme.getName()).isEqualTo(THEME_NAME);
            assertThat(theme.getFinalEntranceFee()).isEqualTo(PRICE - DISCOUNT_AMOUNT);
            assertThat(theme.getCapacity()).isEqualTo(CAPACITY);
            assertThat(theme.getOpenTime()).isEqualTo(NOW);
            assertThat(theme.isAvailable()).isTrue();
            assertThat(theme.getOffice()).isEqualTo(savedOffice);
        });
    }

    @Transactional
    @DisplayName("중복된 이름으로 테마를 저장할 수 없다")
    @Test
    void cannot_create_theme_with_duplicate_theme_name() {
        // given
        Theme theme1 = themeBuilder.name("테스트 테마").office(savedOffice).build();
        themeRepository.save(theme1);

        Theme theme2 = themeBuilder.name("테스트 테마").office(savedOffice).build();

        // when & then
        assertThatThrownBy(() -> themeRepository.save(theme2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Transactional
    @DisplayName("1글자 이상 100자 이하의 테마 이름으로 변경할 수 있다")
    @ParameterizedTest
    @MethodSource("provideInvalidThemeName")
    void fail_changeName(String newName) {
        // given
        Theme themeWithSavedOffice = themeBuilder.office(savedOffice).build();
        Theme savedTheme = themeRepository.save(themeWithSavedOffice);

        // when & then
        assertThatThrownBy(() -> savedTheme.changeName(newName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> provideInvalidThemeName() {
        return Stream.of(
                Arguments.of("a".repeat(101)),
                Arguments.of("test".repeat(101))
        );
    }

    @Transactional
    @DisplayName("1글자 이상 100자 이하의 테마 이름으로 변경할 수 있다")
    @ParameterizedTest
    @MethodSource("provideValidThemeName")
    void success_changeName(String changeName) {
        // given
        Theme themeWithSavedOffice = themeBuilder.office(savedOffice).build();
        Theme savedTheme = themeRepository.save(themeWithSavedOffice);

        // when
        savedTheme.changeName(changeName);
        em.flush();

        Theme findTheme = themeRepository.findById(savedTheme.getId()).orElseThrow();

        // then
        assertThat(findTheme.getName()).isEqualTo(changeName);
    }

    private static Stream<Arguments> provideValidThemeName() {
        return Stream.of(
                Arguments.of("a"),
                Arguments.of("test theme name"),
                Arguments.of("a".repeat(100))
        );
    }

    @Transactional
    @DisplayName("음의 정수 값으로 가격 변경을 할 수 없다")
    @ParameterizedTest
    @ValueSource(ints = {-1, -100, -100_000, Integer.MIN_VALUE})
    void fail_changePrice(int newPrice) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when & then
        assertThatThrownBy(() -> theme.changePrice(newPrice))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("0 이상의 정수 값으로 가격 변경을 할 수 있다")
    @ParameterizedTest
    @ValueSource(ints = {0, 1_000, Integer.MAX_VALUE})
    void success_changePrice(int newPrice) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        theme.changePrice(newPrice);
        em.flush();
        Theme findTheme = themeRepository.findById(theme.getId()).orElseThrow();

        // then
        assertThat(findTheme.getPrice()).isEqualTo(newPrice);
    }

    @Transactional
    @DisplayName("null 값으로 예약 오픈 시간을 변경할 수 없다")
    @Test
    void fail_changeOpenTime() {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        assertThatThrownBy(() -> theme.changeOpenTime(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("null 이 아닌 시간 데이터로 예약 오픈 시간을 변경할 수 있다")
    @ParameterizedTest
    @MethodSource("provideValidOpenTime")
    void success_changeOpenTime(LocalTime newOpenTime) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        theme.changeOpenTime(newOpenTime);
        em.flush();
        Theme findTheme = themeRepository.findById(theme.getId()).orElseThrow();

        // then
        assertThat(findTheme.getOpenTime()).isEqualTo(newOpenTime);
    }

    private static Stream<Arguments> provideValidOpenTime() {
        return Stream.of(
                Arguments.of(NOW.minusHours(1)),
                Arguments.of(NOW.plusHours(1))
        );
    }

    @Transactional
    @DisplayName("음의 정수 값으로 할인 금액 변경을 할 수 없다")
    @ParameterizedTest
    @ValueSource(ints = {-1, -100, -100_000, Integer.MIN_VALUE})
    void fail_changeDiscountAmount(int newDiscountAmount) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when & then
        assertThatThrownBy(() -> theme.changeDiscountAmount(newDiscountAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("0 이상의 정수 값으로 할인 금액 변경을 할 수 있다")
    @ParameterizedTest
    @ValueSource(ints = {0, 1_000, Integer.MAX_VALUE})
    void success_changeDiscountAmount(int newDiscountAmount) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        theme.changePrice(newDiscountAmount);
        em.flush();
        Theme findTheme = themeRepository.findById(theme.getId()).orElseThrow();

        // then
        assertThat(findTheme.getPrice()).isEqualTo(newDiscountAmount);
    }

    @Transactional
    @DisplayName("null 값으로 이용 가능 여부를 변경할 수 없다")
    @Test
    void fail_changeIsAvailable() {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        assertThatThrownBy(() -> theme.changeIsAvailable(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("null 이 아닌 이용 가능 상태로 예약 오픈 시간을 변경할 수 있다")
    @ParameterizedTest
    @MethodSource("provideValidIsAvailable")
    void success_changeIsAvailable(Boolean newAvailable) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        theme.changeIsAvailable(newAvailable);
        em.flush();
        Theme findTheme = themeRepository.findById(theme.getId()).orElseThrow();

        // then
        assertThat(findTheme.isAvailable()).isEqualTo(newAvailable);
    }

    private static Stream<Arguments> provideValidIsAvailable() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    @Transactional
    @DisplayName("0 이하의 정수 값으로 수용 인원을 변경할 수 없다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -100_000, Integer.MIN_VALUE})
    void fail_changeCapacity(int capacity) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when & then
        assertThatThrownBy(() -> theme.changeCapacity(capacity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    @DisplayName("1명 이상 6명 이하의 정수 값으로만 수용 인원을 변경할 수 있다")
    @ParameterizedTest
    @CsvSource(value = {"-1,false", "0,false", "1,true", "2,true", "6,true", "7,false"})
    void success_changeCapacity(int newCapacity, boolean shouldSucceed) {
        // given
        Theme theme = themeRepository.save(themeBuilder.office(savedOffice).build());

        // when
        ThrowingCallable changeCapacityAction = () -> theme.changeCapacity(newCapacity);

        // then
        if (shouldSucceed) {
            assertThatCode(changeCapacityAction).doesNotThrowAnyException();
            em.flush();
            Theme findTheme = themeRepository.findById(theme.getId()).orElseThrow();
            assertThat(findTheme.getCapacity()).isEqualTo(newCapacity);
        } else {
            assertThatThrownBy(changeCapacityAction).isInstanceOf(IllegalArgumentException.class);
        }
    }
}