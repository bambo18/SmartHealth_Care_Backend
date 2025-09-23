# 똑똑하개 건강하개 벡엔드

## 데이터베이스 생성
현재 데이터베이스는 PostgreSQL을 사용합니다. 프로젝트를 실행하기 전에, PostgreSQL 데이터베이스를 생성해야 합니다.

```sql
CREATE DATABASE mydatabase;
```

## application.properties 설정

실행 전에, 총 4개의 환경변수를 설정해야 합니다.
- DB_URL : 데이터베이스 URL - 위에서 생성한 데이터베이스의 이름과 호스트 정보를 포함해야 합니다.
  - 예: jdbc:postgresql://localhost:5432/mydatabase
- DB_USERNAME : 데이터베이스 사용자명
- DB_PASSWORD : 데이터베이스 비밀번호
- JWT_SECRET : JWT 토큰 서명에 사용할 비밀 키
  - https://jwtsecrets.com/#generator 에서 256 비트로 설정 후 생성

## 빌드 및 실행

위에 설정이 완료되면, 다음 명령어로 프로젝트를 빌드하고 실행할 수 있습니다.

```bash
./gradlew bootRun
```