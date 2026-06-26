# V2 운영 메뉴 Baseline

v3 BASE PROJECT에서 static 메뉴와 DB seed가 공통으로 사용하는 v2 기준 메뉴 목록이다. 메뉴명이나 URL을 바꾸면 `StaticMenuSource`, `db/oracle/02_menu_auth_seed.sql`, 이 문서를 함께 갱신한다.

## 메뉴 목록

| MENU_ID | PARENT_MENU_ID | 메뉴명 | URL | 정렬 | 비고 |
|---|---|---|---|---:|---|
| `G_BASIC` |  | 기본메뉴 |  | 10 | 대메뉴 |
| `BASIC_INTRO` | `G_BASIC` | SMS관리시스템 안내 | `/basic/intro` | 10 |  |
| `BASIC_NOTICE` | `G_BASIC` | 공지사항 | `/basic/notice` | 20 |  |
| `BASIC_MESSAGE` | `G_BASIC` | 메시지조회 | `/basic/message` | 30 |  |
| `BASIC_USER_SEARCH` | `G_BASIC` | 사용자조회 | `/basic/user-search` | 40 |  |
| `BASIC_MFA` | `G_BASIC` | MFA사용자관리 | `/basic/mfa` | 50 |  |
| `G_SMS_SEARCH` |  | SMS발송조회 |  | 20 | 대메뉴 |
| `SMS_HISTORY` | `G_SMS_SEARCH` | 발송이력조회 | `/sms/history` | 10 |  |
| `SMS_CUSTOMER_SEARCH` | `G_SMS_SEARCH` | 고객별 조회 | `/sms/customer-search` | 20 |  |
| `SMS_SSN_SEARCH` | `G_SMS_SEARCH` | 주민번호 조회 | `/sms/ssn-search` | 30 |  |
| `G_CAMPAIGN` |  | 캠페인SMS |  | 30 | 대메뉴 |
| `CAMPAIGN_TARGET` | `G_CAMPAIGN` | 발송대상관리 | `/campaign/target-manage` | 10 |  |
| `CAMPAIGN_TARGET_APPROVAL` | `G_CAMPAIGN` | 발송대상승인 | `/approval` | 20 |  |
| `CAMPAIGN_SMS_REGISTER` | `G_CAMPAIGN` | SMS등록 | `/campaign/sms/register` | 30 |  |
| `CAMPAIGN_LMS_REGISTER` | `G_CAMPAIGN` | LMS등록 | `/campaign/lms/register` | 40 |  |
| `CAMPAIGN_ALIMTALK_REGISTER` | `G_CAMPAIGN` | 알림톡등록 | `/campaign/alimtalk/register` | 50 |  |
| `CAMPAIGN_SMS_APPROVE` | `G_CAMPAIGN` | SMS승인 | `/campaign/sms/approve` | 60 |  |
| `CAMPAIGN_LMS_APPROVE` | `G_CAMPAIGN` | LMS승인 | `/campaign/lms/approve` | 70 |  |
| `CAMPAIGN_ALIMTALK_APPROVE` | `G_CAMPAIGN` | 알림톡승인 | `/campaign/alimtalk/approve` | 80 |  |
| `CAMPAIGN_SMS_HISTORY` | `G_CAMPAIGN` | 발송이력조회 | `/sms/campaign` | 90 | SMS 캠페인 발송이력 |
| `CAMPAIGN_LMS_HISTORY` | `G_CAMPAIGN` | LMS발송이력조회 | `/sms/campaign-lms` | 100 |  |
| `CAMPAIGN_ALIMTALK_HISTORY` | `G_CAMPAIGN` | 알림톡 발송이력조회 | `/sms/campaign-alimtalk` | 110 |  |
| `G_SYSTEM` |  | 시스템관리 |  | 40 | 대메뉴 |
| `SYSTEM_DEP` | `G_SYSTEM` | 부서관리 | `/system/dept-manage` | 10 | `DEP` 기준 |
| `SYSTEM_MESSAGE` | `G_SYSTEM` | 메시지 관리 | `/system/message` | 20 |  |
| `SYSTEM_KAKAO_TEMPLATE` | `G_SYSTEM` | 카카오템플릿관리 | `/system/kakao-template` | 30 |  |
| `SYSTEM_AD_MESSAGE` | `G_SYSTEM` | 광고성 메시지관리 | `/system/ad-message` | 40 |  |
| `SYSTEM_HOURLY_STATS` | `G_SYSTEM` | 시간대별조회 | `/sms/dept-stat` | 50 |  |
| `G_ACCOUNT` |  | 시스템관리 계정관리 |  | 50 | 대메뉴 |
| `ACCOUNT_USER` | `G_ACCOUNT` | 사용자관리 | `/account/user-manage` | 10 | `EMP` 기준 |
| `G_STATISTICS` |  | 통계 관리 |  | 60 | 대메뉴 |
| `STAT_MARKETING_OPTOUT` | `G_STATISTICS` | 마케팅 철회 통계 | `/statistics/marketing-optout` | 10 |  |

## 동기화 대상

- `src/main/java/com/scbk/sms/service/menu/StaticMenuSource.java`
- `db/oracle/02_menu_auth_seed.sql`
- `docs/base/v2-menu-baseline.md`

## 주의

`docs/api-mapping.md`에는 v2 개발 중 추가되었거나 운영 메뉴와 다른 화면이 포함될 수 있다. 이 문서는 사용자가 제공한 v2 운영 메뉴 기준을 v3 초기 메뉴 baseline으로 고정한다.