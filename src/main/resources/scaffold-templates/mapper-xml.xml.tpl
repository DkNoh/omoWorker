<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.scbk.sms.mapper.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Mapper">

    <!--
         QuerySpec에서 생성된 Mapper XML. 생성 후 개발자가 직접 수정해 소유한다.
         searchConditions/baseQuery를 분리해 목록·건수·Excel이 같은 조회 조건을 재사용한다.

         화면 DatePicker 검색값은 YYYYMMDD 문자열로 전달된다 (TuiPageBuilder 규약).
         비교 컬럼이 DATE/TIMESTAMP면 TO_DATE/TO_TIMESTAMP로 감싸고,
         단일 날짜 = 조건은 당일 00:00:00 이상, 다음날 00:00:00 미만 범위로 변환한다.
    -->

    <!-- 값이 있는 검색조건만 MyBatis 동적 SQL에 포함한다. -->
    <sql id="searchConditions">
        <where>
[# th:each="condition : ${xml.searchConditions()}"][# th:if="${condition.conditional()}"]            <if test="[( ${condition.test()} )]">
                [( ${condition.sql()} )]
            </if>
[/][# th:unless="${condition.conditional()}"]            [( ${condition.sql()} )]
[/][/]        </where>
    </sql>

    <!-- 사용자가 입력한 SELECT 본문. 복합 쿼리는 의미 보존을 위해 원형 래퍼 구조를 유지한다. -->
    <sql id="baseQuery">
[# th:each="line : ${xml.baseQueryLines()}"]            [( ${line} )]
[/]    </sql>

    <!-- 목록과 동일한 조건의 총 건수. 안전한 단일 테이블 SELECT만 직접 COUNT를 사용한다. -->
    <select id="count" resultType="int">
        /* [( ${model.domainClass()} )]Mapper.count */
[# th:if="${xml.usesDirectCount()}"]        SELECT COUNT(1)
        [( ${xml.directCountFromClause()} )]
        <include refid="searchConditions"/>
[/][# th:unless="${xml.usesDirectCount()}"]
        SELECT COUNT(1) FROM (
        <include refid="baseQuery"/>
[# th:if="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
        ) A
[# th:unless="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
[/]
    </select>

    <!-- 결정적 ORDER BY와 DB 방언별 페이징을 적용한 현재 페이지 조회. -->
    <select id="selectList" resultType="com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO">
        /* [( ${model.domainClass()} )]Mapper.selectList */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
[# th:if="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
        ) A
[# th:unless="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
        ORDER BY [( ${model.orderBy()} )]
        [( ${model.dialect().pageClause()} )]
    </select>
[# th:if="${model.includeCreateUpdate()}"]
    <!-- UpdateRequestDTO 화이트리스트와 생성 기본값만 INSERT한다. -->
    <insert id="insert">
        /* [( ${model.domainClass()} )]Mapper.insert */
        INSERT INTO [( ${model.targetTable()} )] (
[# th:each="value, iter : ${xml.insertValues()}"]            [( ${value.column()} )][# th:if="${!iter.last}"],[/]
[/]        ) VALUES (
[# th:each="value, iter : ${xml.insertValues()}"]            [( ${value.expression()} )][# th:if="${!iter.last}"],[/]
[/]        )
    </insert>

    <!-- 수정 허용 컬럼만 SET하고, 잠금 컬럼을 지정한 경우 조회 시점 값을 WHERE에 함께 둔다. -->
    <update id="update">
        /* [( ${model.domainClass()} )]Mapper.update */
        UPDATE [( ${model.targetTable()} )]
[# th:each="assignment, iter : ${xml.updateAssignments()}"][# th:if="${iter.first}"]           SET [/][# th:unless="${iter.first}"]               [/][( ${assignment.column()} )] = [( ${assignment.expression()} )][# th:if="${!iter.last}"],[/]
[/][# th:each="pk, iter : ${xml.pkBindings()}"][# th:if="${iter.first}"]         WHERE [/][# th:unless="${iter.first}"]           AND [/][( ${pk.column()} )] = [( ${pk.expression()} )]
[/][# th:if="${model.hasLockColumn()}"]           AND ([( ${model.lockColumn()} )] = [( ${xml.beforeLockBinding()} )]
                OR ([( ${model.lockColumn()} )] IS NULL AND [( ${xml.beforeLockBinding()} )] IS NULL))
[/]    </update>

    <!-- 실제 PK 전체를 WHERE 조건으로 사용한다. 업무상 논리 삭제가 필요하면 생성 후 Service/XML을 함께 수정한다. -->
    <delete id="delete">
        /* [( ${model.domainClass()} )]Mapper.delete */
        DELETE FROM [( ${model.targetTable()} )][# th:each="pk, iter : ${xml.pkBindings()}"][# th:if="${iter.first}"] WHERE [/][# th:unless="${iter.first}"]           AND [/][( ${pk.column()} )] = [( ${pk.expression()} )]
[/]    </delete>
[/][# th:if="${model.includeExcel()}"]
    <!-- 페이지 절을 제외하고 현재 검색조건의 전체 결과를 ExcelUtil용 Map으로 반환한다. -->
    <select id="selectListForExcel" resultType="java.util.HashMap">
        /* [( ${model.domainClass()} )]Mapper.selectListForExcel */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
[# th:if="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
        ) A
[# th:unless="${xml.searchConditionsInsideBaseQuery()}"]        <include refid="searchConditions"/>
[/]
        ORDER BY [( ${model.orderBy()} )]
    </select>
[/][# th:if="${model.includePrivacy()}"]
    <!-- 마스킹 해제 권한과 PrivacyLog가 적용된 경로에서만 호출하는 원문 단건 조회. -->
    <select id="selectDetail" resultType="com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO">
        /* [( ${model.domainClass()} )]Mapper.selectDetail */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
        ) A
        WHERE A.[( ${model.pkColumn()} )] = #{[( ${model.pkFieldName()} )]}
    </select>
[/]
</mapper>
