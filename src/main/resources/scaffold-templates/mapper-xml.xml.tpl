<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.scbk.sms.mapper.@@MODULE_NAME@@.@@DOMAIN_CLASS@@Mapper">

    <!-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. -->
    <!-- 화면 DatePicker 검색값은 YYYYMMDD 문자열로 전달된다 (TuiPageBuilder 규약).
         비교 컬럼이 DATE/TIMESTAMP면 TO_DATE/TO_TIMESTAMP로 감싸고,
         단일 날짜 = 조건은 당일 00:00:00 이상, 다음날 00:00:00 미만 범위로 변환한다. -->

    <sql id="searchConditions">
        <where>
@@SEARCH_CONDITIONS@@        </where>
    </sql>

    <sql id="baseQuery">
@@BASE_QUERY@@    </sql>

    <select id="count" resultType="int">
        /* @@DOMAIN_CLASS@@Mapper.count */
        SELECT COUNT(1) FROM (
        <include refid="baseQuery"/>
        ) A
        <include refid="searchConditions"/>
    </select>

    <select id="selectList" resultType="com.scbk.sms.vo.@@MODULE_NAME@@.@@DOMAIN_CLASS@@VO">
        /* @@DOMAIN_CLASS@@Mapper.selectList */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
        ) A
        <include refid="searchConditions"/>
        ORDER BY @@ORDER_BY@@
        @@PAGE_CLAUSE@@
    </select>
@@CRUD_SECTION@@@@EXCEL_SECTION@@@@PRIVACY_SECTION@@
</mapper>
