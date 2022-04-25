## 개요 및 의의
안드로이드 스마트폰을 윈도우 마우스처럼 사용할 수 있습니다.

기존 어플들과 차이점으로 Y축(보통 스마트폰을 바라볼때 위쪽방향) 회전을 고려해서 레이저 포인터와 같이 자연스러운 움직임을 구현했습니다.

대부분의 계산 공식과 아이디어는 [G-건 프로젝트](https://hackmd.io/oaTqnj61RCasSOSzaFw3Yw?view)에서 얻었습니다.

위 프로젝트와 마찬가지로 게임에서 사용 가능합니다.

<img src="https://github.com/myungum/virtual-mouse-app/blob/client/img/img1.jpg" width="30%">

## 주의사항
일부 프로그램에서 서버를 비정상 프로그램으로 인식할 수 있습니다(특히 온라인 게임).

자이로센서와 중력센서를 사용하므로 사용 전 지원 여부를 확인해주세요.

Several programs can perceive the server program as abnormal. (especially game)

Gyro sensor and gravity sensor are needed.


## 추가 개발 목표
### 구현한 기능
서버 업로드

통신 코드 정리하기 (분리하기)

LAN 내 서버 찾기 기능

클라이언트-서버 통신 주기 설정 기능 (프레임? 주사율?)

### 구현할 기능
클라이언트-서버 통신 동기화 (현재 UDP 패킷 무지성 난사 중) + 다른 환경에서 성능 테스트

정확도 보정 및 한손 파지 UI 구현

## 참조
G-건 프로젝트 : https://hackmd.io/oaTqnj61RCasSOSzaFw3Yw?view, https://www.youtube.com/watch?v=cRP3xnpOsM0

[virtual-joystick-android](https://github.com/controlwear/virtual-joystick-android)
