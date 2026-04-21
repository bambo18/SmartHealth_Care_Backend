# 똑똑하개 건강하개 개발 전용 AI 진단 서비스
- 해당 리포지토리는 똑똑하개 건강하개 애플리케이션 내에서 사용되는 AI 진단 모델들을 포함하고 있습니다. 개발환경에서 AI 모델을 테스트하고 통합하는 데 사용됩니다.

## Setup
- 해당 리포지토리를 클론합니다:
  ```bash
  git clone <repository-url>
  cd smart_health_dog_disease_detection
  ```
- 서버의 작동을 위해 OS 환경 변수를 설정합니다:
  ```bash
  export AI_MODEL_SERVICE_EMAIL='AI 전용 계정 사용자 이름'
  export AI_MODEL_SERVICE_PASSWORD='AI 전용 계정 비밀번호'

  export REDIS_HOST='Redis 서버 호스트 (선택 사항: 기본값 localhost)'
  export REDIS_PORT='Redis 서버 포트 (선택 사항: 기본값 6379)'
  export REDIS_PASSWORD='Redis 서버 비밀번호 (선택 사항)'

  export AI_MODEL_SERVICE_EYE_UPDATE_ENDPOINT='AI 모델 서비스 눈 진단 업데이트 엔드포인트 URL'
  export AI_MODEL_SERVICE_URINE_UPDATE_ENDPOINT='AI 모델 서비스 소변 진단 업데이트 엔드포인트 URL'
  export AI_MODEL_SERVICE_STATUS_UPDATE_ENDPOINT='AI 모델 서비스 상태 업데이트 엔드포인트 URL'
  export AI_MODEL_SERVICE_LOGIN_ENDPOINT='AI 모델 서비스 로그인 엔드포인트 URL'
  ```
- Python 가상환경을 설정하고 활성화합니다:
  ```bash
  python -m venv venv
  source venv/bin/activate  # Windows에서는 venv\Scripts\activate
  ```
- 해당 리포지토리를 클론한 후, 필요한 Python 패키지를 설치합니다:
  ```bash
  pip install -r requirements-cv2.txt
  ```
- 프로젝트 관리자에게 문의하여 AI 모델 파일들을 `dog_models` 및 `cat_models` 디렉토리에 배치합니다.

## Usage
- Celery 워커를 시작하여 AI 진단 작업을 처리합니다:
  ```bash
  celery -A smart_health_dog_disease_detection worker -l info -c 1
  ```