# Git 기본 명령어 & 협업 컨벤션 정리

## 1. 핵심 개념

| 용어 | 의미 |
|---|---|
| **로컬(Local)** | 내 컴퓨터 안의 저장소 |
| **origin** | 연결된 원격 저장소(GitHub 등)의 기본 별칭 |
| **브랜치(Branch)** | 독립적으로 작업할 수 있는 작업 공간(가지) |
| **커밋(Commit)** | 변경 내용을 하나의 버전으로 저장하는 것 (스냅샷) |
| **머지(Merge)** | 한 브랜치의 변경 내용을 다른 브랜치에 합치는 것 |
| **충돌(Conflict)** | 같은 부분이 양쪽에서 다르게 수정되어 자동 병합이 불가능한 상태 |
| **PR (Pull Request)** | "이 브랜치 내용을 main에 합쳐주세요"라는 요청. 머지 전 검토 단계 |

```
git pull  = 원격(origin) → 로컬로 가져오기
git push  = 로컬 → 원격(origin)으로 올리기
```

---

## 2. 일상 작업 흐름 (기본 4단계)

```bash
git status                      # 1. 뭐가 바뀌었나 확인
git add .                       # 2. 변경사항 스테이징(담기)
git commit -m "작업 내용 설명"    # 3. 저장(커밋)
git push origin 브랜치명          # 4. 원격(GitHub)으로 업로드
```

이 4단계가 가장 기본이며 작업의 90%를 차지한다.

---

## 3. 자주 쓰는 명령어

### 상태 확인
```bash
git status              # 현재 변경사항, 브랜치 확인 (헷갈릴 때 가장 먼저 실행)
git log                 # 커밋 기록(역사) 보기
git log --oneline       # 한 줄씩 간단히 보기
```

### 변경 내용 비교 (diff)
```bash
git diff                          # 커밋 안 한 변경사항 비교
git diff main 브랜치명              # 두 브랜치 비교
git diff HEAD~1 HEAD              # 직전 커밋과 비교
git diff 파일경로                  # 특정 파일만 비교
```
> diff 화면은 pager로 열리며 `q` 키로 종료

```diff
- 삭제된 줄 (빨간색)
+ 추가된 줄 (초록색)
```

### 저장(커밋)
```bash
git add .                  # 모든 변경 파일 스테이징
git add 파일명               # 특정 파일만 스테이징
git commit -m "메시지"       # 커밋
```

### 원격 동기화
```bash
git push origin 브랜치명     # 로컬 → 원격
git pull origin 브랜치명     # 원격 → 로컬
git remote -v               # origin이 실제로 가리키는 주소 확인
```

### 브랜치 다루기
```bash
git branch                  # 브랜치 목록 보기
git branch 새브랜치명         # 새 브랜치 생성
git checkout 브랜치명         # 브랜치 이동
git checkout -b 새브랜치명    # 생성 + 이동 동시에 (가장 자주 씀)
```

### 변경사항 취소/임시보관
```bash
git restore 파일명           # 커밋 안 한 변경사항 되돌리기 (특정 파일)
git restore .               # 전체 되돌리기
git checkout -- .           # 위와 동일 (구버전 문법)

git stash                   # 변경사항 임시로 치워두기 (브랜치 이동 전)
git stash pop                # 치워둔 변경사항 다시 꺼내오기
```

### 충돌 발생 시 자동 해결 (주의해서 사용)
```bash
git merge main -X ours      # 충돌 시 "내 브랜치" 내용을 우선 적용
git merge main -X theirs    # 충돌 시 "main(상대편)" 내용을 우선 적용
```
> 파일별로 직접 확인이 필요한 경우, VS Code에서 충돌 파일을 열면
> "Accept Current / Accept Incoming / Accept Both" 버튼으로 선택 가능

---

## 4. 커밋 메시지 컨벤션 (Conventional Commits)

### 형식
```
타입(범위): 제목

본문(선택 - "무엇을" 보다 "왜"를 설명)
```

### 타입 목록

| 타입 | 의미 | 예시 |
|---|---|---|
| `feat` | 새 기능 | `feat(sms): 발송 이력 검색 API 추가` |
| `fix` | 버그 수정 | `fix(auth): 로그인 세션 만료 오류 수정` |
| `docs` | 문서만 수정 | `docs: README 빌드 방법 추가` |
| `style` | 코드 포맷팅 (기능 변경 없음) | `style: 들여쓰기 정리` |
| `refactor` | 구조 개선 (동작 동일) | `refactor(mapper): MyBatis 쿼리 분리` |
| `test` | 테스트 코드 | `test: SmsHistoryService 단위테스트 추가` |
| `chore` | 빌드/설정/잡일 | `chore: 의존성 버전 업데이트` |
| `perf` | 성능 개선 | `perf: 페이징 쿼리 인덱스 최적화` |

### 실제 예시
```
fix(controller): 엑셀 다운로드 시 한글 깨짐 수정

CSV 인코딩을 UTF-8 BOM으로 변경하여 엑셀에서
한글이 정상 표시되도록 수정함
```

### Azure DevOps 연동 (work item 자동 연결)
```
feat(sms): 발송 이력 검색 기능 추가 #1234
```
또는
```
Fixes AB#1234
```

---

## 5. 브랜치 네이밍 컨벤션

### 형식
```
타입/이슈번호-간단한설명
```

### 접두어 의미

| 접두어 | 의미 |
|---|---|
| `feat/` | 새 기능 추가 |
| `fix/` | 버그 수정 |
| `chore/` | 자잘한 정리/유지보수 작업 |
| `refactor/` | 코드 구조 개선 |
| `docs/` | 문서 수정 |
| `hotfix/` | 운영 환경 긴급 수정 |

### 예시
```
feature/SMS-123-add-history-search
fix/SMS-456-fix-csrf-token
chore/SMS-789-cleanup-scaffold
hotfix/SMS-999-prod-login-error
```

---

## 6. 브랜치를 나누는 이유

`main`은 항상 안정적으로 동작해야 하는 정식 버전이다. 실험적인 변경을 바로 main에 적용하면
잘못됐을 때 정식 버전 자체가 망가질 위험이 있다.

**작업 흐름**
1. `main`에서 새 브랜치 생성 (`feat/...`, `fix/...` 등)
2. 그 브랜치 안에서 자유롭게 수정/실험
3. 잘 동작하는 것이 확인되면 → PR(Pull Request)로 main에 병합 요청
4. 문제 있으면 → 그 브랜치만 수정하거나 폐기 (main은 영향 없음)

**AI가 코드를 생성/수정하는 경우 더욱 중요**
- AI 작업 → 별도 브랜치에서 진행
- 사람이 PR에서 diff를 보고 검토
- 문제없으면 머지, 문제 있으면 브랜치만 수정 또는 폐기

---

## 7. 협업 시 추가로 챙기면 좋은 습관

- PR 제목도 커밋 메시지와 같은 컨벤션으로 작성 (`feat: ...`, `fix: ...`)
- 하나의 커밋 = 하나의 논리적 변경 단위로 분리 (여러 작업을 한 커밋에 몰아넣지 않기)
- 커밋 메시지는 "무엇을 했는지"보다 "왜 했는지" 위주로 작성 (코드 자체에서 무엇을 했는지는 확인 가능)
- 막히거나 헷갈리면 항상 `git status`부터 실행
