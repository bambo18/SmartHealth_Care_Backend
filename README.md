# 똑똑하개 건강하개 벡엔드

## Database 셋업 및 실행 (Docker 사용)
현재 프로젝트는 PostgreSQL을 사용합니다. 기본적으로 Docker를 사용하여 데이터베이스를 실행하는 방법을 안내합니다.

### Docker 설치
Docker가 설치되어 있지 않다면, [Docker 공식 사이트](https://www.docker.com/get-started)에서 설치 방법을 참고하여 설치합니다.

### 환경변수 설정
#### MacOS / Linux
터미널에서 다음 명령어를 사용하여 환경변수를 설정합니다. (예시 값은 필요에 따라 변경하세요)

```bash
export DB_PASSWORD=your_password # 원하는 비밀번호로 변경
export DB_HOST=localhost # 데이터베이스 호스트
export DB_PORT=5432 # 데이터베이스 포트
export DB_NAME=smart_health_dog # 데이터베이스 이름
export PG_DATABASE_PATH=~/database # 데이터베이스 데이터가 저장될 경로
export JWT_SECRET=your_jwt_secret # JWT 비밀 키. https://jwtsecrets.com/#generator 에서 256 비트로 설정 후 생성
```

#### Windows (PowerShell)
PowerShell에서 다음 명령어를 사용하여 환경변수를 설정합니다. (예시 값은 필요에 따라 변경하세요)

```powershell
[System.Environment]::SetEnvironmentVariable('DB_HOST', 'localhost', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PORT', '5432', 'User')
[System.Environment]::SetEnvironmentVariable('DB_NAME', 'smart_health_dog', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_password', 'User') # 원하는 비밀번호로 변경
[System.Environment]::SetEnvironmentVariable('PG_DATABASE_PATH', "$env:USERPROFILE\database", 'User')
[System.Environment]::SetEnvironmentVariable('JWT_SECRET', 'your_jwt_secret', 'User') # JWT 비밀 키로 변경. https://jwtsecrets.com/#generator 에서 256 비트로 설정 후 생성
```

그 후, 컴퓨터를 재시작하여 환경변수가 적용되도록 합니다.

### Docker Compose로 PostgreSQL 실행
환경변수를 설정한 후, 프로젝트 루트 디렉토리에서 다음 명령어를 실행하여 PostgreSQL 컨테이너를 시작합니다.

```bash
docker-compose up -d
```

## 이메일 전송 서비스 설정
프로젝트는 이메일 전송을 위해 JavaMailSender를 사용합니다. 이메일 전송을 위해 SMTP 서버 설정이 필요하며, 환경변수로 설정할 수 있습니다. SMTP 설정에 필요한 환경변수 값을 이 프로젝트 관리자에게 문의한다음 다음과 같이 설정합니다.

### 환경변수 설정
#### MacOS / Linux
```bash
export SMTP_HOST=[SMTP 서버 호스트]
export SMTP_USER=[SMTP 사용자 이름]
export SMTP_PASSWORD=[SMTP 비밀번호]
```

#### Windows (PowerShell)
```powershell
[System.Environment]::SetEnvironmentVariable('SMTP_HOST', '[SMTP 서버 호스트]', 'User')
[System.Environment]::SetEnvironmentVariable('SMTP_USER', '[SMTP 사용자 이름]', 'User')
[System.Environment]::SetEnvironmentVariable('SMTP_PASSWORD', '[SMTP 비밀번호]', 'User')
```

## 빌드 및 실행

프로젝트에서 사용하는 자바 버전은 24입니다. 자바 24가 설치되어 있는지 확인하세요. 설치되어 있지 않다면, [링크](https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html)에서 설치할 수 있습니다. 본인의 OS에 맞는 버전을 다운로드하여 설치합니다.

위에 설정이 완료되면, 다음 명령어로 프로젝트를 빌드하고 실행할 수 있습니다.

#### MacOS / Linux
```bash
./gradlew bootRun
```

#### Windows (PowerShell)
```powershell
./gradlew bootRun
```