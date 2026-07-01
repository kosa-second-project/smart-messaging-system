# 📝 Smart Messaging System 협업 컨벤션 가이드

이 문서는 **Smart Messaging System** 프로젝트의 코드 스타일 및 개발 컨벤션을 정의합니다. 팀원 간의 원활한 협업과 코드 일관성을 위해 아래 규칙을 준수해 주세요.

---

## 🛠️ 기술 스택 (Tech Stack)
*   **Language & JDK**: Java 17
*   **Framework**: Spring Boot 3.5.16
*   **Database**: Oracle DB
*   **ORM / SQL Mapper**: MyBatis
*   **View Engine (Front)**: Thymeleaf (MPA 방식)
*   **Security**: Spring Security

---

## 🔒 로컬 DB 및 설정 보안 가이드
DB 접속 주소 및 계정 정보와 같은 민감한 커넥션 정보가 깃허브(GitHub)에 절대 유출되지 않도록 설정 파일을 분리했습니다.

1.  **[application.yml](file:///C:/Users/KOSA/Desktop/smart_messaging_system/src/main/resources/application.yml)**:
    *   공통적인 스프링/마이바티스 설정만 명시하며, 기본 활성화 프로필을 로컬(`spring.profiles.active: local`)로 지정합니다. (Git에 업로드됨)
2.  **[application-local.yml.sample](file:///C:/Users/KOSA/Desktop/smart_messaging_system/src/main/resources/application-local.yml.sample)**:
    *   로컬 개발 설정의 템플릿 파일입니다. (Git에 업로드됨)
3.  **application-local.yml (실제 구동 시 필수)**:
    *   프로젝트를 최초로 Clone 받으신 뒤, `application-local.yml.sample`을 복사하여 동일 폴더 내에 `application-local.yml` 파일을 만들어 주세요.
    *   `password: "your_password"` 부분에 실제 로컬 DB 비밀번호를 기입한 뒤 구동합니다.
    *   해당 파일은 **[.gitignore](file:///C:/Users/KOSA/Desktop/smart_messaging_system/.gitignore)**에 차단 등록되어 있어 **절대 깃허브에 공유되지 않습니다.**

---

## 📁 프로젝트 폴더 구조 및 역할
모든 소스코드는 `com.example.smartmessaging` 패키지 하위에 작성하며, 역할별로 다음과 같이 분리합니다.

*   `controller`: 웹 요청(URL)을 받고 Thymeleaf 뷰를 반환하는 MVC 컨트롤러 위치
*   `service`: 실제 비즈니스 로직 및 트랜잭션(`@Transactional`) 처리 레이어
*   `mapper`: MyBatis의 `@Mapper` 인터페이스 파일 위치 (SQL XML 매퍼와 연결)
*   `dto`: 화면/API 데이터 전달용 객체 및 DB 매핑 객체 위치
    *   `request`: 클라이언트로부터 들어오는 입력 데이터 DTO 위치
    *   `response`: 클라이언트로 나가는 출력 데이터 DTO 위치
    *   `vo`: 데이터베이스 테이블과 1:1 매핑되는 객체 위치
*   `config`: Spring Security, MyBatis 등 프로젝트 설정 클래스 위치
*   `security`: Custom UserDetailsService, 인증/인가 관련 컴포넌트 위치
*   `aop`: 공통 관심사(로깅, 성능 측정 등) 처리를 위한 AOP 클래스 위치
*   `utils`: 공통 유틸리티 클래스 위치
*   `exception`: 커스텀 예외 클래스 및 글로벌 예외 처리기(`@ControllerAdvice`) 위치

---

## 🏷️ 클래스 네이밍 및 데이터 모델 컨벤션

데이터를 주고받거나 데이터베이스와 매핑할 때 사용하는 클래스들은 역할에 따라 **`dto` 하위 패키지**에 나누어 정의하며, 아래의 명명 규칙을 철저히 준수합니다.

### 1. DB 테이블 1:1 매핑 객체 (`dto/vo/`)
*   **규칙**: 데이터베이스 테이블 구조와 1:1로 매핑되는 객체는 **`dto/vo`** 패키지 하위에 정의하며, **테이블명 뒤에 `VO`**를 붙여 정의합니다.
*   **예시**:
    *   `UserVO` (USER 테이블 매핑)
    *   `CampaignVO` (CAMPAIGN 테이블 매핑)
*   **용도**: MyBatis Mapper XML에서 조회 결과(`resultType`)나 쿼리 파라미터(`parameterType`)로 직접 매핑할 때 사용합니다.

### 2. 화면/API 요청 데이터 (`dto/request/`)
*   **규칙**: 클라이언트(화면)가 입력하여 서버로 보내는 폼 데이터나 API Request Body는 **`dto/request`** 패키지 하위에 정의하며, 뒤에 **`Request`** 또는 **`RequestDTO`**를 붙입니다.
*   **예시**:
    *   `UserJoinRequest` 또는 `UserJoinRequestDTO`
    *   `TargetSearchRequest` (타겟 설정 조회용 복합 필터 조건)
*   **용도**: Controller에서 `@ModelAttribute` 또는 `@RequestBody`로 사용자 입력을 바인딩할 때 사용합니다.

### 3. 화면/API 응답 데이터 (`dto/response/`)
*   **규칙**: 서버가 처리한 결과를 화면(타임리프 뷰)이나 클라이언트에 보낼 때 사용하는 데이터는 **`dto/response`** 패키지 하위에 정의하며, 뒤에 **`Response`** 또는 **`ResponseDTO`**를 붙입니다.
*   **예시**:
    *   `UserDetailResponse`
    *   `CampaignReportResponse` (발송 건수, 성공률, 비용 조인 결과)
*   **용도**: Service에서 Controller로 데이터를 반환하거나, Controller에서 뷰 템플릿(Model)에 데이터를 실어 보낼 때 사용합니다.

---

## ⚡ MyBatis 작성 규칙
1.  **Mapper 인터페이스 메소드명**과 **Mapper XML의 `id`**는 완전히 일치해야 합니다.
2.  **CamelCase 자동 매핑**: 데이터베이스 컬럼명(`snake_case`)과 Java 필드명(`camelCase`)은 자동으로 매핑되도록 설정되어 있습니다 (`map-underscore-to-camel-case: true`). 
    *   DB 컬럼이 `USER_NAME`이면 Java 필드는 `userName`으로 매핑됩니다.
3.  **Mapper XML 경로**: 모든 MyBatis SQL 파일은 `src/main/resources/mappers/` 패키지 하위에 작성합니다.

---

## 📝 기본 코드 스타일 컨벤션
*   **클래스명**: PascalCase (대문자로 시작, 예: `UserService`)
*   **메소드 및 변수명**: camelCase (소문자로 시작, 예: `getUserInfo`)
*   **상수**: 영문 대문자와 언더스코어(Snake Case) 사용 (예: `MAX_LOGIN_ATTEMPTS`)
*   **Lombok 활용**: Getter, Setter, 생성자 등은 Lombok 어노테이션(`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`)을 사용하여 코드를 간결하게 유지합니다.
